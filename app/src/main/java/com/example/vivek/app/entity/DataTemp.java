package com.example.vivek.app.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
public class DataTemp {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private long rollNo;
    private int age;
    private String userId;
    private long recordNumber;
    private long pageNumber;


    public DataTemp(String name, long rollNo, int age, String userId, long recordNumber, long pageNumber) {
        this.name = name;
        this.rollNo = rollNo;
        this.age = age;
        this.userId = userId;
        this.recordNumber = recordNumber;
        this.pageNumber = pageNumber;
    }

}
