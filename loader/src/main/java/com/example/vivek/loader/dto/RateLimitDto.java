package com.example.vivek.loader.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitDto {

    private int count;
    private long timestamp;

}
