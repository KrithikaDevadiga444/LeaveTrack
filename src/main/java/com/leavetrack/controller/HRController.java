package com.leavetrack.controller;

import com.leavetrack.dto.EmployeeResponse;
import com.leavetrack.dto.LeaveTypeDto;
import com.leavetrack.model.LeaveType;
import com.leavetrack.service.HRService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr")
public class HRController {

    private final HRService hrService;

    public HRController(HRService hrService) {
        this.hrService = hrService;
    }

    @PostMapping("/leave-types")
    public ResponseEntity<LeaveType> createLeaveType(@Valid @RequestBody LeaveTypeDto leaveTypeDto) {
        return new ResponseEntity<>(hrService.createLeaveType(leaveTypeDto), HttpStatus.CREATED);
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        return ResponseEntity.ok(hrService.getAllEmployees());
    }

    @PostMapping("/reset-balances")
    public ResponseEntity<String> resetBalances() {
        hrService.resetAllBalances();
        return ResponseEntity.ok("All employee leave balances have been reset successfully.");
    }
}
