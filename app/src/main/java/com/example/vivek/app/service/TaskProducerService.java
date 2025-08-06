package com.example.vivek.app.service;


import com.example.vivek.app.dto.TaskDto;
import com.example.vivek.app.entity.DataLoaderMetaData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskProducerService {

    @Autowired
    private RabbitTemplate rabbitTemplate;


    public static final String TASK_QUEUE = "my-queue";
    public static final String HIGH_PRIORITY_QUEUE = "my-high-priority-queue";

    public void sendLowPriorityTask(TaskDto dto){
        rabbitTemplate.convertAndSend("my-delayed-exchange", "my-routing-key", dto);
    }

    public void requeueLowPriorityTask(TaskDto taskDto , DataLoaderMetaData metaData,Long duration){

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

    public void sendHighPriorityTask(TaskDto dto){
        rabbitTemplate.convertAndSend("my-delayed-exchange", "high-routing-key", dto);
    }

    public void requeueHighPriorityTask(TaskDto taskDto , DataLoaderMetaData metaData, Long duration){
        rabbitTemplate.convertAndSend(
                "my-delayed-exchange",
                "high-routing-key",
                taskDto,
                message -> {
                    message.getMessageProperties().setHeader("x-delay", duration);
                    return message;
                }
        );
    }



}
