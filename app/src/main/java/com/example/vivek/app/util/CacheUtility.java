package com.example.vivek.app.util;


import com.example.vivek.app.enums.TaskStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheUtility {

    public static Map<String,String> taskStatusCache=new ConcurrentHashMap<>();
//    public static Map<String,Boolean> taskDetailsCache=new ConcurrentHashMap<>();
    public static Map<String,String> tasksCache=new ConcurrentHashMap<>();


    public static void setTaskStatus(String taskId,TaskStatus status){
        taskStatusCache.put(taskId,status.name());
    }

    public static TaskStatus getTaskStatus(String taskId){
        String status=taskStatusCache.get(taskId);
        return status==null?null:TaskStatus.valueOf(status);
    }

    public static long getRequestsPerUser(){
        String req=tasksCache.get("reqPerUser");
        if(req==null){
            tasksCache.put("reqPerUser",String.valueOf(100));
            return 100L;
        }
        return Long.parseLong(req);
    }

    public static void setRequestsPerUser(long users){
        // some calculation
        long req=100;
        tasksCache.put("users",String.valueOf(req));
    }

}
