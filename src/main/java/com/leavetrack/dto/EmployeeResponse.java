package com.leavetrack.dto;

import com.leavetrack.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private String department;
    private LocalDate joiningDate;
    private Long managerId;
    private String managerName;
}
