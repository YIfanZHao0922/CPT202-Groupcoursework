package com.cpt202.pss.service;

import com.cpt202.pss.dto.CategoryDto;
import com.cpt202.pss.entity.Category;
import com.cpt202.pss.exception.BusinessException;
import com.cpt202.pss.exception.ResourceNotFoundException;
import com.cpt202.pss.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> list() { return categoryRepository.findAll(); }

    public Category get(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    @Transactional
    public Category create(CategoryDto.CreateRequest req) {
        if (categoryRepository.existsByName(req.getName())) {
            throw new BusinessException("Category name already exists: " + req.getName());
        }
        Category c = Category.builder()
                .name(req.getName())
                .description(req.getDescription())
                .build();
        return categoryRepository.save(c);
    }

    @Transactional
    public Category update(Integer id, CategoryDto.UpdateRequest req) {
        Category c = get(id);
        if (req.getName() != null && !req.getName().equals(c.getName())) {
            if (categoryRepository.existsByName(req.getName())) {
                throw new BusinessException("Category name already exists");
            }
            c.setName(req.getName());
        }
        if (req.getDescription() != null) c.setDescription(req.getDescription());
        return categoryRepository.save(c);
    }

    @Transactional
    public void delete(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found: " + id);
        }
        categoryRepository.deleteById(id);
    }
}
