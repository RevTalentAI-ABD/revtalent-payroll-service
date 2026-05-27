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
        try {
            n.setType(Notification.Type.valueOf(dto.getType()));
        } catch (IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid notification type");
        }
        return notificationRepository.save(n);
    }

    // ── Employee endpoints (empId scoped) ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getNotifications(Long empId) {
        verifyEmployeeAccess(empId);
        return notificationRepository.findByEmployee_IdOrderByCreatedAtDesc(empId)
                .stream()
                .map(NotificationResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getUnreadNotifications(Long empId) {
        verifyEmployeeAccess(empId);
        return notificationRepository.findByEmployee_IdAndReadFalse(empId)
                .stream()
                .map(NotificationResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(Long empId) {
        verifyEmployeeAccess(empId);
        return notificationRepository.countByEmployee_IdAndReadFalse(empId);
    }

    @Transactional
    public NotificationResponseDTO markAsRead(Long notifId) {
        Notification notification = notificationRepository.findById(notifId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notifId));
        verifyEmployeeAccess(notification.getEmployee().getId());
        notification.setRead(true);
        return NotificationResponseDTO.from(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(Long empId) {
        verifyEmployeeAccess(empId);
        notificationRepository.markAllAsReadByEmployeeId(empId);
    }

    @Transactional
    public void deleteNotification(Long notifId) {
        Notification notification = notificationRepository.findById(notifId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notifId));
        verifyEmployeeAccess(notification.getEmployee().getId());
        notificationRepository.deleteById(notifId);
    }

    private void verifyEmployeeAccess(Long empId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return;
        boolean isAdminOrHR = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));
        if (isAdminOrHR) return;
        Employee emp = employeeRepository.findByUser_Username(auth.getName()).orElse(null);
        if (emp == null || !emp.getId().equals(empId)) {
            throw new RuntimeException("Access Denied");
        }
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
        return employeeRepository.findByUser_Username(username)
                .map(manager -> notificationRepository.findByEmployee_Manager_IdAndReadFalse(manager.getId()).stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }
    
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotificationsForUser(String username) {
        return employeeRepository.findByUser_Username(username)
                .map(emp -> notificationRepository.findByEmployee_IdOrderByCreatedAtDesc(emp.getId()).stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }
    
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllUnreadNotificationsForUser(String username) {
        return employeeRepository.findByUser_Username(username)
                .map(emp -> notificationRepository.findByEmployee_IdAndReadFalse(emp.getId()).stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList()))
                .orElse(List.of());
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
