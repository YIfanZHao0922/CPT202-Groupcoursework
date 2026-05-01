package com.cpt202.pss.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    public enum Status { AVAILABLE, REQUESTED, AGREED, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "projectId")
    private Integer projectId;

    @Column(name = "teacherId", nullable = false)
    private Integer teacherId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String description;

    @Column(name = "requiredSkills", length = 200)
    private String requiredSkills;

    @Column(name = "maxStudents")
    @Builder.Default
    private Integer maxStudents = 1;

    @Column(name = "currentStudents")
    @Builder.Default
    private Integer currentStudents = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    @Builder.Default
    private Status status = Status.AVAILABLE;

    @Column(name = "createdAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /* Many-to-Many with Category through projectcategory join table. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "projectcategory",
            joinColumns = @JoinColumn(name = "projectId"),
            inverseJoinColumns = @JoinColumn(name = "categoryId")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();
}
