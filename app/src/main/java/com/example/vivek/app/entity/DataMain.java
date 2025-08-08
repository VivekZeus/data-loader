package com.example.vivek.app.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;


@Entity
@Data
public class DataMain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private long rollNo;
    private int age;
    private String userId;
    private String taskId;

    public DataMain(String name, long rollNo, int age, String userId,String taskId) {
        this.name = name;
        this.rollNo = rollNo;
        this.age = age;
        this.userId = userId;
        this.taskId=taskId;
    }

    public DataMain(){

    }
}
