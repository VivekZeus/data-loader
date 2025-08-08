package com.example.vivek.app.util;

import com.example.vivek.app.dto.HttpRecordRespDto;
import com.example.vivek.app.dto.RecordRespDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class RecordFetcher {


    @Autowired
    private RestTemplate restTemplate;

    public HttpRecordRespDto getRecords(long size, long page) {
        try {
            String url = String.format("http://localhost:8080/api/records/fetch/%d/%d/my-api-key", page, size);
            ResponseEntity<RecordRespDto> response = restTemplate.getForEntity(url, RecordRespDto.class);

            assert response.getBody() != null;

            HttpHeaders httpHeaders = response.getHeaders();

            int remaining = parseHeaderAsInt(httpHeaders, "X-Ratelimit-Remaining", -1);
            long retryAfter = parseHeaderAsLong(httpHeaders, "X-Ratelimit-Retry-After", -1L);
//            System.out.println(response.getStatusCode());
            return new HttpRecordRespDto((HttpStatus) response.getStatusCode(), response.getBody(), retryAfter, remaining);

        } catch (HttpClientErrorException.TooManyRequests e) {
            HttpHeaders headers = e.getResponseHeaders();
            int remaining = parseHeaderAsInt(headers, "X-Ratelimit-Remaining", -1);
            long retryAfter = parseHeaderAsLong(headers, "X-Ratelimit-Retry-After", -1L);

            return new HttpRecordRespDto(HttpStatus.TOO_MANY_REQUESTS, null, retryAfter, remaining);

        } catch (ResourceAccessException e) {
            if (testServerResponse()) {
                return getRecords(size, page); // Retry
            } else {
                return new HttpRecordRespDto(HttpStatus.BAD_GATEWAY);
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


    private int parseHeaderAsInt(HttpHeaders headers, String key, int defaultValue) {
        try {
            List<String> values = headers != null ? headers.get(key) : null;
            return values != null && !values.isEmpty() ? Integer.parseInt(values.get(0)) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private long parseHeaderAsLong(HttpHeaders headers, String key, long defaultValue) {
        try {
            List<String> values = headers != null ? headers.get(key) : null;
            return values != null && !values.isEmpty() ? Long.parseLong(values.get(0)) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }


}
