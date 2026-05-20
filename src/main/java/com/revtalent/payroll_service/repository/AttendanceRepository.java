package com.revtalent.payroll_service.repository;

import com.revtalent.payroll_service.model.Attendance;
import com.revtalent.payroll_service.model.Employee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByEmployee_IdOrderByWorkDateDesc(Long empId);
    List<Attendance> findByEmployee_IdAndWorkDateBetweenOrderByWorkDateDesc(Long empId, LocalDate from, LocalDate to);
    Optional<Attendance> findByEmployee_IdAndWorkDate(Long empId, LocalDate workDate);
    Optional<Attendance> findByEmployeeAndWorkDate(Employee employee, LocalDate workDate);
    int countByEmployee_IdAndWorkDateBetweenAndStatus(Long empId, LocalDate from, LocalDate to, Attendance.Status status);
    List<Attendance> findByWorkDate(LocalDate workDate);
    List<Attendance> findByEmployee(Employee employee);
    
    long countByWorkDateBetween(LocalDate start, LocalDate end);
    long countByStatusAndWorkDateBetween(Attendance.Status status, LocalDate start, LocalDate end);
    long countByAttendanceTypeAndWorkDateBetween(Attendance.AttendanceType type, LocalDate start, LocalDate end);
    long countByEmployeeAndWorkDateBetween(Employee employee, LocalDate start, LocalDate end);
    long countByEmployeeAndStatusAndWorkDateBetween(Employee employee, Attendance.Status status, LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = {"employee", "employee.user", "employee.department"})
    List<Attendance> findAll();
}
