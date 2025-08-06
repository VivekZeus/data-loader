package com.example.vivek.app.consumer;

import com.example.vivek.app.dto.HttpRecordRespDto;
import com.example.vivek.app.dto.RecordDataDto;
import com.example.vivek.app.dto.RecordRespDto;
import com.example.vivek.app.dto.TaskDto;
import com.example.vivek.app.entity.DataLoaderMetaData;
import com.example.vivek.app.entity.DataMain;
import com.example.vivek.app.entity.DataTemp;
import com.example.vivek.app.enums.TaskStatus;
import com.example.vivek.app.repository.MainDataRepository;
import com.example.vivek.app.repository.MetaDataRepository;
import com.example.vivek.app.repository.TemporaryDataRepository;
import com.example.vivek.app.service.TaskProducerService;
import com.example.vivek.app.util.CacheUtility;
import com.example.vivek.app.util.RecordFetcher;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;





@Service
public class TaskConsumer {

    private static final long maxRecords = 100;
    private static final long maxRecordsToShiftToMain = 5000;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MetaDataRepository metaDataRepository;

    @Autowired
    private TemporaryDataRepository temporaryDataRepository;

    @Autowired
    private MainDataRepository mainDataRepository;

    @Autowired
    private RecordFetcher recordFetcher;

    @Autowired
    private TaskProducerService taskProducerService;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @RabbitListener(queues = "my-queue")
    @Transactional(rollbackOn = Exception.class)
    public void handleLowPriorityTask(TaskDto taskDto) throws Exception {
        System.out.println("Received Task on low priority queue: " + taskDto);
        processTask(taskDto);
    }

    @RabbitListener(queues = "my-high-priority-queue")
    @Transactional(rollbackOn = Exception.class)
    public void handleHighPriorityTask(TaskDto taskDto) throws Exception {
        System.out.println("Received Task on high priority queue: " + taskDto);
        processTask(taskDto);
    }

    private void processTask(TaskDto taskDto) {
        DataLoaderMetaData metaData = fetchMetaData(taskDto);
        if (metaData == null) return;

        initializeTask(taskDto, metaData);

        long requests = taskDto.isHighPriorityTask()?20:80;;
        long pageNumber = metaData.getLastPageProcessed() + 1;
        long size = Math.min(maxRecords, taskDto.getRequestedRecords());

        for (long i = 0; i < requests; i++) {
            if (isCancelled(taskDto, metaData, pageNumber)) return;

            HttpRecordRespDto apiResp = recordFetcher.getRecords(size, pageNumber);
            if (!handleApiResponse(apiResp, taskDto, metaData, pageNumber)) return;

            List<RecordDataDto> data = apiResp.getRespDto().getRecordList();
            if (data.isEmpty() && taskDto.getRequestedRecords() > 0) {
                requeueTask(taskDto, metaData, Duration.ofMinutes(2).toMillis());
                return;
            }

            persistTempData(taskDto, metaData, data, pageNumber, size);
            updateMetaDataAfterProcessing(metaData, taskDto, data.size(), pageNumber);

            if (taskDto.getRequestedRecords() <= 0) break;

            pageNumber++;
            size = Math.min(maxRecords, taskDto.getRequestedRecords());
        }

        finalizeTask(taskDto, metaData, pageNumber);
    }


    private DataLoaderMetaData fetchMetaData(TaskDto taskDto) {
        DataLoaderMetaData metaData = metaDataRepository.findByTaskId(taskDto.getTaskId()).orElse(null);
        return (metaData == null || metaData.getStatus() == TaskStatus.CANCELLED) ? null : metaData;
    }

