package com.example.vivek.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HttpRecordRespDto {

    private HttpStatus status;
    private RecordRespDto respDto;
    private long retryAfter;
    private int reqRemaining;


    public HttpRecordRespDto(HttpStatus httpStatus) {
        this.status=httpStatus;
    }
}
