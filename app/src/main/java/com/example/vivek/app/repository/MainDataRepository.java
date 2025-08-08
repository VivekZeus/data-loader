package com.example.vivek.app.repository;

import com.example.vivek.app.entity.DataMain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface MainDataRepository extends JpaRepository<DataMain,Integer> {

    Page<DataMain> findByTaskIdAndUserId(String taskId, String userId, Pageable pageable);
}
