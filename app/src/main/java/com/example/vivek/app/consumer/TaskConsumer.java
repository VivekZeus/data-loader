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
import com.example.vivek.app.util.CacheUtility;
import com.example.vivek.app.util.RecordFetcher;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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

    @RabbitListener(queues = "my-queue")
    @Transactional(rollbackOn = Exception.class)
    public void handleTask(TaskDto taskDto) throws Exception {
        System.out.println("Received Task: " + taskDto);


        DataLoaderMetaData metaData = metaDataRepository.findByTaskId(taskDto.getTaskId()).orElse(null);
        if (metaData == null || metaData.getStatus() == TaskStatus.CANCELLED) {
            return;
        }
        metaData.setStatus(TaskStatus.IN_PROGRESS);
        CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.IN_PROGRESS);
        metaDataRepository.save(metaData);


        long requests=CacheUtility.getRequestsPerUser();

        long pageNumber=metaData.getLastPageProcessed()+1;
        long size=Math.min(maxRecords,taskDto.getRequestedRecords());

        for(long i=0;i<requests;i++){

            if(CacheUtility.getTaskStatus(taskDto.getTaskId())==TaskStatus.CANCELLED) {
                metaData.setLastPageProcessed(pageNumber);
                metaDataRepository.save(metaData);
                return;
            }
            Map<?,?> apiResp=recordFetcher.getRecords(size,pageNumber);

            HttpStatus status= (HttpStatus) apiResp.get("status");
            if(status!=HttpStatus.OK){
                if(status == HttpStatus.TOO_MANY_REQUESTS){
                    requeueTask(taskDto,metaData,Duration.ofMinutes(5).toMillis());
                } else if (status==HttpStatus.BAD_GATEWAY) {
                    System.out.println("SERVER IS DOWN SO REQUEUED FOR SOME TIME");
                    CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.WAITING);
                    metaData.setStatus(TaskStatus.WAITING);
                    requeueTask(taskDto,metaData,Duration.ofMinutes(2).toMillis());
                }
                metaData.setLastPageProcessed(pageNumber);
                metaDataRepository.save(metaData);
                return;
            }

            RecordRespDto resp= (RecordRespDto) apiResp.get("data");
            List<RecordDataDto> data=resp.getRecordList();

            if (data.isEmpty() && taskDto.getRequestedRecords() > 0) {
                requeueTask(taskDto, metaData,Duration.ofMinutes(2).toMillis());
                return;
            }

            List<DataTemp> tempList=new ArrayList<>();

            for(int j=0;j<data.size();j++){

                RecordDataDto recordDataDto=data.get(j);

                tempList.add(
                        new DataTemp(
                                recordDataDto.getName(),
                                recordDataDto.getRollNo(),
                                recordDataDto.getAge(),
                                taskDto.getUserId(),
                                (pageNumber * size) + (j+1),
                                pageNumber
                        )
                );
            }

            temporaryDataRepository.saveAll(tempList);
            CacheUtility.setRecordsProcessed(taskDto.getTaskId(),metaData.getProcessedRecords() + data.size() );

            metaData.setLastPageProcessed(pageNumber);
            metaData.setProcessedRecords(metaData.getProcessedRecords() + data.size());
            metaDataRepository.save(metaData);


            taskDto.setRequestedRecords(
                    taskDto.getRequestedRecords()-data.size()
            );

            if(taskDto.getRequestedRecords()<=0)break;

            pageNumber+=1;
            size=Math.min(maxRecords,taskDto.getRequestedRecords());

        }
        metaData.setLastPageProcessed(pageNumber);

        if (taskDto.getRequestedRecords() > 0) {
            requeueTask(taskDto, metaData,Duration.ofMinutes(1).toMillis());
        }
        else {
            metaData.setStatus(TaskStatus.COLLECTED);
            CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.COLLECTED);

            moveToMainTable(metaData.getUserId());

            metaData.setStatus(TaskStatus.COMPLETED);
            CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.COMPLETED);
            metaData.setEndedAt(LocalDateTime.now());
            metaData.setCancelled(true);
        }
        metaDataRepository.save(metaData);
    }

    private void requeueTask(TaskDto taskDto , DataLoaderMetaData metaData,Long duration){

        rabbitTemplate.convertAndSend(
            "my-delayed-exchange",
            "my-routing-key",
            taskDto,
            message -> {
                message.getMessageProperties().setHeader("x-delay", duration);
                return message;
            }
        );

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


//    @Transactional
//    private void moveToMainTable(String userId) {
//        int page = 0;
//        boolean hasMoreRecords = true;
//
//        while (hasMoreRecords) {
//            Pageable pageable = PageRequest.of(page, (int)maxRecordsToShiftToMain);
//            List<DataTemp> dataTempList = temporaryDataRepository.findByUserId(userId, pageable).getContent();
//
//            if (dataTempList.isEmpty()) {
//                hasMoreRecords = false;
//                continue;
//            }
//
//            List<DataMain> dataMainList = dataTempList.stream()
//                    .map(temp -> new DataMain(
//                            temp.getName(),
//                            temp.getRollNo(),
//                            temp.getAge(),
//                            userId))
//                    .collect(Collectors.toList());
//
//            mainDataRepository.saveAll(dataMainList);
//            temporaryDataRepository.deleteAll(dataTempList);
//            page++;
//        }
//    }

}
