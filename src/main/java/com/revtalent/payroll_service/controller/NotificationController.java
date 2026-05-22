package com.revtalent.payroll_service.controller;

import com.revtalent.payroll_service.dto.NotificationResponseDTO;
import com.revtalent.payroll_service.dto.notification.NotificationResponse;
import com.revtalent.payroll_service.dto.notification.NotificationRequest;
import com.revtalent.payroll_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor

public class NotificationController {

    private final NotificationService notificationService;

    // ── Employee endpoints (empId scoped) ────────────────────────────────────

    @GetMapping("/{empId}")
    public ResponseEntity<List<NotificationResponseDTO>> getByEmployee(@PathVariable Long empId) {
        return ResponseEntity.ok(notificationService.getNotifications(empId));
    }

    @GetMapping("/{empId}/unread")
    public ResponseEntity<List<NotificationResponseDTO>> getUnread(@PathVariable Long empId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(empId));
    }

    @GetMapping("/{empId}/unread-count")
    public ResponseEntity<Integer> getUnreadCount(@PathVariable Long empId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(empId));
    }

    @PutMapping("/{notifId}/read")
    public ResponseEntity<NotificationResponseDTO> markRead(@PathVariable Long notifId) {
        return ResponseEntity.ok(notificationService.markAsRead(notifId));
    }

    @PutMapping("/{empId}/read-all")
    public ResponseEntity<Void> markAllForEmployee(@PathVariable Long empId) {
        notificationService.markAllAsRead(empId);
        return ResponseEntity.noContent().build();
    }

    // ── Manager / Global endpoints ────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        if (isManager) {
            return ResponseEntity.ok(notificationService.getAllNotificationsForManager(auth.getName()));
        }
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getAllUnread() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        if (isManager) {
            return ResponseEntity.ok(notificationService.getAllUnreadNotificationsForManager(auth.getName()));
        }
        return ResponseEntity.ok(notificationService.getAllUnreadNotifications());
    }

    @PutMapping("/read-all")
    public ResponseEntity<String> markAllReadGlobal() {
        notificationService.markAllAsReadGlobal();
        return ResponseEntity.ok("All marked as read");
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody NotificationRequest dto) {
        return ResponseEntity.ok(notificationService.create(dto));
    }
}
