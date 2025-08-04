package com.example.vivek.app.repository;

import com.example.vivek.app.entity.DataLoaderMetaData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface MetaDataRepository extends JpaRepository<DataLoaderMetaData,String> {

    Optional<DataLoaderMetaData> findByTaskId(String taskId);
    // Fetch the latest task for the given user
    Optional<DataLoaderMetaData> findTopByUserIdOrderByStartedAtDesc(String userId);
}
