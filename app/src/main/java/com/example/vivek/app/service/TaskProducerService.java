package com.example.vivek.app.service;


import com.example.vivek.app.dto.TaskDto;
import com.example.vivek.app.entity.DataLoaderMetaData;
import com.example.vivek.app.enums.TaskStatus;
import com.example.vivek.app.repository.MetaDataRepository;
import com.example.vivek.app.util.CacheUtility;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TaskProducerService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MetaDataRepository metaDataRepository;

    public static final String TASK_QUEUE = "my-queue";

    private boolean canCreateNewTask(TaskStatus status){
        return (status!=TaskStatus.IN_PROGRESS && status!=TaskStatus.COLLECTED && status!=TaskStatus.WAITING);
    }

    public boolean createAndSendTask(String userId, long records) {

        DataLoaderMetaData metaData = metaDataRepository.findTopByUserIdOrderByStartedAtDesc(userId).orElse(null);
        assert metaData != null;
        TaskStatus status=metaData.getStatus();
        if(!canCreateNewTask(status))return false;


        try {
            String taskId = UUID.randomUUID().toString();
            TaskDto taskDto = new TaskDto(taskId, userId, records);


            metaData=new DataLoaderMetaData();
            metaData.setTaskId(taskId);
            metaData.setStatus(TaskStatus.RECEIVED);
            metaData.setUserId(userId);
            metaData.setRequestedRecords(records);
            metaDataRepository.save(metaData);


            // create entry in cache;
            CacheUtility.setTaskStatus(taskId,TaskStatus.RECEIVED);

            rabbitTemplate.convertAndSend("my-delayed-exchange", "my-routing-key", taskDto);
            return true;
        } catch (Exception e) {
            return false;
        }
    }





}
