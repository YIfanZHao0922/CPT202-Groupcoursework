package com.cpt202.pss.service;

import com.cpt202.pss.dto.ProjectDto;
import com.cpt202.pss.entity.Project;
import com.cpt202.pss.entity.User;
import com.cpt202.pss.exception.BusinessException;
import com.cpt202.pss.repository.CategoryRepository;
import com.cpt202.pss.repository.ProjectRepository;
import com.cpt202.pss.repository.UserRepository;
import com.cpt202.pss.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ProjectService projectService;

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private void loginAs(int id, User.Role role) {
        User u = User.builder().userId(id).username("u" + id).password("p")
                .email("u@x").fullName("u").role(role).status(User.Status.ACTIVE).build();
        UserPrincipal p = new UserPrincipal(u);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        p, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    @Test
    void create_succeeds_forTeacher() {
        loginAs(50, User.Role.Teacher);
        ProjectDto.CreateRequest req = new ProjectDto.CreateRequest();
        req.setTitle("New AI Project");
        req.setMaxStudents(2);
        when(projectRepository.save(any(Project.class)))
                .thenAnswer(inv -> { Project p = inv.getArgument(0); p.setProjectId(1); return p; });
        when(userRepository.findById(50))
                .thenReturn(Optional.of(User.builder().userId(50).fullName("Teach").build()));

        ProjectDto.Response r = projectService.create(req);
        assertThat(r.getProjectId()).isEqualTo(1);
        assertThat(r.getStatus()).isEqualTo(Project.Status.AVAILABLE);
        assertThat(r.getTeacherId()).isEqualTo(50);
    }

    @Test
    void create_throws_forStudent() {
        loginAs(100, User.Role.Student);
        ProjectDto.CreateRequest req = new ProjectDto.CreateRequest();
        req.setTitle("X");

        assertThatThrownBy(() -> projectService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only teachers");
    }

    @Test
    void update_throws_whenTeacherTouchesAnotherTeachersProject() {
        loginAs(99, User.Role.Teacher);
        Project p = Project.builder().projectId(1).teacherId(50).maxStudents(2)
                .currentStudents(0).status(Project.Status.AVAILABLE).build();
        when(projectRepository.findById(1)).thenReturn(Optional.of(p));

        ProjectDto.UpdateRequest req = new ProjectDto.UpdateRequest();
        req.setTitle("hijack");

        assertThatThrownBy(() -> projectService.update(1, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("only their own");
    }

    @Test
    void update_succeeds_forAdminOnAnyProject() {
        loginAs(1, User.Role.Admin);
        Project p = Project.builder().projectId(1).teacherId(50).maxStudents(2)
                .currentStudents(0).status(Project.Status.AVAILABLE).build();
        when(projectRepository.findById(1)).thenReturn(Optional.of(p));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(50))
                .thenReturn(Optional.of(User.builder().userId(50).fullName("T").build()));

        ProjectDto.UpdateRequest req = new ProjectDto.UpdateRequest();
        req.setTitle("admin-edit");

        ProjectDto.Response r = projectService.update(1, req);
        assertThat(r.getTitle()).isEqualTo("admin-edit");
    }
}
