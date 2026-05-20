package com.revtalent.payroll_service.repository;

import com.revtalent.payroll_service.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;  // ADD this import
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByEmployee_IdOrderByCreatedAtDesc(Long empId);

    List<Notification> findByEmployee_IdAndReadFalse(Long empId);

    int countByEmployee_IdAndReadFalse(Long empId);

    List<Notification> findByReadFalse();

    List<Notification> findByType(Notification.Type type);
    List<Notification> findByEmployee_Manager_Id(Long managerId);
    List<Notification> findByEmployee_Manager_IdAndReadFalse(Long managerId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.employee.id = :empId")
    void markAllAsReadByEmployeeId(@Param("empId") Long empId);  // ADD @Param here
}
