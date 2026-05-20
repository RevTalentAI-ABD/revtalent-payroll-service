package com.revtalent.payroll_service.service;

import com.revtalent.payroll_service.dto.NotificationResponseDTO;
import com.revtalent.payroll_service.dto.notification.NotificationRequest;
import com.revtalent.payroll_service.dto.notification.NotificationResponse;
import com.revtalent.payroll_service.model.Employee;
import com.revtalent.payroll_service.model.Notification;
import com.revtalent.payroll_service.repository.EmployeeRepository;
import com.revtalent.payroll_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;

    // ── Private helper ────────────────────────────────────────────────────────

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .message(n.getMessage())
                .type(n.getType() != null ? n.getType().name() : null)
                .unread(!n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    // ── Create Notification ───────────────────────────────────────────────────

    @Transactional
    public Notification create(NotificationRequest dto) {
        Notification n = new Notification();
        n.setMessage(dto.getMessage());
        n.setType(Notification.Type.valueOf(dto.getType()));
        return notificationRepository.save(n);
    }

    // ── Employee endpoints (empId scoped) ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getNotifications(Long empId) {
        return notificationRepository.findByEmployee_IdOrderByCreatedAtDesc(empId)
                .stream()
                .map(NotificationResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getUnreadNotifications(Long empId) {
        return notificationRepository.findByEmployee_IdAndReadFalse(empId)
                .stream()
                .map(NotificationResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(Long empId) {
        return notificationRepository.countByEmployee_IdAndReadFalse(empId);
    }

    @Transactional
    public NotificationResponseDTO markAsRead(Long notifId) {
        Notification notification = notificationRepository.findById(notifId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notifId));
        notification.setRead(true);
        return NotificationResponseDTO.from(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(Long empId) {
        notificationRepository.markAllAsReadByEmployeeId(empId);
    }

    @Transactional
    public void deleteNotification(Long notifId) {
        if (!notificationRepository.existsById(notifId)) {
            throw new RuntimeException("Notification not found: " + notifId);
        }
        notificationRepository.deleteById(notifId);
    }

    // ── Manager endpoints (all notifications, no empId scope) ─────────────────

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllUnreadNotifications() {
        return notificationRepository.findByReadFalse().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAllAsReadGlobal() {
        List<Notification> unread = notificationRepository.findByReadFalse();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotificationsForManager(String username) {
        Employee manager = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        return notificationRepository.findByEmployee_Manager_Id(manager.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllUnreadNotificationsForManager(String username) {
        Employee manager = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        return notificationRepository.findByEmployee_Manager_IdAndReadFalse(manager.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Targeted helpers used by Leave & Announcement flows ───────────────────

    /** Send a notification directly to a specific employee. */
    @Transactional
    public void sendToEmployee(Long employeeId, String message, Notification.Type type) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));
        Notification n = new Notification();
        n.setEmployee(emp);
        n.setMessage(message);
        n.setType(type);
        notificationRepository.save(n);
    }

    /** Send a notification to the manager of a given employee. */
    @Transactional
    public void sendToManager(Long employeeId, String message, Notification.Type type) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));
        Employee manager = emp.getManager();
        if (manager == null) return; // no manager assigned — skip silently
        Notification n = new Notification();
        n.setEmployee(manager);
        n.setMessage(message);
        n.setType(type);
        notificationRepository.save(n);
    }

    /** Broadcast an announcement as a notification to ALL managers. */
    @Transactional
    public void broadcastAnnouncementToAllManagers(String message) {
        List<Employee> allManagers = employeeRepository.findAll().stream()
                .filter(e -> e.getUser() != null
                        && e.getUser().getRole() != null
                        && "MANAGER".equals(e.getUser().getRole().name()))
                .collect(Collectors.toList());

        List<Notification> notifications = allManagers.stream()
                .map(manager -> {
                    Notification n = new Notification();
                    n.setEmployee(manager);
                    n.setMessage(message);
                    n.setType(Notification.Type.ANNOUNCEMENT);
                    return n;
                })
                .collect(Collectors.toList());
        notificationRepository.saveAll(notifications);
    }

}
