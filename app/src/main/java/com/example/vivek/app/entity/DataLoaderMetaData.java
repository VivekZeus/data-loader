package com.example.vivek.app.entity;


import com.example.vivek.app.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataLoaderMetaData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;

    private String taskId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime startedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime endedAt;

    private long requestedRecords;
    private long processedRecords=0;

    private boolean cancelled=false;
    private boolean stopped=false;

    private int retryCount=0;
    private long lastPageProcessed=-1;


    @CreationTimestamp
    private LocalDateTime resumedAt;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

}
