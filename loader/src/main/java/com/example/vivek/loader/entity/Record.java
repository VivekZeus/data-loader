package com.example.vivek.loader.entity;


import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Record {


    @Id
    private String id;
    private String name;
    private long rollNo;
    private int age;

    @PrePersist
    public void setId() {
        if (this.id == null) this.id = NanoIdUtils.randomNanoId();
    }

}
