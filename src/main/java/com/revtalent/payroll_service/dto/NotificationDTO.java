package com.revtalent.payroll_service.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private Long id;
    private String icon;
    private String text;
    private String time;
    private boolean unread;
}
