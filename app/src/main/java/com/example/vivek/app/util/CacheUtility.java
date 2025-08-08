package com.example.vivek.app.util;


import com.example.vivek.app.dto.RequestQuotaStatusDto;
import com.example.vivek.app.dto.TaskDto;
import com.example.vivek.app.enums.TaskStatus;

import javax.security.auth.callback.CallbackHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class CacheUtility {

    public static Map<String,String> cache=new ConcurrentHashMap<>();

    public static void setTaskStatus(String taskId,TaskStatus status){
        cache.put("status:"+taskId,status.name());
    }

    public static TaskStatus getTaskStatus(String taskId){
        String status=cache.get("status:"+taskId);
        return status==null?null:TaskStatus.valueOf(status);
    }

    public static void setRecordsProcessed(String taskId,long records){
        cache.put("processed:"+taskId,String.valueOf(records));
    }

    public static void incrementRequestCount(boolean isHighPriority, int by) {
        String key = isHighPriority ? "high-p-req-served" : "low-p-req-served";
        int current = Integer.parseInt(cache.getOrDefault(key, "0"));
        cache.put(key, String.valueOf(current + by));
    }

    public static void setTaskDivision(Boolean status){
        cache.put("division",String.valueOf(status));
    }

    public static boolean getTaskDivision(){
        return Boolean.parseBoolean(cache.get("division"));
    }

    public static synchronized boolean canExecuteRequest(boolean isHighPriority) {
        String countKey = isHighPriority ? "high-p-req-served" : "low-p-req-served";
        String timeKey = isHighPriority ? "last-request-high-priority" : "last-request-low-priority";

        int maxLimit = isHighPriority ? 21 : 81;

        long currentTime = System.currentTimeMillis();
        String firstTimeStr = cache.get(timeKey);

        if (firstTimeStr == null) {
            // First request ever — initialize both time and count
            cache.put(timeKey, String.valueOf(currentTime));
            cache.put(countKey, "1");
            return true;
        }

        long firstTime = Long.parseLong(firstTimeStr);
        long oneHourMillis = 60 * 60 * 1000;

        if (currentTime - firstTime >= oneHourMillis) {
            // More than 1 hour has passed — reset
            cache.put(timeKey, String.valueOf(currentTime));
            cache.put(countKey, "1");
            return true;
        }

        // Still within the 1-hour window
        String countStr = cache.get(countKey);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);

        if (count >= maxLimit) {
            return false;
        }

        // Increment count
        cache.put(countKey, String.valueOf(count + 1));
        return true;
    }

    public static synchronized RequestQuotaStatusDto getQuotaStatus(boolean isHighPriority) {
        String countKey = isHighPriority ? "high-p-req-served" : "low-p-req-served";
        String timeKey = isHighPriority ? "last-request-high-priority" : "last-request-low-priority";

        int maxLimit = isHighPriority ? 20 : 80;
        int perTaskLimit = isHighPriority ? 20 : 80;

        long currentTime = System.currentTimeMillis();
        String firstTimeStr = cache.get(timeKey);
        int servedCount = Integer.parseInt(cache.getOrDefault(countKey, "0"));

        if (firstTimeStr == null || (currentTime - Long.parseLong(firstTimeStr)) >= Duration.ofMinutes(3).toMillis()) {
            cache.put(timeKey, String.valueOf(currentTime));
            cache.put(countKey, "0"); // reset served request count too!
            return new RequestQuotaStatusDto(true, 0, perTaskLimit);
        }


        long firstTime = Long.parseLong(firstTimeStr);
        long waitTime = Math.max(0, (firstTime + Duration.ofMinutes(3).toMillis()) - currentTime);

        if (servedCount >= maxLimit) {
            return new RequestQuotaStatusDto(false, waitTime, 0);
        }

        // Correct logic: don't over-allow
        int remainingQuota = maxLimit - servedCount;
        int canSendNow = Math.min(perTaskLimit, remainingQuota);

        return new RequestQuotaStatusDto(true, 0, canSendNow);
    }






}
