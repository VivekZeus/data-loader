package com.example.vivek.app.controller;


import com.example.vivek.app.dto.TaskRequestDto;
import com.example.vivek.app.service.TaskProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
public class TaskController {

    @Autowired
    private TaskProducerService taskProducerService;

    @PostMapping("/create")
    public ResponseEntity<?> createTask(@RequestBody TaskRequestDto taskRequestDto){
        boolean accepted=taskProducerService.createAndSendTask(taskRequestDto.getUserId(),taskRequestDto.getRecordsRequested());
        if (accepted) {
            return ResponseEntity.status(201).build();
        }
        else {
            return ResponseEntity.status(400).build();
        }
    }

//    @PostMapping("/send")
//    public ResponseEntity<String> sendNormal() {
//        taskProducerService.sendNormal("came from normal message without delay");
//        taskProducerService.sendWithDelay("came from delayed message", 60_000);
//        return ResponseEntity.ok(" message sent");
//    }


}
