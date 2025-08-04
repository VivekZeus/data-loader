package com.example.vivek.app.repository;

import com.example.vivek.app.entity.DataMain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface MainDataRepository extends JpaRepository<DataMain,Integer> {
}
