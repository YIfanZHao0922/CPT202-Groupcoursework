package com.cpt202.pss.config;

import com.cpt202.pss.entity.Category;
import com.cpt202.pss.entity.Project;
import com.cpt202.pss.entity.User;
import com.cpt202.pss.repository.CategoryRepository;
import com.cpt202.pss.repository.ProjectRepository;
import com.cpt202.pss.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Seeds demo data on first startup, ONLY if the users table is empty.
 * Disable in tests by NOT activating this bean (see test config).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Users already present - skipping seeding.");
            return;
        }
        log.info("Seeding demo data...");

        User admin = userRepository.save(User.builder()
                .username("admin").password(passwordEncoder.encode("admin123"))
                .email("admin@xjtlu.edu.cn").fullName("System Admin")
                .role(User.Role.Admin).status(User.Status.ACTIVE).build());

        User teacher1 = userRepository.save(User.builder()
                .username("teacher1").password(passwordEncoder.encode("teacher123"))
                .email("nanlin.jin@xjtlu.edu.cn").fullName("Dr. Nanlin Jin")
                .role(User.Role.Teacher).status(User.Status.ACTIVE).build());
        User teacher2 = userRepository.save(User.builder()
                .username("teacher2").password(passwordEncoder.encode("teacher123"))
                .email("teacher2@xjtlu.edu.cn").fullName("Prof. Alan Smith")
                .role(User.Role.Teacher).status(User.Status.ACTIVE).build());

        userRepository.save(User.builder()
                .username("student1").password(passwordEncoder.encode("student123"))
                .email("student1@xjtlu.edu.cn").fullName("Alice Wong")
                .role(User.Role.Student).status(User.Status.ACTIVE).build());
        userRepository.save(User.builder()
                .username("student2").password(passwordEncoder.encode("student123"))
                .email("student2@xjtlu.edu.cn").fullName("Bob Chen")
                .role(User.Role.Student).status(User.Status.ACTIVE).build());
        userRepository.save(User.builder()
                .username("student3").password(passwordEncoder.encode("student123"))
                .email("student3@xjtlu.edu.cn").fullName("Carol Li")
                .role(User.Role.Student).status(User.Status.ACTIVE).build());

        Category c1 = categoryRepository.save(Category.builder()
                .name("Machine Learning").description("AI and ML projects").build());
        Category c2 = categoryRepository.save(Category.builder()
                .name("Web Development").description("Web-based systems").build());
        Category c3 = categoryRepository.save(Category.builder()
                .name("Data Science").description("Data analytics and visualization").build());

        Set<Category> mlCats = new HashSet<>(Set.of(c1, c3));
        Set<Category> webCats = new HashSet<>(Set.of(c2));

        projectRepository.save(Project.builder()
                .teacherId(teacher1.getUserId())
                .title("Deep Learning for Medical Imaging")
                .description("Apply CNNs to classify medical scan images")
                .requiredSkills("Python, PyTorch, basic statistics")
                .maxStudents(2).currentStudents(0)
                .status(Project.Status.AVAILABLE)
                .categories(mlCats).build());

        projectRepository.save(Project.builder()
                .teacherId(teacher1.getUserId())
                .title("Smart Campus Web Portal")
                .description("Spring Boot + Vue full-stack project for campus services")
                .requiredSkills("Java, Spring Boot, Vue.js, MySQL")
                .maxStudents(3).currentStudents(0)
                .status(Project.Status.AVAILABLE)
                .categories(webCats).build());

        projectRepository.save(Project.builder()
                .teacherId(teacher2.getUserId())
                .title("Recommender System Benchmark")
                .description("Compare collaborative-filtering algorithms on MovieLens")
                .requiredSkills("Python, NumPy, pandas")
                .maxStudents(1).currentStudents(0)
                .status(Project.Status.AVAILABLE)
                .categories(new HashSet<>(Set.of(c1))).build());

        projectRepository.save(Project.builder()
                .teacherId(teacher2.getUserId())
                .title("Online Booking System for Library Rooms")
                .description("Web-based system to reserve study rooms")
                .requiredSkills("HTML/CSS/JS, Java")
                .maxStudents(2).currentStudents(0)
                .status(Project.Status.AVAILABLE)
                .categories(webCats).build());

        log.info("Seed complete:");
        log.info("  admin / admin123      (Admin)");
        log.info("  teacher1 / teacher123 (Teacher)");
        log.info("  teacher2 / teacher123 (Teacher)");
        log.info("  student1 / student123 (Student)");
        log.info("  student2 / student123 (Student)");
        log.info("  student3 / student123 (Student)");
    }
}
