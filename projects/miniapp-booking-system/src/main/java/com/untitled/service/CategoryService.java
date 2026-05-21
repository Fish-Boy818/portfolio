package com.untitled.service;

import com.untitled.dto.CategoryCreateRequest;
import com.untitled.dto.CategoryResponse;
import com.untitled.dto.CategoryUpdateRequest;
import com.untitled.mapper.CategoryMapper;
import com.untitled.model.Category;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryResponse> listAll() {
        return categoryMapper.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<CategoryResponse> listActive() {
        return categoryMapper.findActive().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<CategoryResponse> get(long id) {
        Category category = categoryMapper.findById(id);
        if (category == null) {
            return Optional.empty();
        }
        return Optional.of(toResponse(category));
    }

    /**
     * 作用：
     * 新增后台维护的活动分类。
     * 方法：
     * 先校验分类标识是否可用，再构造分类对象写入名称、状态和排序，
     * 最后保存分类并返回新增结果。
     */
    public CategoryResponse create(CategoryCreateRequest request) { // 作用：新增活动分类；方法：校验 key 后创建分类并保存
        ensureKeyAvailable(null, request.getKey()); // 作用：校验分类 key；方法：检查当前 key 是否已经被其他分类占用
        Category category = new Category(); // 作用：创建分类对象；方法：实例化 Category 作为新增数据载体
        category.setKey(request.getKey());
        category.setName(request.getName());
        category.setStatus(request.getStatus() != null ? request.getStatus() : "active");
        category.setSort(request.getSort() != null ? request.getSort() : 0);
        categoryMapper.insert(category); // 作用：保存分类记录；方法：调用 Mapper 执行数据库插入
        return toResponse(categoryMapper.findById(category.getId())); // 作用：返回分类结果；方法：重新查询新增记录并转换为响应对象
    }

    public Optional<CategoryResponse> update(long id, CategoryUpdateRequest request) {
        Category category = categoryMapper.findById(id);
        if (category == null) {
            return Optional.empty();
        }
        ensureKeyAvailable(id, request.getKey());
        category.setKey(request.getKey());
        category.setName(request.getName());
        category.setStatus(request.getStatus() != null ? request.getStatus() : category.getStatus());
        category.setSort(request.getSort() != null ? request.getSort() : category.getSort());
        categoryMapper.update(category);
        return Optional.of(toResponse(categoryMapper.findById(id)));
    }

    public boolean delete(long id) {
        return categoryMapper.delete(id) > 0;
    }

    private void ensureKeyAvailable(Long currentId, String key) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        Category existing = categoryMapper.findByKey(key);
        if (existing == null) {
            return;
        }
        if (currentId != null && currentId.equals(existing.getId())) {
            return;
        }
        throw new IllegalArgumentException("分类标识已存在");
    }

    private CategoryResponse toResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setKey(category.getKey());
        response.setName(category.getName());
        response.setSort(category.getSort());
        response.setStatus(category.getStatus());
        return response;
    }
}
