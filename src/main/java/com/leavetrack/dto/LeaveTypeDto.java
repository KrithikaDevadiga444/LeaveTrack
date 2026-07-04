package com.leavetrack.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveTypeDto {
    @NotBlank(message = "Leave type name is required")
    private String name;

    @NotNull(message = "Default days per year is required")
    @Min(value = 1, message = "Default days must be at least 1")
    private Integer defaultDaysPerYear;
}
