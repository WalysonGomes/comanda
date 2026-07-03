package com.comanda.menu.products;

import com.comanda.menu.CategoryNotFoundException;
import com.comanda.menu.ProductNotFoundException;
import com.comanda.menu.domain.AdditionalGroupRepository;
import com.comanda.menu.domain.AdditionalItemRepository;
import com.comanda.menu.domain.Category;
import com.comanda.menu.domain.CategoryRepository;
import com.comanda.menu.domain.Product;
import com.comanda.menu.domain.ProductRepository;
import com.comanda.menu.images.ImageStorage;
import com.comanda.menu.products.web.ProductRequest;
import com.comanda.platform.tenancy.TenantContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD isolated by tenant and scoped to a category of the same tenant (design.md Decision 4).
 * Image upload is a separate step (Decision 2): this service only stores whatever
 * {@code imageUrl} it's given and best-effort deletes the previous file when it changes.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AdditionalGroupRepository additionalGroupRepository;
    private final AdditionalItemRepository additionalItemRepository;
    private final ImageStorage imageStorage;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            AdditionalGroupRepository additionalGroupRepository,
            AdditionalItemRepository additionalItemRepository,
            ImageStorage imageStorage) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.additionalGroupRepository = additionalGroupRepository;
        this.additionalItemRepository = additionalItemRepository;
        this.imageStorage = imageStorage;
    }

    public List<ProductResponse> list() {
        return productRepository.findAllByOrderByPositionAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId()).orElseThrow(CategoryNotFoundException::new);
        int position = (int) productRepository.countByCategoryId(category.getId());
        Product product = new Product(
                TenantContext.get(), category, request.name().trim(), request.description(), request.price(), position);
        product.setAvailableDays(validatedDays(request.availableDays()));
        product.setImageUrl(request.imageUrl());
        if (request.available() != null) {
            product.setAvailable(request.available());
        }
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id).orElseThrow(ProductNotFoundException::new);
        Category category = categoryRepository.findById(request.categoryId()).orElseThrow(CategoryNotFoundException::new);

        String previousImageUrl = product.getImageUrl();
        if (previousImageUrl != null && !previousImageUrl.equals(request.imageUrl())) {
            imageStorage.delete(previousImageUrl);
        }

        product.setCategory(category);
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setAvailableDays(validatedDays(request.availableDays()));
        product.setImageUrl(request.imageUrl());
        if (request.available() != null) {
            product.setAvailable(request.available());
        }
        return toResponse(product);
    }

    @Transactional
    public ProductResponse setAvailable(Long id, boolean available) {
        Product product = productRepository.findById(id).orElseThrow(ProductNotFoundException::new);
        product.setAvailable(available);
        return toResponse(product);
    }

    @Transactional
    public void reorder(Long categoryId, List<Long> orderedIds) {
        categoryRepository.findById(categoryId).orElseThrow(CategoryNotFoundException::new);
        List<Product> products = productRepository.findAllById(orderedIds);
        if (products.size() != orderedIds.size()) {
            throw new ProductNotFoundException();
        }
        Map<Long, Product> byId = new LinkedHashMap<>();
        for (Product product : products) {
            if (!product.getCategory().getId().equals(categoryId)) {
                throw new ProductNotFoundException();
            }
            byId.put(product.getId(), product);
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            byId.get(orderedIds.get(i)).setPosition(i);
        }
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id).orElseThrow(ProductNotFoundException::new);
        additionalItemRepository.deleteAllByAdditionalGroupProductId(id);
        additionalGroupRepository.deleteAllByProductId(id);
        if (product.getImageUrl() != null) {
            imageStorage.delete(product.getImageUrl());
        }
        productRepository.delete(product);
    }

    private Integer[] validatedDays(Integer[] days) {
        if (days == null) {
            return null;
        }
        for (Integer day : days) {
            if (day == null || day < 0 || day > 6) {
                throw new IllegalArgumentException("Dia da semana inválido (use 0=Dom … 6=Sáb).");
            }
        }
        return days;
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.isAvailable(),
                product.getPosition(),
                product.getAvailableDays());
    }
}
