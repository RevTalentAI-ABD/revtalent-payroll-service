package com.revtalent.payroll_service.feign;

import com.revtalent.payroll_service.dto.EmployeeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "employee-service")
public interface EmployeeClient {

    @GetMapping("/api/employees/{id}")
    EmployeeDTO getEmployeeById(@PathVariable("id") Long id);

    @GetMapping("/api/employees/username/{username}")
    EmployeeDTO getEmployeeByUsername(@PathVariable("username") String username);
}