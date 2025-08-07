package com.example.vivek.app.util;


import com.example.vivek.app.dto.RequestQuotaStatusDto;
import com.example.vivek.app.dto.TaskDto;
import com.example.vivek.app.enums.TaskStatus;

import javax.security.auth.callback.CallbackHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class CacheUtility {

    public static Map<String,String> cache=new ConcurrentHashMap<>();

    public  static List<TaskDto> taskDtoList=new ArrayList<>();

    public static void removeTaskFromList(TaskDto taskDto){
        for(TaskDto task:CacheUtility.taskDtoList){
            if(Objects.equals(task.getTaskId(), taskDto.getTaskId())){
                CacheUtility.taskDtoList.remove(task);
            }
        }
    }

    public static void setTaskStatus(String taskId,TaskStatus status){
        cache.put("status:"+taskId,status.name());
    }

    public static TaskStatus getTaskStatus(String taskId){
        String status=cache.get("status:"+taskId);
        return status==null?null:TaskStatus.valueOf(status);
    }

    public static long getRequestsPerUser(){
        String req=cache.get("reqPerUser");
        if(req==null){
            cache.put("reqPerUser",String.valueOf(80));
            return 80L;
        }
        return Long.parseLong(req);
    }

    public static void setRequestsPerUser(long users){
        // some calculation
        long req=80;
        cache.put("reqPerUser",String.valueOf(req));
    }

    public static void setRecordsProcessed(String taskId,long records){
        cache.put("processed:"+taskId,String.valueOf(records));
    }

    public static long getRecordsProcessed(String taskId){
        return cache.containsKey("processed:"+taskId)?Long.parseLong(cache.get("processed:"+taskId)):null;
    }

    public static int getRequestCount(boolean isHighPriority) {
        String key = isHighPriority ? "high-p-req-served" : "low-p-req-served";
        return Integer.parseInt(cache.getOrDefault(key, "0"));
    }


    public static int getNumberOfHighPriorityReqServed(){
        cache.putIfAbsent("high-p-req-served", "1");
        return Integer.parseInt(cache.get("high-p-req-served"));
    }

    public static void incrementRequestCount(boolean isHighPriority, int by) {
        String key = isHighPriority ? "high-p-req-served" : "low-p-req-served";
        int current = Integer.parseInt(cache.getOrDefault(key, "0"));
        cache.put(key, String.valueOf(current + by));
    }
    public static boolean incrementLowPriorityRequestCount() {
        String reqServed = cache.get("low-p-req-served");

        // If key doesn't exist, initialize to 1 and return true
        if (reqServed == null) {
            cache.put("low-p-req-served", "1");
            return true;
        }

        // Parse the current count
        int count = Integer.parseInt(reqServed);

        // If count is > 99, return false
        if (count > 79) {
            return false;
        }

        // Increment count and store back in cache
        cache.put("req-served", String.valueOf(count + 1));
        return true;
    }

    public static void setFirstRequestTimeStamp(boolean isHighPriority) {
        String key = isHighPriority ? "last-request-high-priority" : "last-request-low-priority";
        String timestamp = String.valueOf(System.currentTimeMillis());
        cache.put(key, timestamp);
    }

    public static Long getFirstRequestTimeStamp(boolean isHighPriority) {
        String key = isHighPriority ? "last-request-high-priority" : "last-request-low-priority";
        String timestamp = cache.get(key);
        if (timestamp == null) {
            return null;
        }
        try {
            return Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void setTaskDivision(Boolean status){
        cache.put("division",String.valueOf(status));
    }

    public static boolean getTaskDivision(){
        return Boolean.parseBoolean(cache.get("division"));
    }

    public static long getWaitTimeMillis(boolean isHighPriority) {
        String timeKey = isHighPriority ? "last-request-high-priority" : "last-request-low-priority";
        String firstTimeStr = cache.get(timeKey);

        if (firstTimeStr == null) {
            return 0L; // No wait needed
        }

        long firstTime = Long.parseLong(firstTimeStr);
        long oneHourMillis = 60 * 60 * 1000;
        long currentTime = System.currentTimeMillis();

        long waitTime = oneHourMillis - (currentTime - firstTime);
        return Math.max(waitTime, 0);
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

        if (firstTimeStr == null || (currentTime - Long.parseLong(firstTimeStr)) >= 60 * 60 * 1000) {
            // Reset quota for new hour
            cache.put(timeKey, String.valueOf(currentTime));
//            cache.put(countKey, "1");
            return new RequestQuotaStatusDto(true, 0, perTaskLimit);
        }

        long firstTime = Long.parseLong(firstTimeStr);
        long waitTime = Math.max(0, (firstTime + 60 * 60 * 1000) - currentTime);

        if (servedCount >= maxLimit) {
            return new RequestQuotaStatusDto(false, waitTime, 0);
        }

        // Calculate how many can be allowed now
        int remainingQuota = maxLimit - servedCount;
        int canSendNow = Math.min(perTaskLimit, remainingQuota + 1); // +1 because current request is allowed

        // Update served count
//        cache.put(countKey, String.valueOf(servedCount + 1));

        return new RequestQuotaStatusDto(true, 0, canSendNow);
    }




}
