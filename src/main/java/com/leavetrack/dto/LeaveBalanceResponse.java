package com.leavetrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceResponse {
    private Long id;
    private Long leaveTypeId;
    private String leaveTypeName;
    private Integer totalDays;
    private Integer usedDays;
    private Integer remainingDays;
}
