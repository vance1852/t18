package com.kitchen.dto;

import lombok.Data;

import java.util.List;

@Data
public class GenerateScheduleRequest {
    private List<Long> orderIds;
}
