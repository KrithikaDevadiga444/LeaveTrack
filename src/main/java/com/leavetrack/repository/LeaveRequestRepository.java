package com.leavetrack.repository;

import com.leavetrack.model.LeaveRequest;
import com.leavetrack.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeId(Long employeeId);

    @Query("SELECT lr FROM LeaveRequest lr JOIN lr.employee e WHERE e.manager.id = :managerId AND lr.status = :status")
    List<LeaveRequest> findByManagerIdAndStatus(@Param("managerId") Long managerId, @Param("status") LeaveStatus status);

    List<LeaveRequest> findByStatus(LeaveStatus status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.id = :employeeId " +
           "AND lr.status IN :statuses " +
           "AND ((lr.startDate <= :endDate AND lr.endDate >= :startDate))")
    List<LeaveRequest> findOverlappingRequests(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<LeaveStatus> statuses
    );
}
