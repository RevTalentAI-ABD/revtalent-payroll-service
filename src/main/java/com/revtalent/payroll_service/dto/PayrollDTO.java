package com.revtalent.payroll_service.dto;

import com.revtalent.payroll_service.model.Payroll;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PayrollDTO {
    private Integer payMonth;
    private Integer payYear;
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal allowances;
    private BigDecimal deductions;
    private BigDecimal pfDeduction;
    private BigDecimal taxDeduction;
    private Payroll.Status status;
}