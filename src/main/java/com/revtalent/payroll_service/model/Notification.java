package com.revtalent.payroll_service.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(name = "is_read")
    private boolean read = false;

    private LocalDateTime createdAt;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name = "employee_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "notifications",
            "user", "department", "manager"})
    private Employee employee;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Type {
        LEAVE,
        PAYROLL,
        SYSTEM,
        ANNOUNCEMENT
    }
}