    private void initializeTask(TaskDto taskDto, DataLoaderMetaData metaData) {
        metaData.setStatus(TaskStatus.IN_PROGRESS);
        CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.IN_PROGRESS);
        metaDataRepository.save(metaData);
    }

    private boolean isCancelled(TaskDto taskDto, DataLoaderMetaData metaData, long pageNumber) {
        if (CacheUtility.getTaskStatus(taskDto.getTaskId()) == TaskStatus.CANCELLED) {
            metaData.setLastPageProcessed(pageNumber);
            metaData.setCancelled(true);
            metaDataRepository.save(metaData);
            return true;
        }
        return false;
    }

    private boolean handleApiResponse(HttpRecordRespDto apiResp, TaskDto taskDto, DataLoaderMetaData metaData, long pageNumber) {
        HttpStatus status = apiResp.getStatus();

        if (status == HttpStatus.OK) return true;

        taskDto.setFirst(false);
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            System.out.println("Task requeued due to too many requests");
            System.out.println("Duration of wait is "+Duration.ofMillis(apiResp.getRetryAfter()).toMinutes());
            requeueTask(taskDto,metaData,Duration.ofMinutes(3).toMillis());
        } else if (status == HttpStatus.BAD_GATEWAY) {
            System.out.println("SERVER IS DOWN SO REQUEUED FOR SOME TIME");
            CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.WAITING);
            metaData.setStatus(TaskStatus.WAITING);
            System.out.println("Task requeued due to server down");
            requeueTask(taskDto,metaData,Duration.ofMinutes(3).toMillis());
        }

        metaData.setLastPageProcessed(pageNumber);
        metaDataRepository.save(metaData);
        return false;
    }

    private void requeueTask(TaskDto taskDto, DataLoaderMetaData metaData, long delayMillis) {
        taskDto.setFirst(false);
        if(taskDto.isHighPriorityTask()){
            taskProducerService.requeueHighPriorityTask(taskDto, metaData, delayMillis);
        }else {
            taskProducerService.requeueLowPriorityTask(taskDto, metaData, delayMillis);
        }
    }

    private void persistTempData(TaskDto taskDto, DataLoaderMetaData metaData, List<RecordDataDto> data, long pageNumber, long size) {
        List<DataTemp> tempList = new ArrayList<>();
        for (int j = 0; j < data.size(); j++) {
            RecordDataDto record = data.get(j);
            tempList.add(new DataTemp(
                    record.getName(),
                    record.getRollNo(),
                    record.getAge(),
                    taskDto.getUserId(),
                    (pageNumber * size) + (j + 1),
                    pageNumber
            ));
        }
        temporaryDataRepository.saveAll(tempList);
    }

    private void updateMetaDataAfterProcessing(DataLoaderMetaData metaData, TaskDto taskDto, int recordsProcessed, long pageNumber) {
        CacheUtility.setRecordsProcessed(taskDto.getTaskId(), metaData.getProcessedRecords() + recordsProcessed);
        metaData.setLastPageProcessed(pageNumber);
        metaData.setProcessedRecords(metaData.getProcessedRecords() + recordsProcessed);
        metaDataRepository.save(metaData);
        taskDto.setRequestedRecords(taskDto.getRequestedRecords() - recordsProcessed);
    }

    private void finalizeTask(TaskDto taskDto, DataLoaderMetaData metaData, long pageNumber) {
        metaData.setLastPageProcessed(pageNumber);

        if (taskDto.getRequestedRecords() > 0) {
            taskDto.setFirst(false);
            taskProducerService.requeueLowPriorityTask(taskDto, metaData, Duration.ofMinutes(1).toMillis());
        } else {

            metaData.setStatus(TaskStatus.COLLECTED);
            CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.COLLECTED);
            moveToMainTable(metaData.getUserId());

            metaData.setStatus(TaskStatus.COMPLETED);
            CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.COMPLETED);

            metaData.setEndedAt(LocalDateTime.now());
        }

        metaDataRepository.save(metaData);
    }


    @Transactional
    private void moveToMainTable(String userId) {
        long count = temporaryDataRepository.countByUserId(userId);
        int n = (int) Math.ceil((double) count / maxRecordsToShiftToMain);
        for (int i = 0; i < n; i++) {
            int size = (int) Math.min(maxRecordsToShiftToMain, count);
            Pageable pageable = PageRequest.of(i, size);
            List<DataTemp> dataTempList = temporaryDataRepository.findByUserId(userId, pageable).getContent();
            List<DataMain> dataMainList = new ArrayList<>();
            for (DataTemp temp : dataTempList) {
                dataMainList.add(
                        new DataMain(
                                temp.getName(),
                                temp.getRollNo(),
                                temp.getAge(),
                                userId

                        )
                );
            }
            mainDataRepository.saveAll(dataMainList);
            temporaryDataRepository.deleteAll(dataTempList);
            count -= size;
        }
    }


