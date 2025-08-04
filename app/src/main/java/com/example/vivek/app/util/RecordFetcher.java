package com.example.vivek.app.util;

import com.example.vivek.app.dto.RecordRespDto;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class RecordFetcher {


    @Autowired
    private RestTemplate restTemplate;

    public Map<?, ?> getRecords(long size, long page) {
        try {
            String url = String.format("http://localhost:8080/api/records/fetch/%d/%d/my-api-key", page, size);
            ResponseEntity<RecordRespDto> response = restTemplate.getForEntity(url, RecordRespDto.class);
            assert response.getBody() != null;
            return Map.of("data", response.getBody(), "status", response.getStatusCode());
        } catch (ResourceAccessException e) {
            if (testServerResponse()) { // if server is on
                return getRecords(size, page);
            } else {
                // if server is down
                return Map.of("status", HttpStatus.BAD_GATEWAY);
            }
        }
    }


    private boolean  testServerResponse() {
        for(int i=0;i<5;i++){
            System.out.println("came here");
            try {
                String url = "http://localhost:8080/test/test";
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if(response.getStatusCode()==HttpStatus.OK) return true;
            } catch (ResourceAccessException e) {
                //
            }
        }
        return false;

    }



}
