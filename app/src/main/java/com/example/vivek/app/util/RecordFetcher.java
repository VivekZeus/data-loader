package com.example.vivek.app.util;

import com.example.vivek.app.dto.RecordRespDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RecordFetcher {


    @Autowired
    private RestTemplate restTemplate;

    public RecordRespDto getRecords(long size,long page) {
        String url = String.format("http://localhost:8080/api/records/fetch/%d/%d/my-api-key",page,size);
        ResponseEntity<RecordRespDto> response = restTemplate.getForEntity(url, RecordRespDto.class);
        return response.getBody();
    }

}
