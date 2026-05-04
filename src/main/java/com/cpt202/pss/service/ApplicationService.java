package com.cpt202.pss.service;

import com.cpt202.pss.dto.ApplicationDto;
import com.cpt202.pss.entity.Application;
import com.cpt202.pss.entity.Project;
import com.cpt202.pss.entity.User;
import com.cpt202.pss.exception.BusinessException;
import com.cpt202.pss.exception.ResourceNotFoundException;
import com.cpt202.pss.repository.ApplicationRepository;
import com.cpt202.pss.repository.ProjectRepository;
import com.cpt202.pss.repository.UserRepository;
import com.cpt202.pss.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public List<ApplicationDto.Response> myApplications() {
        if (SecurityUtils.currentRole() != User.Role.Student) {
            throw new BusinessException(403, "Only students have an application history");
        }
        return applicationRepository.findByStudentId(SecurityUtils.currentUserId())
                .stream().map(this::toDto).toList();
    }

    public List<ApplicationDto.Response> forProject(Integer projectId) {
        Project p = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        // Only the owning teacher (or admin) may inspect a project's applications.
        if (SecurityUtils.currentRole() != User.Role.Admin
                && !p.getTeacherId().equals(SecurityUtils.currentUserId())) {
            throw new BusinessException(403, "Only the project owner may view its applications");
        }
        return applicationRepository.findByProjectId(projectId)
                .stream().map(this::toDto).toList();
    }

    public ApplicationDto.Response get(Integer id) {
        Application a = getEntity(id);
        // Visible to: the applicant, the project owner, or admin.
        if (SecurityUtils.currentRole() != User.Role.Admin) {
            Project p = projectRepository.findById(a.getProjectId()).orElseThrow();
            Integer me = SecurityUtils.currentUserId();
            if (!a.getStudentId().equals(me) && !p.getTeacherId().equals(me)) {
                throw new BusinessException(403, "You may not view this application");
            }
        }
        return toDto(a);
    }

    /**
     * Student applies to a project.
     * Business rules enforced:
     *  - only Student role may apply
     *  - target project must exist and not be CLOSED
     *  - student cannot already hold an AGREED active project (one-active-agreement rule)
     *  - a student cannot have a duplicate PENDING/ACCEPTED application on the same project
     */
    @Transactional
    public ApplicationDto.Response apply(ApplicationDto.CreateRequest req) {
        if (SecurityUtils.currentRole() != User.Role.Student) {
            throw new BusinessException(403, "Only students may apply for projects");
        }
        Integer studentId = SecurityUtils.currentUserId();

        Project project = projectRepository.findById(req.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (project.getStatus() == Project.Status.CLOSED) {
            throw new BusinessException("Closed/archived projects cannot receive new requests");
        }
        if (project.getCurrentStudents() != null
                && project.getCurrentStudents() >= project.getMaxStudents()) {
            throw new BusinessException("Project capacity is full");
        }

        // Rule: one active agreed project per student.
        applicationRepository.findFirstByStudentIdAndStatus(studentId, Application.Status.ACCEPTED)
                .ifPresent(a -> {
                    throw new BusinessException(
                            "You already have an active agreed project (id=" + a.getProjectId() + ")");
                });

        // Prevent duplicate PENDING request on the same project.
        applicationRepository.findFirstByProjectIdAndStudentIdAndStatus(
                req.getProjectId(), studentId, Application.Status.PENDING)
                .ifPresent(a -> {
                    throw new BusinessException("You already have a pending request for this project");
                });

        Application app = Application.builder()
                .projectId(req.getProjectId())
                .studentId(studentId)
                .status(Application.Status.PENDING)
                .notes(req.getNotes())
                .build();
        app = applicationRepository.save(app);

        // Promote project status visually to REQUESTED on first request.
        if (project.getStatus() == Project.Status.AVAILABLE) {
            project.setStatus(Project.Status.REQUESTED);
            projectRepository.save(project);
        }
        return toDto(app);
    }

    /** Student withdraws a PENDING request. */
    @Transactional
    public ApplicationDto.Response withdraw(Integer applicationId) {
        Application a = getEntity(applicationId);
        if (!a.getStudentId().equals(SecurityUtils.currentUserId())) {
            throw new BusinessException(403, "You may only withdraw your own request");
        }
        if (a.getStatus() != Application.Status.PENDING) {
            throw new BusinessException("Only PENDING requests can be withdrawn");
        }
        a.setStatus(Application.Status.WITHDRAWN);
        return toDto(applicationRepository.save(a));
    }

    /**
     * Teacher decides on an application: ACCEPT or REJECT.
     * On ACCEPT:
     *  - currentStudents++
     *  - if maxStudents reached: project &rarr; AGREED, all other PENDING apps rejected
     *  - if not full: project stays REQUESTED
     */
    @Transactional
    public ApplicationDto.Response decide(Integer applicationId,
                                          ApplicationDto.DecisionRequest req) {
        if (req.getStatus() != Application.Status.ACCEPTED
                && req.getStatus() != Application.Status.REJECTED) {
            throw new BusinessException("status must be ACCEPTED or REJECTED");
        }
        Application a = getEntity(applicationId);
        if (a.getStatus() != Application.Status.PENDING) {
            throw new BusinessException("Only PENDING requests can be decided");
        }

        Project p = projectRepository.findById(a.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (SecurityUtils.currentRole() != User.Role.Admin
                && !p.getTeacherId().equals(SecurityUtils.currentUserId())) {
            throw new BusinessException(403, "Only the project owner may decide");
        }

        a.setStatus(req.getStatus());
        a.setFeedback(req.getFeedback());

        if (req.getStatus() == Application.Status.ACCEPTED) {
            // Defensive double-check: student must still be eligible.
            applicationRepository
                .findFirstByStudentIdAndStatus(a.getStudentId(), Application.Status.ACCEPTED)
                .ifPresent(other -> {
                    throw new BusinessException("Student already has an active agreed project");
                });

            int now = (p.getCurrentStudents() == null ? 0 : p.getCurrentStudents()) + 1;
            p.setCurrentStudents(now);
            if (now >= p.getMaxStudents()) {
                p.setStatus(Project.Status.AGREED);
                // Auto-reject other PENDING requests for the same project.
                List<Application> others = applicationRepository
                        .findByProjectIdAndStatus(p.getProjectId(), Application.Status.PENDING);
                for (Application o : others) {
                    if (!o.getApplicationId().equals(a.getApplicationId())) {
                        o.setStatus(Application.Status.REJECTED);
                        o.setFeedback("Auto-rejected: project capacity reached");
                        applicationRepository.save(o);
                    }
                }
            } else if (p.getStatus() == Project.Status.AVAILABLE) {
                p.setStatus(Project.Status.REQUESTED);
            }
            projectRepository.save(p);
        }
        return toDto(applicationRepository.save(a));
    }

    Application getEntity(Integer id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    private ApplicationDto.Response toDto(Application a) {
        Project p = projectRepository.findById(a.getProjectId()).orElse(null);
        String projectTitle = p == null ? null : p.getTitle();
        Integer teacherId   = p == null ? null : p.getTeacherId();
        String teacherName  = teacherId == null ? null
                : userRepository.findById(teacherId).map(User::getFullName).orElse(null);
        String studentName  = userRepository.findById(a.getStudentId())
                .map(User::getFullName).orElse(null);
        return ApplicationDto.Response.builder()
                .applicationId(a.getApplicationId())
                .projectId(a.getProjectId())
                .projectTitle(projectTitle)
                .studentId(a.getStudentId())
                .studentName(studentName)
                .teacherId(teacherId)
                .teacherName(teacherName)
                .status(a.getStatus())
                .notes(a.getNotes())
                .feedback(a.getFeedback())
                .appliedAt(a.getAppliedAt())
                .build();
    }
}
