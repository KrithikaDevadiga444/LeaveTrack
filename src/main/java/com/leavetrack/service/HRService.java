package com.leavetrack.service;

import com.leavetrack.dto.EmployeeResponse;
import com.leavetrack.dto.LeaveTypeDto;
import com.leavetrack.exception.BadRequestException;
import com.leavetrack.model.Employee;
import com.leavetrack.model.LeaveBalance;
import com.leavetrack.model.LeaveType;
import com.leavetrack.repository.EmployeeRepository;
import com.leavetrack.repository.LeaveBalanceRepository;
import com.leavetrack.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HRService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    public HRService(
            LeaveTypeRepository leaveTypeRepository,
            EmployeeRepository employeeRepository,
            LeaveBalanceRepository leaveBalanceRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
        this.employeeRepository = employeeRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
    }

    @Transactional
    public LeaveType createLeaveType(LeaveTypeDto dto) {
        if (leaveTypeRepository.existsByName(dto.getName())) {
            throw new BadRequestException("Leave Type with this name already exists");
        }

        LeaveType leaveType = LeaveType.builder()
                .name(dto.getName())
                .defaultDaysPerYear(dto.getDefaultDaysPerYear())
                .build();

        LeaveType savedType = leaveTypeRepository.save(leaveType);

        // Auto-initialize balance for all existing employees
        List<Employee> employees = employeeRepository.findAll();
        for (Employee emp : employees) {
            LeaveBalance balance = LeaveBalance.builder()
                    .employee(emp)
                    .leaveType(savedType)
                    .totalDays(savedType.getDefaultDaysPerYear())
                    .usedDays(0)
                    .remainingDays(savedType.getDefaultDaysPerYear())
                    .build();
            leaveBalanceRepository.save(balance);
        }

        return savedType;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(this::mapToEmployeeResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void resetAllBalances() {
        List<LeaveBalance> balances = leaveBalanceRepository.findAll();
        for (LeaveBalance balance : balances) {
            int defaultDays = balance.getLeaveType().getDefaultDaysPerYear();
            balance.setTotalDays(defaultDays);
            balance.setUsedDays(0);
            balance.setRemainingDays(defaultDays);
            leaveBalanceRepository.save(balance);
        }
    }

    private EmployeeResponse mapToEmployeeResponse(Employee employee) {
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
