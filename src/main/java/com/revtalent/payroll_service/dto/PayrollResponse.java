package com.revtalent.payroll_service.dto;

import com.revtalent.payroll_service.model.Payroll;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @AllArgsConstructor @NoArgsConstructor
public class PayrollResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String departmentName;
    private Integer payMonth;
    private Integer payYear;
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal allowances;
    private BigDecimal deductions;
    private BigDecimal pfDeduction;
    private BigDecimal taxDeduction;
    private BigDecimal netPay;
    private Payroll.Status status;
    private LocalDateTime processedAt;
    private BigDecimal netSalary;
}