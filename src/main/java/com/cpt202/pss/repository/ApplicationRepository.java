package com.cpt202.pss.repository;

import com.cpt202.pss.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
