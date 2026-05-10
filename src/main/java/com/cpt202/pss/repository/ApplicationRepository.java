package com.cpt202.pss.repository;

import com.cpt202.pss.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    List<Application> findByStudentId(Integer studentId);

    List<Application> findByProjectId(Integer projectId);

    List<Application> findByStudentIdAndStatus(Integer studentId, Application.Status status);

    /** Used to enforce: a student can only have one active AGREED project at a time. */
    Optional<Application> findFirstByStudentIdAndStatus(Integer studentId, Application.Status status);

    Optional<Application> findFirstByProjectIdAndStudentIdAndStatus(
            Integer projectId, Integer studentId, Application.Status status);

    /** Used to mass-reject other PENDING apps once one is ACCEPTED for the same project (when full). */
    List<Application> findByProjectIdAndStatus(Integer projectId, Application.Status status);

    /**
     * Admin-only: list all applications across the system with optional filters.
     * Each filter is nullable; null means "no constraint".
     */
    @Query("SELECT a FROM Application a WHERE " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:projectId IS NULL OR a.projectId = :projectId) AND " +
           "(:studentId IS NULL OR a.studentId = :studentId) " +
           "ORDER BY a.appliedAt DESC")
    List<Application> findAllFiltered(@Param("status") Application.Status status,
                                      @Param("projectId") Integer projectId,
                                      @Param("studentId") Integer studentId);
}
