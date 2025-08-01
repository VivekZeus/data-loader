package com.example.vivek.app.service;


import com.example.vivek.app.dto.TaskDto;
import com.example.vivek.app.entity.DataLoaderMetaData;
import com.example.vivek.app.enums.TaskStatus;
import com.example.vivek.app.repository.MetaDataRepository;
import com.example.vivek.app.util.CacheUtility;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class TaskProducerService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MetaDataRepository metaDataRepository;

    public static final String TASK_QUEUE = "my-queue";

    public boolean createAndSendTask(String userId, long records) {

        try {
            String taskId = UUID.randomUUID().toString();
            TaskDto taskDto = new TaskDto(taskId, userId, records,0);


            DataLoaderMetaData metaData=new DataLoaderMetaData();
            metaData.setTaskId(taskId);
            metaData.setStatus(TaskStatus.IN_PROGRESS);
            metaData.setUserId(userId);
            metaData.setRequestedRecords(records);
            metaDataRepository.save(metaData);


            // create entry in cache;
            CacheUtility.setTaskStatus(taskId,TaskStatus.IN_PROGRESS);

            rabbitTemplate.convertAndSend("my-delayed-exchange", "my-routing-key", taskDto);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


//    public void sendNormal(String message) {
//        rabbitTemplate.convertAndSend("my-delayed-exchange", "my-routing-key", message);
//        System.out.println("Sent normal message: " + message);
//    }
//
//    public void sendWithDelay(String message, int delayMillis) {
//        MessageProperties props = new MessageProperties();
//        props.setHeader("x-delay", delayMillis);
//        Message msg = new Message(message.getBytes(StandardCharsets.UTF_8), props);
//        rabbitTemplate.send("my-delayed-exchange", "my-routing-key", msg);
//        System.out.println("Sent delayed message: " + message + " (" + delayMillis + " ms)");
//    }


}
