package com.example.vivek.app.repository;

import com.example.vivek.app.entity.DataTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemporaryDataRepository extends JpaRepository<DataTemp,Integer> {
}
