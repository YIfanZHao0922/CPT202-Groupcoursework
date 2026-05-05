package com.cpt202.pss.service;

import com.cpt202.pss.dto.ProjectDto;
import com.cpt202.pss.entity.Category;
import com.cpt202.pss.entity.Project;
import com.cpt202.pss.entity.User;
import com.cpt202.pss.exception.BusinessException;
import com.cpt202.pss.exception.ResourceNotFoundException;
import com.cpt202.pss.repository.CategoryRepository;
import com.cpt202.pss.repository.ProjectRepository;
import com.cpt202.pss.repository.UserRepository;
import com.cpt202.pss.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public List<ProjectDto.Response> search(String keyword,
                                            Project.Status status,
                                            Integer teacherId,
                                            Integer categoryId) {
        return projectRepository.search(keyword, status, teacherId, categoryId)
                .stream().map(this::toDto).toList();
    }

    public ProjectDto.Response get(Integer id) {
        return toDto(getEntity(id));
    }

    public List<ProjectDto.Response> myProjects() {
        if (SecurityUtils.currentRole() != User.Role.Teacher
                && SecurityUtils.currentRole() != User.Role.Admin) {
            throw new BusinessException(403, "Only teachers may list 'my projects'");
        }
        return projectRepository.findByTeacherId(SecurityUtils.currentUserId())
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ProjectDto.Response create(ProjectDto.CreateRequest req) {
        if (SecurityUtils.currentRole() != User.Role.Teacher
                && SecurityUtils.currentRole() != User.Role.Admin) {
            throw new BusinessException(403, "Only teachers may create projects");
        }
        Project p = Project.builder()
                .teacherId(SecurityUtils.currentUserId())
                .title(req.getTitle())
                .description(req.getDescription())
                .requiredSkills(req.getRequiredSkills())
                .maxStudents(req.getMaxStudents() == null ? 1 : req.getMaxStudents())
                .currentStudents(0)
                .status(Project.Status.AVAILABLE)
                .categories(resolveCategories(req.getCategoryIds()))
                .build();
        return toDto(projectRepository.save(p));
    }

    @Transactional
    public ProjectDto.Response update(Integer id, ProjectDto.UpdateRequest req) {
        Project p = getEntity(id);
        ensureOwnedOrAdmin(p);
        if (req.getTitle()          != null) p.setTitle(req.getTitle());
        if (req.getDescription()    != null) p.setDescription(req.getDescription());
        if (req.getRequiredSkills() != null) p.setRequiredSkills(req.getRequiredSkills());
        if (req.getMaxStudents()    != null) {
            if (req.getMaxStudents() < p.getCurrentStudents()) {
                throw new BusinessException("maxStudents cannot be less than currentStudents");
            }
            p.setMaxStudents(req.getMaxStudents());
        }
        if (req.getStatus() != null) p.setStatus(req.getStatus());
        if (req.getCategoryIds() != null) p.setCategories(resolveCategories(req.getCategoryIds()));
        return toDto(projectRepository.save(p));
    }

    @Transactional
    public ProjectDto.Response changeStatus(Integer id, Project.Status status) {
        Project p = getEntity(id);
        ensureOwnedOrAdmin(p);
        p.setStatus(status);
        return toDto(projectRepository.save(p));
    }

    @Transactional
    public void delete(Integer id) {
        Project p = getEntity(id);
        ensureOwnedOrAdmin(p);
        if (p.getCurrentStudents() != null && p.getCurrentStudents() > 0) {
            throw new BusinessException("Cannot delete a project with active students; close/archive first");
        }
        projectRepository.delete(p);
    }

    Project getEntity(Integer id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }

    private void ensureOwnedOrAdmin(Project p) {
        if (SecurityUtils.currentRole() == User.Role.Admin) return;
        if (!p.getTeacherId().equals(SecurityUtils.currentUserId())) {
            throw new BusinessException(403, "Teachers may manage only their own projects");
        }
    }

    private Set<Category> resolveCategories(Set<Integer> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        return new HashSet<>(categoryRepository.findAllById(ids));
    }

    ProjectDto.Response toDto(Project p) {
        String teacherName = userRepository.findById(p.getTeacherId())
                .map(User::getFullName).orElse(null);
        List<ProjectDto.CategoryBrief> cats = p.getCategories().stream()
                .map(c -> ProjectDto.CategoryBrief.builder()
                        .categoryId(c.getCategoryId()).name(c.getName()).build())
                .toList();
        return ProjectDto.Response.builder()
                .projectId(p.getProjectId())
                .teacherId(p.getTeacherId())
                .teacherName(teacherName)
                .title(p.getTitle())
                .description(p.getDescription())
                .requiredSkills(p.getRequiredSkills())
                .maxStudents(p.getMaxStudents())
                .currentStudents(p.getCurrentStudents())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .categories(cats)
                .build();
    }
}
