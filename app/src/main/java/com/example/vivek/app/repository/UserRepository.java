package com.example.vivek.app.repository;

import com.example.vivek.app.entity.CustomUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<CustomUser , Integer> {

    Optional<CustomUser> findByUserId(String userId);
}
