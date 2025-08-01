package com.example.vivek.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskDto {

    private String taskId;
    private String userId;
    private long requestedRecords;
    private long processedRecords;

}
