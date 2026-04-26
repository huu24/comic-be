package com.example.comic.service;

import com.example.comic.exception.AlreadyExistsException;
import com.example.comic.exception.NotFoundException;
import com.example.comic.model.Category;
import com.example.comic.model.dto.CategoryCreateRequest;
import com.example.comic.model.dto.CategoryResponse;
import com.example.comic.repository.CategoryRepository;
import com.example.comic.repository.ComicCategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ComicCategoryRepository comicCategoryRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(c -> CategoryResponse.builder().id(c.getId()).name(c.getName()).build())
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        currentUserService.requireAdmin();

        String name = request.getName().trim();
        if (categoryRepository.existsByName(name)) {
            throw new AlreadyExistsException("Danh mục '" + name + "' đã tồn tại.");
        }

        Category category = Category.builder().name(name).build();
        Category saved = categoryRepository.save(category);
        return CategoryResponse.builder().id(saved.getId()).name(saved.getName()).build();
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryCreateRequest request) {
        currentUserService.requireAdmin();

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với id: " + id));

        String name = request.getName().trim();
        if (categoryRepository.existsByName(name) && !category.getName().equals(name)) {
            throw new AlreadyExistsException("Danh mục '" + name + "' đã tồn tại.");
        }

        category.setName(name);
        Category saved = categoryRepository.save(category);
        return CategoryResponse.builder().id(saved.getId()).name(saved.getName()).build();
    }

    @Transactional
    public void delete(Long id) {
        currentUserService.requireAdmin();

        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("Không tìm thấy danh mục với id: " + id);
        }

        comicCategoryRepository.deleteByCategoryId(id);
        categoryRepository.deleteById(id);
    }
}
