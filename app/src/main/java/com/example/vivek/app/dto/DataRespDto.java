package com.example.vivek.app.dto;


import com.example.vivek.app.entity.DataMain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataRespDto {

    private boolean status;
    private List<DataMain> data;
}
