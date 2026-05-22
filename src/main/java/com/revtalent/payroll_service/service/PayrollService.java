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
        res.setEmployeeName(p.getEmployee().getUser().getName());
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

    @Transactional
    public List<PayrollResponse> generatePayroll(int month, int year) {
        List<Employee> employees = employeeRepository.findAll();
        List<Payroll> result = new ArrayList<>();

        for (Employee emp : employees) {
            boolean exists = payrollRepository.findByEmployee(emp).stream()
                    .anyMatch(p -> p.getPayMonth() == month && p.getPayYear() == year);

            if (exists) continue;

            Payroll p = Payroll.builder()
                    .employee(emp)
                    .payMonth(month)
                    .payYear(year)
                    .basicSalary(BigDecimal.valueOf(50000))
                    .hra(BigDecimal.valueOf(10000))
                    .allowances(BigDecimal.valueOf(5000))
                    .deductions(BigDecimal.valueOf(2000))
                    .pfDeduction(BigDecimal.valueOf(1500))
                    .taxDeduction(BigDecimal.valueOf(3000))
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

    @Transactional(readOnly = true)
    public List<PayrollResponse> getByEmployee(Long empId) {
        Employee emp = employeeRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + empId));
        return payrollRepository.findByEmployee_IdOrderByPayYearDescPayMonthDesc(empId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayrollResponse getByEmployeeAndMonth(Long empId, int month, int year) {
        Payroll p = payrollRepository
                .findByEmployee_IdAndPayMonthAndPayYear(empId, month, year)
                .orElseThrow(() -> new RuntimeException(
                        "Payroll not found for employee " + empId + " — " + month + "/" + year));
        return mapToResponse(p);
    }

    @Transactional(readOnly = true)
    public List<PayrollResponse> getByStatus(Long empId, Payroll.Status status) {
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


    public byte[] generateSalarySlip(Long id) {

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document document = new Document();
            PdfWriter.getInstance(document, out);

            document.open();

            document.add(new Paragraph("Salary Slip"));
            document.add(new Paragraph("Employee ID: " + id));
            document.add(new Paragraph("Salary: 70000"));

            document.close();

            return out.toByteArray(); // 🔥 IMPORTANT

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }public byte[] generateSlip(Long id) {

        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));

        String content = "Salary Slip\n\n" +
                "Employee: " + payroll.getEmployee().getUser().getName() + "\n" +
                "Basic: " + payroll.getBasicSalary() + "\n" +
                "Net Pay: " + payroll.getNetPay();

        return content.getBytes(); // temporary (text file)
    }


    public List<Payroll> processAllPayroll() {

        List<Payroll> list = payrollRepository.findAll();

        for (Payroll p : list) {

            BigDecimal net = p.getBasicSalary()
                    .add(p.getHra())
                    .add(p.getAllowances())
                    .subtract(p.getDeductions())
                    .subtract(p.getPfDeduction())
                    .subtract(p.getTaxDeduction());


            p.setNetPay(net);
            p.setStatus(Payroll.Status.PROCESSED);
        }

        return payrollRepository.saveAll(list);
    }

    public Payroll processSingle(Long id) {

        Payroll p = payrollRepository.findById(id).orElseThrow();

        BigDecimal net = p.getBasicSalary()
                .add(p.getHra())
                .add(p.getAllowances())
                .subtract(p.getDeductions())
                .subtract(p.getPfDeduction())
                .subtract(p.getTaxDeduction());

        p.setNetPay(net);
        p.setStatus(Payroll.Status.PROCESSED);

        return payrollRepository.save(p);
    }



}
