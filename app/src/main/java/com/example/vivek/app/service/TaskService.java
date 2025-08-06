package com.example.vivek.app.service;

import com.example.vivek.app.dto.TaskDto;
import com.example.vivek.app.entity.CustomUser;
import com.example.vivek.app.entity.DataLoaderMetaData;
import com.example.vivek.app.enums.TaskStatus;
import com.example.vivek.app.repository.MetaDataRepository;
import com.example.vivek.app.repository.UserRepository;
import com.example.vivek.app.util.CacheUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class TaskService {

    @Autowired
    private MetaDataRepository metaDataRepository;

    @Autowired
    private TaskProducerService taskProducerService;

    @Autowired
    private UserRepository userRepository;

    private boolean checkUserExists(String userId){
        try {
            userRepository.findByUserId(userId).orElseThrow();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean canResumeTask(TaskStatus status){
        return (status==TaskStatus.STOPPED || status==TaskStatus.CANCELLED || status==TaskStatus.RECEIVED);
    }
    private boolean canCancelTask(TaskStatus status){
        return (status==TaskStatus.IN_PROGRESS || status==TaskStatus.WAITING || status==TaskStatus.COMPLETED);
    }
    private boolean canCreateNewTask(TaskStatus status){
        return (status!=TaskStatus.IN_PROGRESS && status!=TaskStatus.COLLECTED && status!=TaskStatus.WAITING);
    }

    public boolean cancelUserTask(String userId){
        if(!checkUserExists(userId)) return false;
        DataLoaderMetaData metaData = metaDataRepository.findTopByUserIdOrderByStartedAtDesc(userId).orElse(null);
        assert metaData != null;
        TaskStatus status=metaData.getStatus();
        if(canCancelTask(status)){
            metaData.setStatus(TaskStatus.CANCELLED);
            metaDataRepository.save(metaData);
            CacheUtility.setTaskStatus(metaData.getTaskId(),TaskStatus.CANCELLED);
            return true;
        }

        return false;
    }

    public String getTaskStatus(String userId) throws Exception {
        if(!checkUserExists(userId)) throw new Exception();
        DataLoaderMetaData metaData = metaDataRepository.findTopByUserIdOrderByStartedAtDesc(userId).orElse(null);
        assert metaData != null;
        return  metaData.getStatus().name();
    }

    public Map<String,Object> getTaskProgress(String userId) throws Exception {
        if(!checkUserExists(userId)) throw new Exception();
        DataLoaderMetaData metaData = metaDataRepository.findTopByUserIdOrderByStartedAtDesc(userId).orElse(null);
        assert metaData != null;
        return  Map.of("recordsProcessed",metaData.getProcessedRecords(),
                "recordsRequested",metaData.getRequestedRecords());
    }

    public boolean createAndSendTask(String userId, long records) {
        if(!checkUserExists(userId)) return false;
        DataLoaderMetaData metaData = metaDataRepository.findTopByUserIdOrderByStartedAtDesc(userId).orElse(null);
        if(metaData!=null) {
            TaskStatus status = metaData.getStatus();
            if (!canCreateNewTask(status)) return false;
        }

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
            if(records<=2_000){
                taskProducerService.sendHighPriorityTask(taskDto);
                return true;
            }
            taskProducerService.sendLowPriorityTask(taskDto);
            CacheUtility.taskDtoList.add(taskDto);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean resumeTask(String userId) {
        if(!checkUserExists(userId))return false;
        DataLoaderMetaData metaData = metaDataRepository.findTopByUserIdOrderByStartedAtDesc(userId).orElse(null);
        assert metaData != null;
        TaskStatus status=metaData.getStatus();
        if(!canResumeTask(status) || metaData.getRequestedRecords()==metaData.getProcessedRecords())return false;

        // create task dto using metadata
        TaskDto taskDto= new TaskDto();
        taskDto.setRequestedRecords(metaData.getRequestedRecords()-metaData.getProcessedRecords());
        taskDto.setUserId(userId);
        taskDto.setFirst(true);
        taskDto.setTaskId(metaData.getTaskId());
        // check if it is priority task

        metaData.setStatus(TaskStatus.RECEIVED);
        metaData.setResumedAt(LocalDateTime.now());
        metaData.setCancelled(false);
        metaDataRepository.save(metaData);
        CacheUtility.setTaskStatus(metaData.getTaskId(),TaskStatus.RECEIVED);
        CacheUtility.setRecordsProcessed(metaData.getTaskId(),metaData.getProcessedRecords());
        if(taskDto.getRequestedRecords()<= 2_000){
            taskDto.setHighPriorityTask(true);
            taskProducerService.sendHighPriorityTask(taskDto);
        }else {
            taskProducerService.sendLowPriorityTask(taskDto);
        }
        return true;

    }
}
