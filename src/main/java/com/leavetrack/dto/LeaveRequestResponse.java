package com.leavetrack.dto;

import com.leavetrack.model.LeaveStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private Long leaveTypeId;
    private String leaveTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer requestedDays;
    private String reason;
    private LeaveStatus status;
    private LocalDateTime appliedOn;
    private Long reviewedById;
    private String reviewedByName;
}
