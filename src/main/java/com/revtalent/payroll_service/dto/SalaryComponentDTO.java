package com.revtalent.payroll_service.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalaryComponentDTO {
    private String label;
    private double amount;
    private String type;
}
