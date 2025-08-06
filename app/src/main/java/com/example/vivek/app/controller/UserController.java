package com.example.vivek.app.controller;


import com.example.vivek.app.entity.CustomUser;
import com.example.vivek.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;


    @PostMapping("/verify")
    public ResponseEntity<?> addOrVerifyUser(@RequestBody Map<String,String> data){
        String userId=data.get("userId");
        try {
            CustomUser user=userRepository.findByUserId(userId).orElseThrow();
        } catch (Exception e) {
            userRepository.save(
                    new CustomUser(userId)
            );
            return ResponseEntity.status(201).build();
        }
        return ResponseEntity.status(200).build();
    }
}
