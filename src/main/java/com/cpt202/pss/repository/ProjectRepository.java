package com.cpt202.pss.repository;

import com.cpt202.pss.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    List<Project> findByTeacherId(Integer teacherId);

    List<Project> findByStatus(Project.Status status);

    /**
     * Multi-criteria search used by browse/search/filter feature.
     * All params are nullable. categoryId joins through project_category.
     */
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.categories c WHERE " +
           "(:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(p.requiredSkills) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:teacherId IS NULL OR p.teacherId = :teacherId) " +
           "AND (:categoryId IS NULL OR c.categoryId = :categoryId)")
    List<Project> search(@Param("keyword") String keyword,
                         @Param("status") Project.Status status,
                         @Param("teacherId") Integer teacherId,
                         @Param("categoryId") Integer categoryId);
}
