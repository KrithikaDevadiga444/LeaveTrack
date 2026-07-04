package com.leavetrack.controller;

import com.leavetrack.dto.LeaveBalanceResponse;
import com.leavetrack.dto.LeaveRequestDto;
import com.leavetrack.dto.LeaveRequestResponse;
import com.leavetrack.security.CustomUserDetails;
import com.leavetrack.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping("/apply")
    public ResponseEntity<LeaveRequestResponse> applyLeave(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LeaveRequestDto requestDto) {
        return new ResponseEntity<>(leaveService.applyLeave(userDetails.getId(), requestDto), HttpStatus.CREATED);
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequestResponse>> getMyLeaves(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(leaveService.getMyLeaves(userDetails.getId()));
    }

    @GetMapping("/balance")
    public ResponseEntity<List<LeaveBalanceResponse>> getMyBalances(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(leaveService.getMyBalances(userDetails.getId()));
    }

    @GetMapping("/team")
    public ResponseEntity<List<LeaveRequestResponse>> getTeamPendingRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(leaveService.getTeamPendingRequests(userDetails.getId()));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<LeaveRequestResponse> approveLeave(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(leaveService.approveLeave(id, userDetails.getId()));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<LeaveRequestResponse> rejectLeave(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(leaveService.rejectLeave(id, userDetails.getId()));
    }
}
