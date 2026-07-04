package com.leavetrack.service;

import com.leavetrack.dto.AuthResponse;
import com.leavetrack.dto.EmployeeResponse;
import com.leavetrack.dto.LoginRequest;
import com.leavetrack.dto.SignupRequest;
import com.leavetrack.exception.BadRequestException;
import com.leavetrack.exception.ResourceNotFoundException;
import com.leavetrack.model.Employee;
import com.leavetrack.model.LeaveBalance;
import com.leavetrack.model.LeaveType;
import com.leavetrack.repository.EmployeeRepository;
import com.leavetrack.repository.LeaveBalanceRepository;
import com.leavetrack.repository.LeaveTypeRepository;
import com.leavetrack.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            EmployeeRepository employeeRepository,
            LeaveTypeRepository leaveTypeRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            AuthenticationManager authenticationManager) {
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public EmployeeResponse signup(SignupRequest request) {
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already taken");
        }

        Employee manager = null;
        if (request.getManagerId() != null) {
            manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID: " + request.getManagerId()));
        }

        Employee employee = Employee.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .department(request.getDepartment())
                .joiningDate(request.getJoiningDate())
                .manager(manager)
                .build();

        Employee savedEmployee = employeeRepository.save(employee);

        // Auto-initialize leave balances for all existing leave types
        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();
        for (LeaveType type : leaveTypes) {
            LeaveBalance balance = LeaveBalance.builder()
                    .employee(savedEmployee)
                    .leaveType(type)
                    .totalDays(type.getDefaultDaysPerYear())
                    .usedDays(0)
                    .remainingDays(type.getDefaultDaysPerYear())
                    .build();
            leaveBalanceRepository.save(balance);
        }

        return mapToEmployeeResponse(savedEmployee);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        Employee employee = employeeRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with email: " + request.getEmail()));

        return AuthResponse.builder()
                .token(token)
                .employeeId(employee.getId())
                .email(employee.getEmail())
                .name(employee.getName())
                .role(employee.getRole())
                .build();
    }

    public EmployeeResponse mapToEmployeeResponse(Employee employee) {
        if (employee == null) return null;
        return EmployeeResponse.builder()
                .id(employee.getId())
                .name(employee.getName())
                .email(employee.getEmail())
                .role(employee.getRole())
                .department(employee.getDepartment())
                .joiningDate(employee.getJoiningDate())
                .managerId(employee.getManager() != null ? employee.getManager().getId() : null)
                .managerName(employee.getManager() != null ? employee.getManager().getName() : null)
                .build();
    }
}
