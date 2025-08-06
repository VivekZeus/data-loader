package com.example.vivek.loader.limitter;

import com.example.vivek.loader.dto.RateLimitDto;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public  class RateLimitter {

     private static Map<String, RateLimitDto> rateLimitCache=new ConcurrentHashMap<>();

     private static  final int REQUEST_LIMIT=100;
     private static final int PERIOD=3;
     private static final ChronoUnit UNIT=ChronoUnit.MINUTES;

    public static  Map<String, String> shouldAllowRequest(String apiKey) {
        long now = Instant.now().toEpochMilli();
        long windowMillis = Duration.of(PERIOD, UNIT).toMillis();
        boolean status;
        RateLimitDto dto = rateLimitCache.get(apiKey);

        if (dto == null || now - dto.getTimestamp() > windowMillis) {
            rateLimitCache.put(apiKey, new RateLimitDto(1, now));
            status=true;
        } else {
            if (dto.getCount() < REQUEST_LIMIT) {
                dto.setCount(dto.getCount()+1);
                rateLimitCache.put(apiKey, dto);
                status=true;
            } else {
                status=false;
            }
        }
//        long requestAfterTime=status ? 0: (dto.getTimestamp()+Duration.ofHours(1).toMillis())-now;
        long requestAfterTime = status ? 0 : (dto.getTimestamp() + windowMillis) - now;

        int remaining=status?dto==null?REQUEST_LIMIT-1:REQUEST_LIMIT-dto.getCount():0;
        return Map.of(
                "X-Ratelimit-Retry-After",String.valueOf(requestAfterTime),
                "X-Ratelimit-limit",String.valueOf (REQUEST_LIMIT),
                "X-Ratelimit-Remaining",String.valueOf (remaining),
                "status",String.valueOf(status)
        );
    }


    
}