//    private void processTask(TaskDto taskDto){
//
//        DataLoaderMetaData metaData = metaDataRepository.findByTaskId(taskDto.getTaskId()).orElse(null);
//        if(metaData ==null||metaData.getStatus()==TaskStatus.CANCELLED) {
//            return;
//        }
//        metaData.setStatus(TaskStatus.IN_PROGRESS);
//        CacheUtility.setTaskStatus(taskDto.getTaskId(),TaskStatus.IN_PROGRESS);
//        metaDataRepository.save(metaData);
//
//        long requests = CacheUtility.getRequestsPerUser();
//
//        long pageNumber = metaData.getLastPageProcessed() + 1;
//        long size = Math.min(maxRecords, taskDto.getRequestedRecords());
//
//        for(long i = 0; i<requests;i++) {
//
//        if (CacheUtility.getTaskStatus(taskDto.getTaskId()) == TaskStatus.CANCELLED) {
//            metaData.setLastPageProcessed(pageNumber);
//            metaData.setCancelled(true);
//            metaDataRepository.save(metaData);
//            return;
//        }
//        HttpRecordRespDto apiResp = recordFetcher.getRecords(size, pageNumber);
//
//        HttpStatus status = apiResp.getStatus();
//        if (status != HttpStatus.OK) {
//            taskDto.setFirst(false);
//            if (status == HttpStatus.TOO_MANY_REQUESTS) {
//                taskProducerService.requeueLowPriorityTask(taskDto, metaData, Duration.ofMinutes(5).toMillis());
//            } else if (status == HttpStatus.BAD_GATEWAY) {
//                System.out.println("SERVER IS DOWN SO REQUEUED FOR SOME TIME");
//                CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.WAITING);
//                metaData.setStatus(TaskStatus.WAITING);
//                taskProducerService.requeueLowPriorityTask(taskDto, metaData, Duration.ofMinutes(2).toMillis());
//            }
//            metaData.setLastPageProcessed(pageNumber);
//            metaDataRepository.save(metaData);
//            return;
//        }
//
//        RecordRespDto resp =apiResp.getRespDto();
//        List<RecordDataDto> data = resp.getRecordList();
//
//        if (data.isEmpty() && taskDto.getRequestedRecords() > 0) {
//            taskDto.setFirst(false);
//            taskProducerService.requeueLowPriorityTask(taskDto, metaData, Duration.ofMinutes(2).toMillis());
//            return;
//        }
//
//        List<DataTemp> tempList = new ArrayList<>();
//
//        for (int j = 0; j < data.size(); j++) {
//            RecordDataDto recordDataDto = data.get(j);
//            tempList.add(
//                    new DataTemp(
//                            recordDataDto.getName(),
//                            recordDataDto.getRollNo(),
//                            recordDataDto.getAge(),
//                            taskDto.getUserId(),
//                            (pageNumber * size) + (j + 1),
//                            pageNumber
//                    )
//            );
//        }
//
//        temporaryDataRepository.saveAll(tempList);
//        CacheUtility.setRecordsProcessed(taskDto.getTaskId(), metaData.getProcessedRecords() + data.size());
//
//        metaData.setLastPageProcessed(pageNumber);
//        metaData.setProcessedRecords(metaData.getProcessedRecords() + data.size());
//        metaDataRepository.save(metaData);
//
//
//        taskDto.setRequestedRecords(
//                taskDto.getRequestedRecords() - data.size()
//        );
//
//        if (taskDto.getRequestedRecords() <= 0) break;
//
//        pageNumber += 1;
//        size = Math.min(maxRecords, taskDto.getRequestedRecords());
//
//    }
//        metaData.setLastPageProcessed(pageNumber);
//
//        if(taskDto.getRequestedRecords()>0)
//            {
//                taskDto.setFirst(false);
//                taskProducerService.requeueLowPriorityTask(taskDto, metaData, Duration.ofMinutes(1).toMillis());
//            }
//        else
//        {
//            metaData.setStatus(TaskStatus.COLLECTED);
//            CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.COLLECTED);
//
//            moveToMainTable(metaData.getUserId());
//
//            metaData.setStatus(TaskStatus.COMPLETED);
//            CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.COMPLETED);
//            metaData.setEndedAt(LocalDateTime.now());
//
//        }
//        metaDataRepository.save(metaData);
//}

}
