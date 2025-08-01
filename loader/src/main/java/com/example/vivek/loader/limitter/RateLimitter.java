package com.example.vivek.loader.limitter;

import com.example.vivek.loader.dto.RateLimitDto;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public  class RateLimitter {

     private static Map<String, RateLimitDto> rateLimitCache=new ConcurrentHashMap<>();

     private static  final int REQUEST_LIMIT=3;
     private static final int PERIOD=1;
     private static final ChronoUnit UNIT=ChronoUnit.MINUTES;

    public static boolean shouldAllowRequest(String apiKey) {
        long now = Instant.now().toEpochMilli();
        long windowMillis = Duration.of(PERIOD, UNIT).toMillis();

        RateLimitDto dto = rateLimitCache.get(apiKey);

        if (dto == null || now - dto.getTimestamp() > windowMillis) {
            rateLimitCache.put(apiKey, new RateLimitDto(1, now));
            return true;
        } else {
            if (dto.getCount() < REQUEST_LIMIT) {
                dto.setCount(dto.getCount()+1);
                rateLimitCache.put(apiKey, dto);
                return true;
            } else {
                return false;
            }
        }
    }
    
}
