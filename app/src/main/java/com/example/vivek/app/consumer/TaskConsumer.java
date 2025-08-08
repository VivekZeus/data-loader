package com.example.vivek.app.consumer;

import com.example.vivek.app.dto.HttpRecordRespDto;
import com.example.vivek.app.dto.RecordDataDto;
import com.example.vivek.app.dto.RequestQuotaStatusDto;
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
import com.rabbitmq.client.Channel;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
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
import java.util.Properties;

@Slf4j
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

    @Autowired
    private AmqpAdmin amqpAdmin;

    private int getLowPriorityQueueMessageCount() {
        Properties properties = amqpAdmin.getQueueProperties("my-queue");
        if (properties != null) {
            return (Integer) properties.get("QUEUE_MESSAGE_COUNT");
        }
        return 0;
    }

    private int getRequestCount(TaskDto taskDto,int remaining){
        int requests;
        if(!taskDto.isHighPriorityTask()){
            int messagesInQueue=getLowPriorityQueueMessageCount()+1;
            if(messagesInQueue>1){
               log.info("Multiple tasks are there with count {}",messagesInQueue);
                // now check if really i will divide
                // if it is first task then division=false
                if(!CacheUtility.getTaskDivision())
                {
                    CacheUtility.setTaskDivision(true);
                }
                else{ // task division was set to true so don't break now
                    CacheUtility.setTaskDivision(false);
                }
                requests = Math.min(40, remaining);
            }
            else {
                requests = Math.min(80, remaining);
            }
        }
        else {
            requests = Math.min(20, remaining);
        }
        return requests;
    }

    @RabbitListener(queues = "my-queue")
    @Transactional(rollbackOn = Exception.class)
    public void handleLowPriorityTask(TaskDto taskDto) throws Exception {
        log.info("Task Received on Low Priority Queue {}",taskDto);
        processTask(taskDto);
    }

    @RabbitListener(queues = "my-queue", containerFactory = "rabbitListenerContainerFactory")
    @Transactional(rollbackOn = Exception.class)
    public void handleLowPriorityTask(TaskDto taskDto, Channel channel, Message message) throws Exception {
        try {
            log.info("Received Task on low priority queue: {}", taskDto);
            processTask(taskDto);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception ex) {
            log.error("Task failed: {}", ex.getMessage());
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            throw ex;
        }
    }

    @RabbitListener(queues = "my-high-priority-queue", containerFactory = "rabbitListenerContainerFactory")
    @Transactional(rollbackOn = Exception.class)
    public void handleHighPriorityTask(TaskDto taskDto, Channel channel, Message message) throws Exception {
        try {
            log.info("Received Task on high priority queue: {}", taskDto);

            processTask(taskDto); // your DB work

            // ✅ ACK after successful processing + commit
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception ex) {
            log.error("Failed to process high priority task: {}", ex.getMessage());


            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);


            throw ex;
        }
    }

    private void processTask(TaskDto taskDto) throws InterruptedException {

        RequestQuotaStatusDto quotaStatus=CacheUtility.getQuotaStatus(taskDto.isHighPriorityTask());
        log.info("The quota status is {}",quotaStatus);
        if(!quotaStatus.isCanExecuteNow()){
            long waitingTime=quotaStatus.getWaitTimeMillis();
          log.info("Task requeued due to lower request count");
            requeueTask(taskDto,waitingTime);
            return;
        }


        int requests=getRequestCount(taskDto,quotaStatus.getAllowedRequestCount());
        log.info("{} requests will be served",requests );





        DataLoaderMetaData metaData = fetchMetaData(taskDto);
        if (metaData == null) return;

        initializeTask(taskDto, metaData);



        long pageNumber = metaData.getLastPageProcessed() + 1;
        long size = Math.min(maxRecords, taskDto.getRequestedRecords());

        for (long i = 0; i < requests; i++) {
            if (isCancelled(taskDto, metaData, pageNumber)) return;

            HttpRecordRespDto apiResp = recordFetcher.getRecords(size, pageNumber);
            if (!handleApiResponse(apiResp, taskDto, metaData, pageNumber)) return;
            CacheUtility.incrementRequestCount(taskDto.isHighPriorityTask(),1);
            List<RecordDataDto> data = apiResp.getRespDto().getRecordList();
            if (data.isEmpty() && taskDto.getRequestedRecords() > 0) {
                requeueTask(taskDto, Duration.ofMinutes(2).toMillis());
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
        if(pageNumber==0){
            taskDto.setFirst(true);
            pageNumber=-1;
        }else taskDto.setFirst(false);
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            log.info("Task requeued due to too many requests");
            requeueTask(taskDto,Duration.ofMinutes(3).toMillis());
        } else if (status == HttpStatus.BAD_GATEWAY) {
           log.info("SERVER IS DOWN SO REQUEUED FOR SOME TIME");
            CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.WAITING);
            metaData.setStatus(TaskStatus.WAITING);
           log.error("Task requeued due to server down");
            requeueTask(taskDto,Duration.ofMinutes(3).toMillis());
        }

        metaData.setLastPageProcessed(pageNumber);
        metaDataRepository.save(metaData);
        return false;
    }

    private void requeueTask(TaskDto taskDto, long delayMillis) {
        taskDto.setFirst(false);
        if(taskDto.isHighPriorityTask()){
            taskProducerService.requeueHighPriorityTask(taskDto, delayMillis);
        }else {
            taskProducerService.requeueLowPriorityTask(taskDto, delayMillis);
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
            requeueTask(taskDto,Duration.ofMinutes(3).toMillis());
        } else {

            metaData.setStatus(TaskStatus.COLLECTED);
            CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.COLLECTED);
            moveToMainTable(metaData.getUserId(),taskDto.getTaskId());

            metaData.setStatus(TaskStatus.COMPLETED);
            CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.COMPLETED);

            metaData.setEndedAt(LocalDateTime.now());
        }

        metaDataRepository.save(metaData);
    }

    @Transactional
    private void moveToMainTable(String userId,String taskId) {
        while (true) {
            Pageable pageable = PageRequest.of(0, (int) maxRecordsToShiftToMain); // always start from 0
            List<DataTemp> dataTempList = temporaryDataRepository.findByUserId(userId, pageable).getContent();

            if (dataTempList.isEmpty()) {
                break;
            }

            List<DataMain> dataMainList = new ArrayList<>();
            for (DataTemp temp : dataTempList) {
                dataMainList.add(new DataMain(
                        temp.getName(),
                        temp.getRollNo(),
                        temp.getAge(),
                        userId,
                        taskId

                ));
            }

            mainDataRepository.saveAll(dataMainList);
            temporaryDataRepository.deleteAll(dataTempList);
        }
    }

}
