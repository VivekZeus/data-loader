package com.example.vivek.app.util;


import com.example.vivek.app.enums.TaskStatus;

import java.util.Map;
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

    public boolean isHighPriorityRequestAvailable(){
        cache.putIfAbsent("high-priority-request", String.valueOf(false));
        cache.putIfAbsent("high-priority-requests-available", String.valueOf(20));
        return Boolean.parseBoolean(cache.get("high-priority-request"));
    }





}
