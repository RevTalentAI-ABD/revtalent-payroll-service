package com.revtalent.payroll_service.controller;

import com.revtalent.payroll_service.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ReportController {
    private final ReportService reportService;


    // ✅ Summary
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        return reportService.getSummary();
    }

    // ✅ Team Summary
    @GetMapping("/team-summary")
    public Map<String, Object> getTeamSummary() {
        return reportService.getTeamSummary();
    }

    // ✅ All Reports
    @GetMapping("/all")
    public Map<String, Object> getAllReports() {
        return reportService.getAllReports();
    }
}
