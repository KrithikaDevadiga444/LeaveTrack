package com.leavetrack.service;

import com.leavetrack.model.LeaveRequest;
import com.leavetrack.model.LeaveStatus;
import com.leavetrack.repository.LeaveRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveScheduler {

    private static final Logger log = LoggerFactory.getLogger(LeaveScheduler.class);

    private final LeaveRequestRepository leaveRequestRepository;
    private final HRService hrService;

    public LeaveScheduler(LeaveRequestRepository leaveRequestRepository, HRService hrService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.hrService = hrService;
    }

    /**
     * Auto-reject pending leave requests if the start date is less than 2 days away.
     * Runs daily at 1:00 AM.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void autoRejectStaleRequests() {
        log.info("Starting scheduled task: Auto-reject stale leave requests");
        List<LeaveRequest> pendingRequests = leaveRequestRepository.findByStatus(LeaveStatus.PENDING);
        LocalDate limitDate = LocalDate.now().plusDays(2);
        int rejectCount = 0;

        for (LeaveRequest req : pendingRequests) {
            if (req.getStartDate().isBefore(limitDate)) {
                req.setStatus(LeaveStatus.REJECTED);
                leaveRequestRepository.save(req);
                rejectCount++;
                log.info("Auto-rejected pending leave request ID: {} because start date ({}) is within 2 days",
                        req.getId(), req.getStartDate());
            }
        }
        log.info("Finished auto-reject scheduled task. Rejected {} requests", rejectCount);
    }

    /**
     * Reset all employee leave balances to default yearly values.
     * Scheduled to run at midnight on January 1st.
     */
    @Scheduled(cron = "0 0 0 1 1 ?")
    @Transactional
    public void yearlyResetBalances() {
        log.info("Starting scheduled task: Yearly leave balance reset");
        hrService.resetAllBalances();
        log.info("Finished yearly leave balance reset");
    }
}
