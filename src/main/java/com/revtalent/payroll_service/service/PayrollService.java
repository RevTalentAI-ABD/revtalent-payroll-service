package com.revtalent.payroll_service.service;

import com.revtalent.payroll_service.dto.PayrollDTO;
import com.revtalent.payroll_service.dto.PayrollResponse;
import com.revtalent.payroll_service.model.Employee;
import com.revtalent.payroll_service.model.Payroll;
import com.revtalent.payroll_service.repository.EmployeeRepository;
import com.revtalent.payroll_service.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;

    // ── Private Mapper ────────────────────────────────────────────────────────

    private PayrollResponse mapToResponse(Payroll p) {
        PayrollResponse res = new PayrollResponse();
        res.setId(p.getId());
        res.setEmployeeId(p.getEmployee().getId());
        res.setEmployeeName(p.getEmployee().getUser() != null ? p.getEmployee().getUser().getName() : "Unknown");
        res.setEmployeeCode(p.getEmployee().getEmployeeCode());
        res.setDepartmentName(p.getEmployee().getDepartment() != null ? p.getEmployee().getDepartment().getName() : "N/A");
        res.setPayMonth(p.getPayMonth());
        res.setPayYear(p.getPayYear());
        res.setBasicSalary(p.getBasicSalary());
        res.setHra(p.getHra());
        res.setAllowances(p.getAllowances());
        res.setDeductions(p.getDeductions());
        res.setPfDeduction(p.getPfDeduction());
        res.setTaxDeduction(p.getTaxDeduction());
        res.setNetPay(p.getNetPay());
        res.setNetSalary(p.getNetPay());
        res.setStatus(p.getStatus());
        res.setProcessedAt(p.getProcessedAt());
        return res;
    }

    // ── Generate Payroll (bulk for all employees) ─────────────────────────────

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public synchronized List<PayrollResponse> generatePayroll(int month, int year) {
        List<Employee> employees = employeeRepository.findAll();
        List<Payroll> result = new ArrayList<>();

        for (Employee emp : employees) {
            boolean exists = payrollRepository.findByEmployee(emp).stream()
                    .anyMatch(p -> p.getPayMonth() == month && p.getPayYear() == year);

            if (exists) continue;

            Payroll template = payrollRepository.findByEmployee_IdOrderByPayYearDescPayMonthDesc(emp.getId())
                    .stream().findFirst().orElse(null);

            BigDecimal basic = template != null ? orZero(template.getBasicSalary()) : BigDecimal.valueOf(50000);
            BigDecimal hra = template != null ? orZero(template.getHra()) : BigDecimal.valueOf(10000);
            BigDecimal allowances = template != null ? orZero(template.getAllowances()) : BigDecimal.valueOf(5000);
            BigDecimal deductions = template != null ? orZero(template.getDeductions()) : BigDecimal.valueOf(2000);
            BigDecimal pf = template != null ? orZero(template.getPfDeduction()) : BigDecimal.valueOf(1500);
            BigDecimal tax = template != null ? orZero(template.getTaxDeduction()) : BigDecimal.valueOf(3000);

            BigDecimal net = basic.add(hra).add(allowances)
                    .subtract(deductions).subtract(pf).subtract(tax);
            if (net.compareTo(BigDecimal.ZERO) <= 0) continue;

            Payroll p = Payroll.builder()
                    .employee(emp)
                    .payMonth(month)
                    .payYear(year)
                    .basicSalary(basic)
                    .hra(hra)
                    .allowances(allowances)
                    .deductions(deductions)
                    .pfDeduction(pf)
                    .taxDeduction(tax)
                    .netPay(net)
                    .status(Payroll.Status.PENDING)
                    .build();

            result.add(p);
        }

        return payrollRepository.saveAll(result)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Get All Payrolls (HR/Manager view) ────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PayrollResponse> getAll() {
        return payrollRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Get All Payrolls for a specific month/year (HR view) ──────────────────

    @Transactional(readOnly = true)
    public List<PayrollResponse> getByMonth(int month, int year) {
        return payrollRepository.findByPayMonthAndPayYear(month, year)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Employee-scoped endpoints ─────────────────────────────────────────────

    private void verifyEmployeeAccess(Long empId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new RuntimeException("Access Denied");
        }
        boolean isPrivileged = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR_ADMIN"));
        if (isPrivileged) return;
        Employee emp = employeeRepository.findByUser_Username(auth.getName()).orElse(null);
        if (emp == null || !emp.getId().equals(empId)) {
            throw new RuntimeException("Access Denied");
        }
    }

    @Transactional(readOnly = true)
    public List<PayrollResponse> getByEmployee(Long empId) {
        verifyEmployeeAccess(empId);
        Employee emp = employeeRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + empId));
        return payrollRepository.findByEmployee_IdOrderByPayYearDescPayMonthDesc(empId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayrollResponse getByEmployeeAndMonth(Long empId, int month, int year) {
        verifyEmployeeAccess(empId);
        Payroll p = payrollRepository
                .findByEmployee_IdAndPayMonthAndPayYear(empId, month, year)
                .orElseThrow(() -> new RuntimeException(
                        "Payroll not found for employee " + empId + " — " + month + "/" + year));
        return mapToResponse(p);
    }

    @Transactional(readOnly = true)
    public List<PayrollResponse> getByStatus(Long empId, Payroll.Status status) {
        verifyEmployeeAccess(empId);
        return payrollRepository.findByEmployee_IdAndStatus(empId, status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional
    public PayrollResponse create(Long empId, PayrollDTO dto) {
        payrollRepository
                .findByEmployee_IdAndPayMonthAndPayYear(empId, dto.getPayMonth(), dto.getPayYear())
                .ifPresent(p -> { throw new RuntimeException(
                        "Payroll already exists for " + dto.getPayMonth() + "/" + dto.getPayYear()); });

        Employee emp = employeeRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + empId));

        Payroll payroll = buildPayroll(emp, dto);
        return mapToResponse(payrollRepository.save(payroll));
    }

    @Transactional
    public PayrollResponse update(Long payrollId, PayrollDTO dto) {
        Payroll payroll = findOrThrow(payrollId);

        if (payroll.getStatus() == Payroll.Status.PAID) {
            throw new RuntimeException("Cannot update a PAID payroll");
        }

        payroll.setBasicSalary(orZero(dto.getBasicSalary()));
        payroll.setHra(orZero(dto.getHra()));
        payroll.setAllowances(orZero(dto.getAllowances()));
        payroll.setDeductions(orZero(dto.getDeductions()));
        payroll.setPfDeduction(orZero(dto.getPfDeduction()));
        payroll.setTaxDeduction(orZero(dto.getTaxDeduction()));
        payroll.setNetPay(calculateNet(dto));

        if (dto.getStatus() != null) {
            payroll.setStatus(dto.getStatus());
        }

        return mapToResponse(payrollRepository.save(payroll));
    }

    @Transactional
    public void delete(Long payrollId) {
        Payroll payroll = findOrThrow(payrollId);

        if (payroll.getStatus() != Payroll.Status.PENDING) {
            throw new RuntimeException("Only PENDING payrolls can be deleted");
        }

        payrollRepository.deleteById(payrollId);
    }

    // ── Status Transitions ────────────────────────────────────────────────────



    @Transactional
    public PayrollResponse process(Long payrollId) {
        Payroll payroll = findOrThrow(payrollId);

        if (payroll.getStatus() != Payroll.Status.PENDING) {
            throw new RuntimeException("Only PENDING payrolls can be processed");
        }

        recalculateNetPay(payroll);
        payroll.setStatus(Payroll.Status.PROCESSED);
        return mapToResponse(payrollRepository.save(payroll));
    }

    @Transactional
    public PayrollResponse markPaid(Long payrollId) {
        Payroll payroll = findOrThrow(payrollId);

        if (payroll.getStatus() != Payroll.Status.PROCESSED) {
            throw new RuntimeException("Only PROCESSED payrolls can be marked as PAID");
        }

        payroll.setStatus(Payroll.Status.PAID);
        return mapToResponse(payrollRepository.save(payroll));
    }

    @Transactional
    public List<PayrollResponse> bulkProcess(int month, int year) {
        List<Payroll> pending = payrollRepository.findByPayMonthAndPayYear(month, year)
                .stream()
                .filter(p -> p.getStatus() == Payroll.Status.PENDING)
                .toList();

        if (pending.isEmpty()) {
            throw new RuntimeException("No PENDING payrolls found for " + month + "/" + year);
        }

        pending.forEach(p -> p.setStatus(Payroll.Status.PROCESSED));
        return payrollRepository.saveAll(pending)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Payroll findOrThrow(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found: " + id));
    }

    private Payroll buildPayroll(Employee emp, PayrollDTO dto) {
        BigDecimal net = calculateNet(dto);
        return Payroll.builder()
                .employee(emp)
                .payMonth(dto.getPayMonth())
                .payYear(dto.getPayYear())
                .basicSalary(orZero(dto.getBasicSalary()))
                .hra(orZero(dto.getHra()))
                .allowances(orZero(dto.getAllowances()))
                .deductions(orZero(dto.getDeductions()))
                .pfDeduction(orZero(dto.getPfDeduction()))
                .taxDeduction(orZero(dto.getTaxDeduction()))
                .netPay(net)
                .status(dto.getStatus() != null ? dto.getStatus() : Payroll.Status.PENDING)
                .build();
    }

    private BigDecimal calculateNet(PayrollDTO dto) {
        return orZero(dto.getBasicSalary())
                .add(orZero(dto.getHra()))
                .add(orZero(dto.getAllowances()))
                .subtract(orZero(dto.getDeductions()))
                .subtract(orZero(dto.getPfDeduction()))
                .subtract(orZero(dto.getTaxDeduction()));
    }

    private BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private void recalculateNetPay(Payroll p) {
        BigDecimal net = orZero(p.getBasicSalary())
                .add(orZero(p.getHra()))
                .add(orZero(p.getAllowances()))
                .subtract(orZero(p.getDeductions()))
                .subtract(orZero(p.getPfDeduction()))
                .subtract(orZero(p.getTaxDeduction()));
        p.setNetPay(net);
    }


    public byte[] generateSalarySlip(Long id) {
        try {
            Payroll payroll = findOrThrow(id);
            verifyEmployeeAccess(payroll.getEmployee().getId());
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document document = new Document();
            PdfWriter.getInstance(document, out);

            document.open();

            document.add(new Paragraph("Salary Slip"));
            document.add(new Paragraph("Employee Name: " + (payroll.getEmployee().getUser() != null ? payroll.getEmployee().getUser().getName() : "Unknown")));
            document.add(new Paragraph("Salary: " + payroll.getNetPay()));

            document.close();

            return out.toByteArray(); // 🔥 IMPORTANT

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }public byte[] generateSlip(Long id) {

        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));
        verifyEmployeeAccess(payroll.getEmployee().getId());
        
        String content = "Salary Slip\n\n" +
                "Employee: " + (payroll.getEmployee().getUser() != null ? payroll.getEmployee().getUser().getName() : "Unknown") + "\n" +
                "Basic: " + payroll.getBasicSalary() + "\n" +
                "Net Pay: " + payroll.getNetPay();

        return content.getBytes(); // temporary (text file)
    }


    public List<Payroll> processAllPayroll() {

        List<Payroll> list = payrollRepository.findAll();
        List<Payroll> processed = new ArrayList<>();

        for (Payroll p : list) {
            if (p.getStatus() == Payroll.Status.PAID) continue;

            recalculateNetPay(p);
            p.setStatus(Payroll.Status.PROCESSED);
            processed.add(p);
        }

        return payrollRepository.saveAll(processed);
    }

    public Payroll processSingle(Long id) {

        Payroll p = payrollRepository.findById(id).orElseThrow();
        if (p.getStatus() == Payroll.Status.PAID) {
            throw new RuntimeException("Cannot process a PAID payroll");
        }

        recalculateNetPay(p);
        p.setStatus(Payroll.Status.PROCESSED);

        return payrollRepository.save(p);
    }



}
