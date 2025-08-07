package com.example.vivek.app.controller;


import com.example.vivek.app.dto.TaskRequestDto;
import com.example.vivek.app.entity.DataLoaderMetaData;
import com.example.vivek.app.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/task")
public class TaskController {


    @Autowired
    private TaskService taskService;

    @PostMapping("/create")
    public ResponseEntity<?> createTask(@RequestBody TaskRequestDto taskRequestDto){
        boolean accepted=taskService.createAndSendTask(taskRequestDto.getUserId(),taskRequestDto.getRecordsRequested());
        if (accepted) {
            return ResponseEntity.status(201).build();
        }
        else {
            return ResponseEntity.status(400).build();
        }
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<?> getTaskStatus(@PathVariable String userId) throws Exception {
       String status= taskService.getTaskStatus(userId);
       return ResponseEntity.status(200).body(Map.of("taskStatus",status));
    }

    @GetMapping("/progress/{userId}")
    public ResponseEntity<?> getProgress(@PathVariable String userId) {
        try {
            DataLoaderMetaData data= taskService.getTaskProgress(userId);
            return ResponseEntity.status(200).body(data);
        } catch (Exception e) {
         return    ResponseEntity.status(400).build();
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelOnGoingTask(@RequestBody Map<String,String> data){
       if (taskService.cancelUserTask(data.get("userId"))){
           return ResponseEntity.status(200).build();
       }
       return ResponseEntity.status(400).build();
    }

    @PostMapping("/resume")
    public ResponseEntity<?> resumeTask(@RequestBody  Map<String,String> data ){

       if ( taskService.resumeTask( data.get("userId"))){
           return ResponseEntity.status(200).build();
       }
       return ResponseEntity.badRequest().build();

    }

}
