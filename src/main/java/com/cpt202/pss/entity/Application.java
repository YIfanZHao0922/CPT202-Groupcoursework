package com.cpt202.pss.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    public enum Status { PENDING, ACCEPTED, REJECTED, WITHDRAWN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "applicationId")
    private Integer applicationId;

    @Column(name = "projectId", nullable = false)
    private Integer projectId;

    @Column(name = "studentId", nullable = false)
    private Integer studentId;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(length = 200)
    private String notes;

    @Column(name = "appliedAt", insertable = false, updatable = false)
    private LocalDateTime appliedAt;

    @Column(length = 200)
    private String feedback;
}
