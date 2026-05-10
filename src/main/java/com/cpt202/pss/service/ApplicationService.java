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

    public List<ApplicationDto.Response> listAll(Application.Status status,
                                                 Integer projectId,
                                                 Integer studentId) {
        if (SecurityUtils.currentRole() != User.Role.Admin) {
            throw new BusinessException(403, "Only admin may list all applications");
        }
        return applicationRepository.findAllFiltered(status, projectId, studentId)
                .stream().map(this::toDto).toList();
    }

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
        if (SecurityUtils.currentRole() != User.Role.Admin
                && !p.getTeacherId().equals(SecurityUtils.currentUserId())) {
            throw new BusinessException(403, "Only the project owner may view its applications");
        }
        return applicationRepository.findByProjectId(projectId)
                .stream().map(this::toDto).toList();
    }

    public ApplicationDto.Response get(Integer id) {
        Application a = getEntity(id);
        if (SecurityUtils.currentRole() != User.Role.Admin) {
            Project p = projectRepository.findById(a.getProjectId()).orElseThrow();
            Integer me = SecurityUtils.currentUserId();
            if (!a.getStudentId().equals(me) && !p.getTeacherId().equals(me)) {
                throw new BusinessException(403, "You may not view this application");
            }
        }
        return toDto(a);
    }

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

        applicationRepository.findFirstByStudentIdAndStatus(studentId, Application.Status.ACCEPTED)
                .ifPresent(a -> {
                    throw new BusinessException(
                            "You already have an active agreed project (id=" + a.getProjectId() + ")");
                });

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

        if (project.getStatus() == Project.Status.AVAILABLE) {
            project.setStatus(Project.Status.REQUESTED);
            projectRepository.save(project);
        }
        return toDto(app);
    }

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

        // 防御性校验必须在修改 a.status 之前，否则 JPA 自动 flush 会把
        // 当前这条记录刷成 ACCEPTED，查询时就会把自己当成"已存在的同意项目"
        if (req.getStatus() == Application.Status.ACCEPTED) {
            applicationRepository
                .findFirstByStudentIdAndStatus(a.getStudentId(), Application.Status.ACCEPTED)
                .ifPresent(other -> {
                    if (!other.getApplicationId().equals(a.getApplicationId())) {
                        throw new BusinessException("Student already has an active agreed project");
                    }
                });
        }

        a.setStatus(req.getStatus());
        a.setFeedback(req.getFeedback());

        if (req.getStatus() == Application.Status.ACCEPTED) {
            int now = (p.getCurrentStudents() == null ? 0 : p.getCurrentStudents()) + 1;
            p.setCurrentStudents(now);
            if (now >= p.getMaxStudents()) {
                p.setStatus(Project.Status.AGREED);
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