package com.leavetrack.service;

import com.leavetrack.dto.LeaveBalanceResponse;
import com.leavetrack.dto.LeaveRequestDto;
import com.leavetrack.dto.LeaveRequestResponse;
import com.leavetrack.exception.BadRequestException;
import com.leavetrack.exception.ResourceNotFoundException;
import com.leavetrack.model.*;
import com.leavetrack.repository.EmployeeRepository;
import com.leavetrack.repository.LeaveBalanceRepository;
import com.leavetrack.repository.LeaveRequestRepository;
import com.leavetrack.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveService(
            LeaveRequestRepository leaveRequestRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            EmployeeRepository employeeRepository,
            LeaveTypeRepository leaveTypeRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Transactional
    public LeaveRequestResponse applyLeave(Long employeeId, LeaveRequestDto requestDto) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        LeaveType leaveType = leaveTypeRepository.findById(requestDto.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave Type not found with ID: " + requestDto.getLeaveTypeId()));

        LocalDate startDate = requestDto.getStartDate();
        LocalDate endDate = requestDto.getEndDate();

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }

        int requestedDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (requestedDays <= 0) {
            throw new BadRequestException("Requested leave days must be greater than zero");
        }

        // Check leave balance
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeId(employeeId, leaveType.getId())
                .orElseThrow(() -> new BadRequestException("Leave balance not initialized for type: " + leaveType.getName()));

        if (balance.getRemainingDays() < requestedDays) {
            throw new BadRequestException(String.format("Insufficient leave balance. Remaining: %d, Requested: %d",
                    balance.getRemainingDays(), requestedDays));
        }

        // Check overlap
        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlappingRequests(
                employeeId, startDate, endDate, Arrays.asList(LeaveStatus.PENDING, LeaveStatus.APPROVED));

        if (!overlapping.isEmpty()) {
            throw new BadRequestException("Leave request dates overlap with an existing pending or approved leave request");
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .leaveType(leaveType)
                .startDate(startDate)
                .endDate(endDate)
                .reason(requestDto.getReason())
                .status(LeaveStatus.PENDING)
                .appliedOn(LocalDateTime.now())
                .build();

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        return mapToLeaveRequestResponse(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getMyLeaves(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
        }
        return leaveRequestRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::mapToLeaveRequestResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getMyBalances(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
        }
        return leaveBalanceRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::mapToLeaveBalanceResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getTeamPendingRequests(Long managerId) {
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID: " + managerId));

        if (manager.getRole() != Role.MANAGER && manager.getRole() != Role.HR) {
            throw new BadRequestException("Only managers or HR can view team requests");
        }

        return leaveRequestRepository.findByManagerIdAndStatus(managerId, LeaveStatus.PENDING)
                .stream()
                .map(this::mapToLeaveRequestResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LeaveRequestResponse approveLeave(Long leaveRequestId, Long reviewerId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + leaveRequestId));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Only pending leave requests can be approved");
        }

        Employee reviewer = employeeRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found with ID: " + reviewerId));

        // Authorize: Reviewer must be the employee's manager OR HR
        boolean isManager = leaveRequest.getEmployee().getManager() != null &&
                leaveRequest.getEmployee().getManager().getId().equals(reviewerId);
        boolean isHR = reviewer.getRole() == Role.HR;

        if (!isManager && !isHR) {
            throw new BadRequestException("You are not authorized to approve this leave request");
        }

        int requestedDays = (int) ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1;

        // Deduct balance
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeId(
                leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType().getId())
                .orElseThrow(() -> new BadRequestException("Leave balance not initialized for type: " + leaveRequest.getLeaveType().getName()));

        if (balance.getRemainingDays() < requestedDays) {
            throw new BadRequestException(String.format("Insufficient leave balance at approval time. Remaining: %d, Requested: %d",
                    balance.getRemainingDays(), requestedDays));
        }

        balance.setUsedDays(balance.getUsedDays() + requestedDays);
        balance.setRemainingDays(balance.getRemainingDays() - requestedDays);
        leaveBalanceRepository.save(balance);

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setReviewedBy(reviewer);

        LeaveRequest updatedRequest = leaveRequestRepository.save(leaveRequest);
        return mapToLeaveRequestResponse(updatedRequest);
    }

    @Transactional
    public LeaveRequestResponse rejectLeave(Long leaveRequestId, Long reviewerId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + leaveRequestId));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Only pending leave requests can be rejected");
        }

        Employee reviewer = employeeRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found with ID: " + reviewerId));

        // Authorize: Reviewer must be the employee's manager OR HR
        boolean isManager = leaveRequest.getEmployee().getManager() != null &&
                leaveRequest.getEmployee().getManager().getId().equals(reviewerId);
        boolean isHR = reviewer.getRole() == Role.HR;

        if (!isManager && !isHR) {
            throw new BadRequestException("You are not authorized to reject this leave request");
        }

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setReviewedBy(reviewer);

        LeaveRequest updatedRequest = leaveRequestRepository.save(leaveRequest);
        return mapToLeaveRequestResponse(updatedRequest);
    }

    public LeaveRequestResponse mapToLeaveRequestResponse(LeaveRequest request) {
        if (request == null) return null;
        int requestedDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        return LeaveRequestResponse.builder()
                .id(request.getId())
                .employeeId(request.getEmployee().getId())
                .employeeName(request.getEmployee().getName())
                .employeeEmail(request.getEmployee().getEmail())
                .leaveTypeId(request.getLeaveType().getId())
                .leaveTypeName(request.getLeaveType().getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .requestedDays(requestedDays)
                .reason(request.getReason())
                .status(request.getStatus())
                .appliedOn(request.getAppliedOn())
                .reviewedById(request.getReviewedBy() != null ? request.getReviewedBy().getId() : null)
                .reviewedByName(request.getReviewedBy() != null ? request.getReviewedBy().getName() : null)
                .build();
    }

    public LeaveBalanceResponse mapToLeaveBalanceResponse(LeaveBalance balance) {
        if (balance == null) return null;
        return LeaveBalanceResponse.builder()
                .id(balance.getId())
                .leaveTypeId(balance.getLeaveType().getId())
                .leaveTypeName(balance.getLeaveType().getName())
                .totalDays(balance.getTotalDays())
                .usedDays(balance.getUsedDays())
                .remainingDays(balance.getRemainingDays())
                .build();
    }
}
