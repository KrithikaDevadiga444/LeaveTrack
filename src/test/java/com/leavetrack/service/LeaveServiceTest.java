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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @InjectMocks
    private LeaveService leaveService;

    private Employee employee;
    private Employee manager;
    private Employee hr;
    private LeaveType sickLeave;
    private LeaveBalance sickBalance;
    private LeaveRequestDto requestDto;

    @BeforeEach
    void setUp() {
        manager = Employee.builder()
                .id(2L)
                .name("Manager Bob")
                .email("manager@test.com")
                .role(Role.MANAGER)
                .build();

        hr = Employee.builder()
                .id(3L)
                .name("HR Alice")
                .email("hr@test.com")
                .role(Role.HR)
                .build();

        employee = Employee.builder()
                .id(1L)
                .name("Employee John")
                .email("john@test.com")
                .role(Role.EMPLOYEE)
                .manager(manager)
                .build();

        sickLeave = LeaveType.builder()
                .id(10L)
                .name("Sick")
                .defaultDaysPerYear(10)
                .build();

        sickBalance = LeaveBalance.builder()
                .id(100L)
                .employee(employee)
                .leaveType(sickLeave)
                .totalDays(10)
                .usedDays(2)
                .remainingDays(8)
                .build();

        requestDto = new LeaveRequestDto();
        requestDto.setLeaveTypeId(10L);
        requestDto.setStartDate(LocalDate.now().plusDays(5));
        requestDto.setEndDate(LocalDate.now().plusDays(7)); // 3 days
        requestDto.setReason("Sick leave request");
    }

    @Test
    void applyLeave_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(10L)).thenReturn(Optional.of(sickLeave));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeId(1L, 10L)).thenReturn(Optional.of(sickBalance));
        when(leaveRequestRepository.findOverlappingRequests(eq(1L), any(), any(), any())).thenReturn(new ArrayList<>());

        LeaveRequest mockSavedRequest = LeaveRequest.builder()
                .id(1L)
                .employee(employee)
                .leaveType(sickLeave)
                .startDate(requestDto.getStartDate())
                .endDate(requestDto.getEndDate())
                .reason(requestDto.getReason())
                .status(LeaveStatus.PENDING)
                .appliedOn(LocalDateTime.now())
                .build();

        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(mockSavedRequest);

        LeaveRequestResponse response = leaveService.applyLeave(1L, requestDto);

        assertNotNull(response);
        assertEquals(LeaveStatus.PENDING, response.getStatus());
        assertEquals(3, response.getRequestedDays());
        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
    }

    @Test
    void applyLeave_EmployeeNotFound() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveService.applyLeave(1L, requestDto));
    }

    @Test
    void applyLeave_LeaveTypeNotFound() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveService.applyLeave(1L, requestDto));
    }

    @Test
    void applyLeave_StartDateAfterEndDate() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(10L)).thenReturn(Optional.of(sickLeave));

        requestDto.setStartDate(LocalDate.now().plusDays(5));
        requestDto.setEndDate(LocalDate.now().plusDays(3)); // Invalid

        assertThrows(BadRequestException.class, () -> leaveService.applyLeave(1L, requestDto));
    }

    @Test
    void applyLeave_InsufficientBalance() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(10L)).thenReturn(Optional.of(sickLeave));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeId(1L, 10L)).thenReturn(Optional.of(sickBalance)); // 8 days remaining

        requestDto.setStartDate(LocalDate.now().plusDays(5));
        requestDto.setEndDate(LocalDate.now().plusDays(15)); // 11 days (Requested > 8)

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> leaveService.applyLeave(1L, requestDto));
        assertTrue(exception.getMessage().contains("Insufficient leave balance"));
    }

    @Test
    void applyLeave_OverlappingDates() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(10L)).thenReturn(Optional.of(sickLeave));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeId(1L, 10L)).thenReturn(Optional.of(sickBalance));

        List<LeaveRequest> overlappingList = Collections.singletonList(new LeaveRequest());
        when(leaveRequestRepository.findOverlappingRequests(eq(1L), any(), any(), any())).thenReturn(overlappingList);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> leaveService.applyLeave(1L, requestDto));
        assertTrue(exception.getMessage().contains("overlap"));
    }

    @Test
    void getMyLeaves_Success() {
        when(employeeRepository.existsById(1L)).thenReturn(true);
        when(leaveRequestRepository.findByEmployeeId(1L)).thenReturn(new ArrayList<>());

        List<LeaveRequestResponse> list = leaveService.getMyLeaves(1L);
        assertNotNull(list);
    }

    @Test
    void getMyBalances_Success() {
        when(employeeRepository.existsById(1L)).thenReturn(true);
        when(leaveBalanceRepository.findByEmployeeId(1L)).thenReturn(Collections.singletonList(sickBalance));

        List<LeaveBalanceResponse> balances = leaveService.getMyBalances(1L);
        assertEquals(1, balances.size());
        assertEquals("Sick", balances.get(0).getLeaveTypeName());
    }

    @Test
    void getTeamPendingRequests_Success() {
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.findByManagerIdAndStatus(2L, LeaveStatus.PENDING)).thenReturn(new ArrayList<>());

        List<LeaveRequestResponse> requests = leaveService.getTeamPendingRequests(2L);
        assertNotNull(requests);
    }

    @Test
    void getTeamPendingRequests_NotManagerOrHR() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee)); // Role is EMPLOYEE

        assertThrows(BadRequestException.class, () -> leaveService.getTeamPendingRequests(1L));
    }

    @Test
    void approveLeave_Success_Manager() {
        LeaveRequest pendingRequest = LeaveRequest.builder()
                .id(100L)
                .employee(employee)
                .leaveType(sickLeave)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3)) // 3 days
                .status(LeaveStatus.PENDING)
                .build();

        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(manager)); // Reviewer is manager
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeId(1L, 10L)).thenReturn(Optional.of(sickBalance)); // Remaining 8
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(pendingRequest);

        LeaveRequestResponse response = leaveService.approveLeave(100L, 2L);

        assertNotNull(response);
        assertEquals(LeaveStatus.APPROVED, response.getStatus());
        assertEquals(2L, response.getReviewedById());
        assertEquals(5, sickBalance.getUsedDays()); // 2 + 3
        assertEquals(5, sickBalance.getRemainingDays()); // 8 - 3
    }

    @Test
    void approveLeave_Success_HR() {
        LeaveRequest pendingRequest = LeaveRequest.builder()
                .id(100L)
                .employee(employee)
                .leaveType(sickLeave)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3)) // 3 days
                .status(LeaveStatus.PENDING)
                .build();

        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(hr)); // Reviewer is HR
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeId(1L, 10L)).thenReturn(Optional.of(sickBalance)); // Remaining 8
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(pendingRequest);

        LeaveRequestResponse response = leaveService.approveLeave(100L, 3L);

        assertNotNull(response);
        assertEquals(LeaveStatus.APPROVED, response.getStatus());
        assertEquals(3L, response.getReviewedById());
    }

    @Test
    void approveLeave_NotAuthorizedReviewer() {
        LeaveRequest pendingRequest = LeaveRequest.builder()
                .id(100L)
                .employee(employee)
                .leaveType(sickLeave)
                .status(LeaveStatus.PENDING)
                .build();

        Employee anotherEmployee = Employee.builder()
                .id(4L)
                .role(Role.EMPLOYEE)
                .build();

        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(anotherEmployee));

        assertThrows(BadRequestException.class, () -> leaveService.approveLeave(100L, 4L));
    }

    @Test
    void rejectLeave_Success() {
        LeaveRequest pendingRequest = LeaveRequest.builder()
                .id(100L)
                .employee(employee)
                .leaveType(sickLeave)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(LeaveStatus.PENDING)
                .build();

        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(pendingRequest);

        LeaveRequestResponse response = leaveService.rejectLeave(100L, 2L);

        assertNotNull(response);
        assertEquals(LeaveStatus.REJECTED, response.getStatus());
        assertEquals(2, sickBalance.getUsedDays()); // No change in balance
        assertEquals(8, sickBalance.getRemainingDays()); // No change
    }

    @Test
    void rejectLeave_NotPending() {
        LeaveRequest approvedRequest = LeaveRequest.builder()
                .id(100L)
                .status(LeaveStatus.APPROVED)
                .build();

        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(approvedRequest));

        assertThrows(BadRequestException.class, () -> leaveService.rejectLeave(100L, 2L));
    }
}
