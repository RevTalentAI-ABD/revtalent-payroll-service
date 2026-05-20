package com.revtalent.payroll_service.dto;

import lombok.Data;

@Data
public class EmployeeDTO {
    private Long id;
    private String employeeCode;
    private String designation;
    private String status;
    private String name;
    private String username;
    private String email;
    private String departmentName;
    private Long managerId;
}