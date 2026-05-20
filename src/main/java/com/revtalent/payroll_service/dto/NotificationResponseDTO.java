package com.revtalent.payroll_service.dto;

import com.revtalent.payroll_service.model.Notification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponseDTO {

    private Long id;
    private Long employeeId;
    private String message;
    private Notification.Type type;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponseDTO from(Notification n) {
        return NotificationResponseDTO.builder()
                .id(n.getId())
                .employeeId(n.getEmployee() != null ? n.getEmployee().getId() : null)
                .message(n.getMessage())
                .type(n.getType())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
