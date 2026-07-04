package com.leavetrack.dto;

import com.leavetrack.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long employeeId;
    private String email;
    private String name;
    private Role role;
}
