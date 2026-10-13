package com.eventhub.business.concretes;

import com.eventhub.business.abstracts.CategoryService;
import com.eventhub.dataAccess.CategoryDao;
import com.eventhub.entities.Category;
import com.eventhub.types.output.CategoryOutput;
import com.eventhub.types.request.CategoryRequest;
import com.eventhub.utilities.result.DataResult;
import com.eventhub.utilities.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryManager implements CategoryService {
    private final CategoryDao categoryDao;

    @Override
    public DataResult<List<CategoryOutput>> getAll() {
        List<CategoryOutput> outputs = categoryDao.findAll().stream().map(this::toOutput).collect(Collectors.toList());
        return new DataResult<>(outputs, true, 200);
    }

    @Override
    public Result<String> create(CategoryRequest request) {
        String slug = slugify(request.getName());
        if (categoryDao.findBySlug(slug).isPresent()) {
            return new Result<>(false, 409, "Bu isimde bir kategori zaten var.");
        }
        Category category = Category.builder()
                .name(request.getName().trim())
                .slug(slug)
                .build();
        categoryDao.save(category);
        return new Result<>(true, 200, "Kategori oluşturuldu.");
    }

    @Transactional
    @Override
    public Result<String> update(UUID id, CategoryRequest request) {
        Optional<Category> found = categoryDao.findById(id);
        if (found.isEmpty()) return new Result<>(false, 404, "Geçersiz kategori.");
        Category category = found.get();
        category.setName(request.getName().trim());
        category.setSlug(slugify(request.getName()));
        categoryDao.saveAndFlush(category);
        return new Result<>(true, 200, "Kategori güncellendi.");
    }

    @Transactional
    @Override
    public Result<String> delete(UUID id) {
        if (categoryDao.findById(id).isEmpty()) return new Result<>(false, 404, "Geçersiz kategori.");
        categoryDao.deleteById(id);
        return new Result<>(true, 200, "Kategori silindi.");
    }

    private String slugify(String name) {
        return name.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
    }

    private CategoryOutput toOutput(Category category) {
        return CategoryOutput.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .build();
    }
}
