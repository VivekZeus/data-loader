package com.example.vivek.app.consumer;


import com.example.vivek.app.dto.RecordDataDto;
import com.example.vivek.app.dto.RecordRespDto;
import com.example.vivek.app.dto.TaskDto;
import com.example.vivek.app.entity.DataLoaderMetaData;
import com.example.vivek.app.entity.DataTemp;
import com.example.vivek.app.enums.TaskStatus;
import com.example.vivek.app.repository.MetaDataRepository;
import com.example.vivek.app.repository.TemporaryDataRepository;
import com.example.vivek.app.util.CacheUtility;
import com.example.vivek.app.util.RecordFetcher;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


@Service
public class TaskConsumer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MetaDataRepository metaDataRepository;

    @Autowired
    private TemporaryDataRepository temporaryDataRepository;

    private static final long maxRecords=100;

    @Autowired
    private RecordFetcher recordFetcher;

    @RabbitListener(queues = "my-queue")
    public void handleTask(TaskDto taskDto) throws Exception {
        System.out.println("Received Task: " + taskDto);

        DataLoaderMetaData metaData = metaDataRepository.findByTaskId(taskDto.getTaskId()).orElse(null);
        if (metaData == null || metaData.getStatus() == TaskStatus.CANCELLED) {
            return;
        }


        long requests=CacheUtility.getRequestsPerUser();

        long pageNumber=metaData.getLastPageProcessed()+1;
        long size=Math.min(maxRecords,taskDto.getRequestedRecords());

        int processedRecords=0;


        for(long i=0;i<requests;i++){

            if(CacheUtility.getTaskStatus(taskDto.getTaskId())==TaskStatus.CANCELLED)return;

            RecordRespDto resp= recordFetcher.getRecords(size,pageNumber);

            List<RecordDataDto> data=resp.getRecordList();
            List<DataTemp> tempList=new ArrayList<>();

            for(int j=0;j<data.size();j++){

                RecordDataDto recordDataDto=data.get(j);

                tempList.add(
                        new DataTemp(
                                recordDataDto.getName(),
                                recordDataDto.getRollNo(),
                                recordDataDto.getAge(),
                                taskDto.getUserId(),
                                (pageNumber * size) + j,
                                pageNumber
                        )
                );
            }

            temporaryDataRepository.saveAll(tempList);

            metaData.setLastPageProcessed(pageNumber);
            metaData.setProcessedRecords(metaData.getProcessedRecords()+data.size());
            metaDataRepository.save(metaData);

            processedRecords+=data.size();

            if(metaData.getRequestedRecords()<=0)break;

            pageNumber+=1;
            size=Math.min(maxRecords,taskDto.getRequestedRecords()-processedRecords);


            //need to put functionality if 3rd party goes down then again put to queue and return

        }

        long recordsRemaining=taskDto.getRequestedRecords()-processedRecords;
        if (recordsRemaining > 0) {
            taskDto.setRequestedRecords(recordsRemaining);

            rabbitTemplate.convertAndSend(
                    "my-delayed-exchange",
                    "my-routing-key",
                    taskDto,
                    message -> {
                        message.getMessageProperties().setHeader("x-delay", Duration.ofMinutes(5).toMillis());
                        return message;
                    }
            );
        }


    }




}
