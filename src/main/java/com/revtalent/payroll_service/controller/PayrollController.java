package com.revtalent.payroll_service.controller;

import com.revtalent.payroll_service.dto.PayrollDTO;
import com.revtalent.payroll_service.dto.PayrollResponse;
import com.revtalent.payroll_service.model.Payroll;
import com.revtalent.payroll_service.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
public class PayrollController {

    private final PayrollService payrollService;

    // ── Employee endpoints ────────────────────────────────────────────────────

    @GetMapping("/employee/{empId}")
    public ResponseEntity<List<PayrollResponse>> getByEmployee(@PathVariable Long empId) {
        return ResponseEntity.ok(payrollService.getByEmployee(empId));
    }

    @GetMapping("/employee/{empId}/month")
    public ResponseEntity<PayrollResponse> getByMonth(
            @PathVariable Long empId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(payrollService.getByEmployeeAndMonth(empId, month, year));
    }

    @GetMapping("/employee/{empId}/status")
    public ResponseEntity<List<PayrollResponse>> getByStatus(
            @PathVariable Long empId,
            @RequestParam Payroll.Status status) {
        return ResponseEntity.ok(payrollService.getByStatus(empId, status));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HR_ADMIN')")
    @PostMapping("/employee/{empId}")
    public ResponseEntity<PayrollResponse> create(
            @PathVariable Long empId,
            @RequestBody PayrollDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.create(empId, dto));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HR_ADMIN')")
    @PutMapping("/{payrollId}")
    public ResponseEntity<PayrollResponse> update(
            @PathVariable Long payrollId,
            @RequestBody PayrollDTO dto) {
        return ResponseEntity.ok(payrollService.update(payrollId, dto));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HR_ADMIN')")
    @PutMapping("/{payrollId}/process")
    public ResponseEntity<PayrollResponse> processSingle(@PathVariable Long payrollId) {
        return ResponseEntity.ok(payrollService.process(payrollId));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HR_ADMIN')")
    @PutMapping("/{payrollId}/pay")
    public ResponseEntity<PayrollResponse> markPaid(@PathVariable Long payrollId) {
        return ResponseEntity.ok(payrollService.markPaid(payrollId));
    }

    // ── Month endpoints ───────────────────────────────────────────────────────

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HR_ADMIN')")
    @GetMapping("/month")
    public ResponseEntity<List<PayrollResponse>> getAllByMonth(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(payrollService.getByMonth(month, year));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HR_ADMIN')")
    @PutMapping("/bulk-process")
    public ResponseEntity<List<PayrollResponse>> bulkProcess(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(payrollService.bulkProcess(month, year));
    }

    // ── HR / Manager endpoints ────────────────────────────────────────────────

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HR_ADMIN')")
    @GetMapping
    public ResponseEntity<List<PayrollResponse>> getAll() {
        return ResponseEntity.ok(payrollService.getAll());
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HR_ADMIN')")
    @PostMapping("/generate")
    public ResponseEntity<List<PayrollResponse>> generate(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(payrollService.generatePayroll(month, year));
    }



    @GetMapping("/salary-slip/{id}")
    public ResponseEntity<byte[]> downloadSalarySlip(@PathVariable Long id) {

        byte[] pdfBytes = payrollService.generateSalarySlip(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=salary-slip.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }


    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HR_ADMIN')")
    @PostMapping("/payroll/{id}/process")
    public ResponseEntity<?> processOne(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.processSingle(id));
    }
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HR_ADMIN')")
    @PostMapping("/process-all")
    public ResponseEntity<?> processAllPayroll() {
        List<Payroll> data = payrollService.processAllPayroll();
        return ResponseEntity.ok(data);
    }



}