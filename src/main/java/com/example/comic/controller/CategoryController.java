package com.example.comic.controller;

import com.example.comic.model.dto.CategoryCreateRequest;
import com.example.comic.model.dto.CategoryResponse;
import com.example.comic.model.dto.DataResponse;
import com.example.comic.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<DataResponse<List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(
                DataResponse.<List<CategoryResponse>>builder().data(categoryService.getAll()).build());
    }

    @PostMapping
    public ResponseEntity<DataResponse<CategoryResponse>> create(@Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                DataResponse.<CategoryResponse>builder().data(categoryService.create(request)).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataResponse<CategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.ok(
                DataResponse.<CategoryResponse>builder().data(categoryService.update(id, request)).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
