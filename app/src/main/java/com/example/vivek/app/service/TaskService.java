package com.example.vivek.app.service;

import com.example.vivek.app.entity.DataLoaderMetaData;
import com.example.vivek.app.enums.TaskStatus;
import com.example.vivek.app.repository.MetaDataRepository;
import com.example.vivek.app.util.CacheUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class TaskService {



    @Autowired
    private MetaDataRepository metaDataRepository;

    public String getTaskStatus(String userId){
        DataLoaderMetaData metaData = metaDataRepository.findTopByUserIdOrderByStartedAtDesc(userId).orElse(null);
        assert metaData != null;
        return  metaData.getStatus().name();
    }

    public Map<String,Object> getTaskProgress(String userId){
        DataLoaderMetaData metaData = metaDataRepository.findTopByUserIdOrderByStartedAtDesc(userId).orElse(null);
        assert metaData != null;
        return  Map.of("recordsProcessed",metaData.getProcessedRecords(),
                "recordsRequested",metaData.getRequestedRecords());
    }

    private boolean canCancelTask(TaskStatus status){
        return (status==TaskStatus.IN_PROGRESS || status==TaskStatus.WAITING || status==TaskStatus.COMPLETED);
    }

    private boolean canResumeTask(TaskStatus status){
        return (status==TaskStatus.STOPPED || status==TaskStatus.CANCELLED);
    }

    public boolean cancelUserTask(String userId){
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

    public boolean resumeTask(String userId) {
        DataLoaderMetaData metaData = metaDataRepository.findTopByUserIdOrderByStartedAtDesc(userId).orElse(null);
        assert metaData != null;
        TaskStatus status=metaData.getStatus();
        if(!canResumeTask(status))return false;

        // create task dto using metadata
        // and requeue
        // update the states too
        return true;

    }
}
