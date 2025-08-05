package com.example.vivek.app.consumer;

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
import org.hibernate.Cache;
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
import java.util.Map;
import java.util.Objects;


@Service
public class TaskConsumer {

    private static final long maxRecords=100;
    private static final long maxRecordsToShiftToMain=5000;

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
//        System.out.println("Received Task: " + taskDto);
//
//
//        DataLoaderMetaData metaData = metaDataRepository.findByTaskId(taskDto.getTaskId()).orElse(null);
//        if (metaData == null || metaData.getStatus() == TaskStatus.CANCELLED) {
//            return;
//        }
//        metaData.setStatus(TaskStatus.IN_PROGRESS);
//        CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.IN_PROGRESS);
//        metaDataRepository.save(metaData);
//
//        long requests=CacheUtility.getRequestsPerUser();
//
//        long pageNumber=metaData.getLastPageProcessed()+1;
//        long size=Math.min(maxRecords,taskDto.getRequestedRecords());
//
//        for(long i=0;i<requests;i++){
//
//            if(CacheUtility.getTaskStatus(taskDto.getTaskId())==TaskStatus.CANCELLED) {
//                metaData.setLastPageProcessed(pageNumber);
//                metaData.setCancelled(true);
//                metaDataRepository.save(metaData);
//                return;
//            }
//            Map<?,?> apiResp=recordFetcher.getRecords(size,pageNumber);
//
//            HttpStatus status= (HttpStatus) apiResp.get("status");
//            if(status!=HttpStatus.OK){
//                taskDto.setFirst(false);
//                if(status == HttpStatus.TOO_MANY_REQUESTS){
//                    taskProducerService.requeueLowPriorityTask(taskDto,metaData,Duration.ofMinutes(5).toMillis());
//                } else if (status==HttpStatus.BAD_GATEWAY) {
//                    System.out.println("SERVER IS DOWN SO REQUEUED FOR SOME TIME");
//                    CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.WAITING);
//                    metaData.setStatus(TaskStatus.WAITING);
//                    taskProducerService.requeueLowPriorityTask(taskDto,metaData,Duration.ofMinutes(2).toMillis());
//                }
//                metaData.setLastPageProcessed(pageNumber);
//                metaDataRepository.save(metaData);
//                return;
//            }
//
//            RecordRespDto resp= (RecordRespDto) apiResp.get("data");
//            List<RecordDataDto> data=resp.getRecordList();
//
//            if (data.isEmpty() && taskDto.getRequestedRecords() > 0) {
//                taskDto.setFirst(false);
//                taskProducerService.requeueLowPriorityTask(taskDto, metaData,Duration.ofMinutes(2).toMillis());
//                return;
//            }
//
//            List<DataTemp> tempList=new ArrayList<>();
//
//            for(int j=0;j<data.size();j++){
//
//                RecordDataDto recordDataDto=data.get(j);
//
//                tempList.add(
//                        new DataTemp(
//                                recordDataDto.getName(),
//                                recordDataDto.getRollNo(),
//                                recordDataDto.getAge(),
//                                taskDto.getUserId(),
//                                (pageNumber * size) + (j+1),
//                                pageNumber
//                        )
//                );
//            }
//
//            temporaryDataRepository.saveAll(tempList);
//            CacheUtility.setRecordsProcessed(taskDto.getTaskId(),metaData.getProcessedRecords() + data.size() );
//
//            metaData.setLastPageProcessed(pageNumber);
//            metaData.setProcessedRecords(metaData.getProcessedRecords() + data.size());
//            metaDataRepository.save(metaData);
//
//
//            taskDto.setRequestedRecords(
//                    taskDto.getRequestedRecords()-data.size()
//            );
//
//            if(taskDto.getRequestedRecords()<=0)break;
//
//            pageNumber+=1;
//            size=Math.min(maxRecords,taskDto.getRequestedRecords());
//
//        }
//        metaData.setLastPageProcessed(pageNumber);
//
//        if (taskDto.getRequestedRecords() > 0) {
//            taskDto.setFirst(false);
//            taskProducerService.requeueLowPriorityTask(taskDto, metaData,Duration.ofMinutes(1).toMillis());
//        }
//        else {
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
        processTask(taskDto);
    }

    @RabbitListener(queues = "my-high-priority-queue")
    @Transactional(rollbackOn = Exception.class)
    public void handleHighPriorityTask(TaskDto taskDto) throws Exception {
        processTask(taskDto);
    }

    @Transactional
    private void moveToMainTable(String userId){
        long count=temporaryDataRepository.countByUserId(userId);
        int n=(int) Math.ceil((double) count /maxRecordsToShiftToMain);
        for (int i=0;i<n;i++){
            int size=(int)Math.min(maxRecordsToShiftToMain,count);
            Pageable pageable = PageRequest.of(i, size);
            List<DataTemp> dataTempList = temporaryDataRepository.findByUserId(userId,pageable).getContent();
            List<DataMain> dataMainList=new ArrayList<>();
            for (DataTemp temp:dataTempList){
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
            count-=size;
        }
    }

    private void processTask(TaskDto taskDto) throws Exception {
        int count=0;
        boolean shouldDivideTasks = false;
        if(!taskDto.isHighPriorityTask() && !CacheUtility.getTaskDivision()) {
            for (TaskDto task : CacheUtility.taskDtoList) {
                if (task.isFirst() && !task.isHighPriorityTask()) {
                    count++;
                    if (count == 2) {
                        shouldDivideTasks = true;
                        CacheUtility.setTaskDivision(true);
                        break;
                    }
                }
            }
        }

        System.out.println("Received Task: " + taskDto);

        DataLoaderMetaData metaData = metaDataRepository.findByTaskId(taskDto.getTaskId()).orElse(null);
        if (metaData == null || metaData.getStatus() == TaskStatus.CANCELLED) {
            return;
        }
        metaData.setStatus(TaskStatus.IN_PROGRESS);
        CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.IN_PROGRESS);
        metaDataRepository.save(metaData);

        long requests = taskDto.isHighPriorityTask()?20:80;
        if(shouldDivideTasks || CacheUtility.getTaskDivision())requests=(long)requests/2;
        if(!shouldDivideTasks && CacheUtility.getTaskDivision())CacheUtility.setTaskDivision(false);
        long pageNumber = metaData.getLastPageProcessed() + 1;
        long size = Math.min(maxRecords, taskDto.getRequestedRecords());

        for (long i = 0; i < requests; i++) {
            if (CacheUtility.getTaskStatus(taskDto.getTaskId()) == TaskStatus.CANCELLED) {
                metaData.setLastPageProcessed(pageNumber);
                metaDataRepository.save(metaData);
                return;
            }

            Map<?, ?> apiResp = recordFetcher.getRecords(size, pageNumber);
            HttpStatus status = (HttpStatus) apiResp.get("status");

            if (status != HttpStatus.OK) {
                taskDto.setFirst(false);
                if (status == HttpStatus.TOO_MANY_REQUESTS) {
                    taskProducerService.requeueLowPriorityTask(taskDto, metaData, Duration.ofMinutes(5).toMillis());
                } else if (status == HttpStatus.BAD_GATEWAY) {
                    System.out.println("SERVER IS DOWN SO REQUEUED FOR SOME TIME");
                    CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.WAITING);
                    metaData.setStatus(TaskStatus.WAITING);
                    taskProducerService.requeueLowPriorityTask(taskDto, metaData, Duration.ofMinutes(2).toMillis());
                }
                CacheUtility.removeTaskFromList(taskDto);
                CacheUtility.taskDtoList.add(taskDto);
                metaData.setLastPageProcessed(pageNumber);
                metaDataRepository.save(metaData);
                return;
            }

            RecordRespDto resp = (RecordRespDto) apiResp.get("data");
            List<RecordDataDto> data = resp.getRecordList();

            if (data.isEmpty() && taskDto.getRequestedRecords() > 0) {
                taskDto.setFirst(false);
                taskProducerService.requeueLowPriorityTask(taskDto, metaData, Duration.ofMinutes(2).toMillis());
                CacheUtility.removeTaskFromList(taskDto);
                CacheUtility.taskDtoList.add(taskDto);
                return;
            }

            List<DataTemp> tempList = new ArrayList<>();
            for (int j = 0; j < data.size(); j++) {
                RecordDataDto recordDataDto = data.get(j);
                tempList.add(new DataTemp(
                        recordDataDto.getName(),
                        recordDataDto.getRollNo(),
                        recordDataDto.getAge(),
                        taskDto.getUserId(),
                        (pageNumber * size) + (j + 1),
                        pageNumber
                ));
            }

            temporaryDataRepository.saveAll(tempList);
            CacheUtility.setRecordsProcessed(taskDto.getTaskId(), metaData.getProcessedRecords() + data.size());
            metaData.setLastPageProcessed(pageNumber);
            metaData.setProcessedRecords(metaData.getProcessedRecords() + data.size());
            metaDataRepository.save(metaData);

            taskDto.setRequestedRecords(taskDto.getRequestedRecords() - data.size());
            if (taskDto.getRequestedRecords() <= 0) break;

            pageNumber += 1;
            size = Math.min(maxRecords, taskDto.getRequestedRecords());
        }

        metaData.setLastPageProcessed(pageNumber);

        if (taskDto.getRequestedRecords() > 0) {
            taskDto.setFirst(false);
            taskProducerService.requeueLowPriorityTask(taskDto, metaData, Duration.ofMinutes(1).toMillis());
            CacheUtility.removeTaskFromList(taskDto);
            CacheUtility.taskDtoList.add(taskDto);
        } else {
            metaData.setStatus(TaskStatus.COLLECTED);
            CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.COLLECTED);

            moveToMainTable(metaData.getUserId());
            CacheUtility.removeTaskFromList(taskDto);

            metaData.setStatus(TaskStatus.COMPLETED);
            CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.COMPLETED);
            metaData.setEndedAt(LocalDateTime.now());
        }
        metaDataRepository.save(metaData);
    }

}
