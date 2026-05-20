package com.revtalent.payroll_service.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.revtalent.payroll_service.model.Attendance;
import com.revtalent.payroll_service.model.Payroll;
import com.revtalent.payroll_service.repository.AttendanceRepository;
import com.revtalent.payroll_service.repository.EmployeeRepository;
import com.revtalent.payroll_service.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EmployeeRepository employeeRepo;
    private final AttendanceRepository attendanceRepo;
    private final PayrollRepository payrollRepo;

    // ===============================
    // ✅ SUMMARY (CARDS)
    // ===============================
    public Map<String, Object> getSummary() {

        Map<String, Object> data = new HashMap<>();

        long totalEmployees = employeeRepo.count();
        long inactive = employeeRepo.countByStatus(com.revtalent.payroll_service.model.Employee.Status.INACTIVE);

        double attrition = totalEmployees == 0 ? 0 :
                (inactive * 100.0) / totalEmployees;

        LocalDate now = LocalDate.now();
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());

        long totalAttendance = attendanceRepo.countByWorkDateBetween(start, end);
        long presentAttendance =
                attendanceRepo.countByStatusAndWorkDateBetween(
                        Attendance.Status.PRESENT, start, end
                );

        double avgAttendance = totalAttendance == 0 ? 0 :
                (presentAttendance * 100.0) / totalAttendance;

        List<Payroll> payrolls =
                payrollRepo.findByPayMonthAndPayYear(now.getMonthValue(), now.getYear());

        double productivity = payrolls.stream()
                .map(p -> p.getBasicSalary()
                        .add(p.getHra())
                        .add(p.getAllowances())
                        .subtract(p.getDeductions())
                        .subtract(p.getPfDeduction())
                        .subtract(p.getTaxDeduction()))
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0);

        data.put("totalEmployees", totalEmployees);
        data.put("attritionRate", Math.round(attrition));
        data.put("avgAttendance", Math.round(avgAttendance));
        data.put("productivity", Math.round(productivity));

        return data;
    }

    // ===============================
    // 📊 ATTENDANCE (BAR CHART)
    // ===============================
    public Map<String, Object> getAttendanceData() {

        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();

        LocalDate now = LocalDate.now();
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());

        employeeRepo.findAll().forEach(emp -> {

            String dept = emp.getDepartment().getName();

            long total = attendanceRepo.countByEmployeeAndWorkDateBetween(emp, start, end);

            long present = attendanceRepo.countByEmployeeAndStatusAndWorkDateBetween(
                    emp, Attendance.Status.PRESENT, start, end
            );

            double pct = total == 0 ? 0 : (present * 100.0) / total;

            Map<String, Object> m = new HashMap<>();
            m.put("name", dept);
            m.put("percentage", Math.round(pct));

            list.add(m);
        });

        data.put("departments", list);
        return data;
    }

    // ===============================
    // 📈 PRODUCTIVITY (LINE CHART)
    // ===============================
    public Map<String, Object> getProductivityData() {

        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();

        payrollRepo.findAll().forEach(p -> {

            String month = p.getPayMonth() + "/" + p.getPayYear();

            double net = p.getBasicSalary()
                    .add(p.getHra())
                    .add(p.getAllowances())
                    .subtract(p.getDeductions())
                    .subtract(p.getPfDeduction())
                    .subtract(p.getTaxDeduction())
                    .doubleValue();

            Map<String, Object> m = new HashMap<>();
            m.put("month", month);
            m.put("value", net);

            list.add(m);
        });

        data.put("data", list);
        return data;
    }

    // ===============================
    // 📊 HEADCOUNT TREND
    // ===============================
    public Map<String, Object> getTeamSummary() {

        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Integer> map = new HashMap<>();

        payrollRepo.findAll().forEach(p -> {
            String month = p.getPayMonth() + "/" + p.getPayYear();
            map.put(month, map.getOrDefault(month, 0) + 1);
        });

        map.forEach((k, v) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("month", k);
            m.put("count", v);
            list.add(m);
        });

        data.put("monthlyTrend", list);
        return data;
    }

    // ===============================
    // 🔥 UNIFIED API
    // ===============================
    public Map<String, Object> getAllReports() {

        Map<String, Object> data = new HashMap<>();

        data.putAll(getSummary());
        data.put("attendanceByDept", getAttendanceData().get("departments"));
        data.put("productivityTrend", getProductivityData().get("data"));
        data.put("headcountTrend", getTeamSummary().get("monthlyTrend"));

        return data;
    }

    // ===============================
    // 📄 PDF EXPORT
    // ===============================
    public byte[] generateReportsPdf() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document document = new Document();
            PdfWriter.getInstance(document, out);

            document.open();
            document.add(new Paragraph("Reports Summary"));
            document.add(new Paragraph("Generated dynamically"));
            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
