package com.example.vivek.app.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordRespDto {

    private List<RecordDataDto> recordList;
}
