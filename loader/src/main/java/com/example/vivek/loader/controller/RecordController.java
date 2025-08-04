package com.example.vivek.loader.controller;

import com.example.vivek.loader.entity.Record;
import com.example.vivek.loader.limitter.RateLimitter;
import com.example.vivek.loader.repository.RecordRepository;
import com.example.vivek.loader.service.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private RandomGenerator randomGenerator;


    @PostMapping("/create")
    public ResponseEntity<?> createRecords(@RequestParam("count") int count){

        List<Record> recordList=new ArrayList<>();
        for(int i=0;i<count;i++){
            recordList.add(new Record(
                    null,randomGenerator.generateRandomName(),
                    randomGenerator.generateSixDigitNumber(),
                    randomGenerator.generateBetween20And100()
            ));
        }

        recordRepository.saveAll(recordList);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/fetch/{page}/{size}/{apiKey}")
    public ResponseEntity<?> getRecords(
            @PathVariable("page") int page,
            @PathVariable("size") int size,
            @PathVariable("apiKey") String apiKey
    ){
        if(size>100){
            return ResponseEntity.status(400).body(Map.of("message","Page size more than 100"));
        }
//        if(!RateLimitter.shouldAllowRequest(apiKey)){
//            return ResponseEntity.status(429).body(Map.of("message","Too many requests sent"));
//        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Record> resultPage = recordRepository.findAll(pageable);

        return ResponseEntity.ok( Map.of("recordList",resultPage.getContent()));

    }
}
