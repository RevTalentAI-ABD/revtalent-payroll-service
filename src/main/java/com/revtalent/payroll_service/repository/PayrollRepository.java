package com.revtalent.payroll_service.repository;

import com.revtalent.payroll_service.model.Employee;
import com.revtalent.payroll_service.model.Payroll;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    List<Payroll> findByEmployee_IdOrderByPayYearDescPayMonthDesc(Long empId);
    Optional<Payroll> findByEmployee_IdAndPayMonthAndPayYear(Long empId, int month, int year);
    List<Payroll> findByPayMonthAndPayYear(int month, int year);
    List<Payroll> findByEmployee_IdAndStatus(Long empId, Payroll.Status status);
    List<Payroll> findByEmployee_Id(Long employeeId);
    List<Payroll> findByEmployee(Employee emp);

    @Query("SELECT p FROM Payroll p WHERE p.payYear = :year AND p.status = :status")
    List<Payroll> findByYearAndStatus(@Param("year") int year, @Param("status") Payroll.Status status);

    @EntityGraph(attributePaths = {"employee", "employee.user", "employee.department"})
    List<Payroll> findAll();
}