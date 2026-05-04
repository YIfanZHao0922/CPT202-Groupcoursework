package com.cpt202.pss.service;

import com.cpt202.pss.dto.ApplicationDto;
import com.cpt202.pss.entity.Application;
import com.cpt202.pss.entity.Project;
import com.cpt202.pss.entity.User;
import com.cpt202.pss.exception.BusinessException;
import com.cpt202.pss.repository.ApplicationRepository;
import com.cpt202.pss.repository.ProjectRepository;
import com.cpt202.pss.repository.UserRepository;
import com.cpt202.pss.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ApplicationService applicationService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Integer userId, String username, User.Role role) {
        User u = User.builder().userId(userId).username(username).password("p")
                .email(username + "@x").fullName(username).role(role)
                .status(User.Status.ACTIVE).build();
        UserPrincipal p = new UserPrincipal(u);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        p, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private Project availableProject(int teacherId, int max) {
        return Project.builder()
                .projectId(10).teacherId(teacherId).title("T")
                .maxStudents(max).currentStudents(0)
                .status(Project.Status.AVAILABLE).build();
    }

    @Test
    void apply_succeeds_forStudentOnAvailableProject() {
        loginAs(100, "alice", User.Role.Student);
        Project p = availableProject(50, 2);
        when(projectRepository.findById(10)).thenReturn(Optional.of(p));
        when(applicationRepository.findFirstByStudentIdAndStatus(100, Application.Status.ACCEPTED))
                .thenReturn(Optional.empty());
        when(applicationRepository.findFirstByProjectIdAndStudentIdAndStatus(
                10, 100, Application.Status.PENDING)).thenReturn(Optional.empty());
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(inv -> { Application a = inv.getArgument(0); a.setApplicationId(1); return a; });
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApplicationDto.CreateRequest req = new ApplicationDto.CreateRequest();
        req.setProjectId(10);
        req.setNotes("Interested!");

        ApplicationDto.Response resp = applicationService.apply(req);

        assertThat(resp.getApplicationId()).isEqualTo(1);
        assertThat(resp.getStatus()).isEqualTo(Application.Status.PENDING);
        // Project promoted to REQUESTED
        verify(projectRepository).save(argThat(pp -> pp.getStatus() == Project.Status.REQUESTED));
    }

    @Test
    void apply_throws_whenStudentAlreadyHasAcceptedProject() {
        loginAs(100, "alice", User.Role.Student);
        when(projectRepository.findById(10)).thenReturn(Optional.of(availableProject(50, 2)));
        Application existing = Application.builder().applicationId(99).projectId(7).studentId(100)
                .status(Application.Status.ACCEPTED).build();
        when(applicationRepository.findFirstByStudentIdAndStatus(100, Application.Status.ACCEPTED))
                .thenReturn(Optional.of(existing));

        ApplicationDto.CreateRequest req = new ApplicationDto.CreateRequest();
        req.setProjectId(10);

        assertThatThrownBy(() -> applicationService.apply(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("active agreed project");
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void apply_throws_whenProjectClosed() {
        loginAs(100, "alice", User.Role.Student);
        Project p = availableProject(50, 1);
        p.setStatus(Project.Status.CLOSED);
        when(projectRepository.findById(10)).thenReturn(Optional.of(p));

        ApplicationDto.CreateRequest req = new ApplicationDto.CreateRequest();
        req.setProjectId(10);

        assertThatThrownBy(() -> applicationService.apply(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Closed");
    }

    @Test
    void apply_throws_whenTeacherTriesToApply() {
        loginAs(50, "teach", User.Role.Teacher);

        ApplicationDto.CreateRequest req = new ApplicationDto.CreateRequest();
        req.setProjectId(10);

        assertThatThrownBy(() -> applicationService.apply(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only students");
    }

    @Test
    void decide_accept_marksProjectAgreedWhenCapacityReached() {
        loginAs(50, "teach", User.Role.Teacher);
        Project p = availableProject(50, 1);
        p.setStatus(Project.Status.REQUESTED);
        Application a = Application.builder()
                .applicationId(1).projectId(10).studentId(100)
                .status(Application.Status.PENDING).build();

        when(applicationRepository.findById(1)).thenReturn(Optional.of(a));
        when(projectRepository.findById(10)).thenReturn(Optional.of(p));
        when(applicationRepository.findFirstByStudentIdAndStatus(100, Application.Status.ACCEPTED))
                .thenReturn(Optional.empty());
        when(applicationRepository.findByProjectIdAndStatus(10, Application.Status.PENDING))
                .thenReturn(List.of(a));
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApplicationDto.DecisionRequest req = new ApplicationDto.DecisionRequest();
        req.setStatus(Application.Status.ACCEPTED);
        req.setFeedback("Welcome");

        ApplicationDto.Response resp = applicationService.decide(1, req);

        assertThat(resp.getStatus()).isEqualTo(Application.Status.ACCEPTED);
        verify(projectRepository).save(argThat(pp ->
                pp.getStatus() == Project.Status.AGREED && pp.getCurrentStudents() == 1));
    }

    @Test
    void decide_reject_doesNotIncrementStudents() {
        loginAs(50, "teach", User.Role.Teacher);
        Project p = availableProject(50, 2);
        p.setStatus(Project.Status.REQUESTED);
        Application a = Application.builder()
                .applicationId(1).projectId(10).studentId(100)
                .status(Application.Status.PENDING).build();

        when(applicationRepository.findById(1)).thenReturn(Optional.of(a));
        when(projectRepository.findById(10)).thenReturn(Optional.of(p));
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApplicationDto.DecisionRequest req = new ApplicationDto.DecisionRequest();
        req.setStatus(Application.Status.REJECTED);
        req.setFeedback("Skills mismatch");

        ApplicationDto.Response resp = applicationService.decide(1, req);

        assertThat(resp.getStatus()).isEqualTo(Application.Status.REJECTED);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void decide_throws_whenNotProjectOwner() {
        loginAs(99, "otherTeach", User.Role.Teacher);
        Project p = availableProject(50, 2);
        Application a = Application.builder()
                .applicationId(1).projectId(10).studentId(100)
                .status(Application.Status.PENDING).build();
        when(applicationRepository.findById(1)).thenReturn(Optional.of(a));
        when(projectRepository.findById(10)).thenReturn(Optional.of(p));

        ApplicationDto.DecisionRequest req = new ApplicationDto.DecisionRequest();
        req.setStatus(Application.Status.ACCEPTED);

        assertThatThrownBy(() -> applicationService.decide(1, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only the project owner");
    }

    @Test
    void withdraw_succeeds_forOwnPendingApp() {
        loginAs(100, "alice", User.Role.Student);
        Application a = Application.builder()
                .applicationId(1).projectId(10).studentId(100)
                .status(Application.Status.PENDING).build();
        when(applicationRepository.findById(1)).thenReturn(Optional.of(a));
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApplicationDto.Response resp = applicationService.withdraw(1);

        assertThat(resp.getStatus()).isEqualTo(Application.Status.WITHDRAWN);
    }

    @Test
    void withdraw_throws_whenNotOwner() {
        loginAs(101, "bob", User.Role.Student);
        Application a = Application.builder()
                .applicationId(1).projectId(10).studentId(100)
                .status(Application.Status.PENDING).build();
        when(applicationRepository.findById(1)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> applicationService.withdraw(1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("only withdraw your own");
    }
}
