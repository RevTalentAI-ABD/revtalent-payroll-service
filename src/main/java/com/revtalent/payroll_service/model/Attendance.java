package com.revtalent.payroll_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_attendance_day", columnNames = {"employee_id", "work_date"})
        },
        indexes = {
                @Index(name = "idx_attendance_date", columnList = "work_date"),
                @Index(name = "idx_attendance_type", columnList = "attendance_type")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_attendance_emp"))
    private Employee employee;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "check_in")
    private LocalDateTime checkIn;

    @Column(name = "check_out")
    private LocalDateTime checkOut;

    @Column(name = "duration_min", insertable = false, updatable = false)
    private Integer durationMin;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_type", nullable = false,
            columnDefinition = "ENUM('WFO','WFH','FIELD') DEFAULT 'WFO'")
    private AttendanceType attendanceType = AttendanceType.WFO;

    @Column(name = "is_regularized", nullable = false)
    private boolean isRegularized = false;

    @Column(length = 255)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PRESENT;

    public enum Status { PRESENT, ABSENT, WFH, ON_LEAVE }
    public enum AttendanceType { WFO, WFH, FIELD }
}
