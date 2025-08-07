package com.example.vivek.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RequestQuotaStatusDto {
    private boolean canExecuteNow;
    private long waitTimeMillis;
    private int allowedRequestCount;

    public RequestQuotaStatusDto(boolean canExecuteNow, long waitTimeMillis, int allowedRequestCount) {
        this.canExecuteNow = canExecuteNow;
        this.waitTimeMillis = waitTimeMillis;
        this.allowedRequestCount = allowedRequestCount;
    }

}
