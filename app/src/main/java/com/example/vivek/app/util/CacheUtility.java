package com.example.vivek.app.util;


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

    public static boolean incrementHighPriorityRequestCount(){
        String reqServed = cache.get("high-p-req-served");

        // If key doesn't exist, initialize to 1 and return true
        if (reqServed == null) {
            cache.put("high-p-req-served", "1");
            return true;
        }

        // Parse the current count
        int count = Integer.parseInt(reqServed);

        // If count is > 99, return false
        if (count > 29) {
            return false;
        }

        // Increment count and store back in cache
        cache.put("high-p-req-served", String.valueOf(count + 1));
        return true;
    }
    public static int getNumberOfHighPriorityReqServed(){
        cache.putIfAbsent("high-p-req-served", "1");
        return Integer.parseInt(cache.get("high-p-req-served"));
    }

    public static int getNumberOfLowPriorityReqServed(){
        // If key doesn't exist, initialize to 1 and return true
        cache.putIfAbsent("high-p-req-served", "1");
        return Integer.parseInt(cache.get("high-p-req-served"));
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


}
