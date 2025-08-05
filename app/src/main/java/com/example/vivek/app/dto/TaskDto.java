package com.example.vivek.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class TaskDto {

    private String taskId;
    private String userId;
    private long requestedRecords;
    private boolean isHighPriorityTask=false;
    private boolean isFirst=true;

    public TaskDto(){}

    public TaskDto(String taskId, String userId, long requestedRecords) {
        this.taskId = taskId;
        this.userId = userId;
        this.requestedRecords = requestedRecords;
    }


}
