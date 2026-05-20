package com.revtalent.payroll_service.dto;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor
public class DashboardStatsDTO {
    private int leaveBalance;
    private double attendancePercentage;
    private String nextPayslipDate;
    private double hoursThisWeek;
}