package com.example.vivek.loader.repository;

import com.example.vivek.loader.entity.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecordRepository extends JpaRepository<Record, String> {
    Page<Record> findAll(Pageable pageable);
}