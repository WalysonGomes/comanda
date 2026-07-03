package com.comanda.menu.additionals;

import com.comanda.menu.AdditionalGroupNotFoundException;
import com.comanda.menu.AdditionalItemNotFoundException;
import com.comanda.menu.ProductNotFoundException;
import com.comanda.menu.additionals.web.AdditionalGroupRequest;
import com.comanda.menu.additionals.web.AdditionalItemRequest;
import com.comanda.menu.domain.AdditionalGroup;
import com.comanda.menu.domain.AdditionalGroupRepository;
import com.comanda.menu.domain.AdditionalItem;
import com.comanda.menu.domain.AdditionalItemRepository;
import com.comanda.menu.domain.Product;
import com.comanda.menu.domain.ProductRepository;
import com.comanda.menu.domain.SelectionType;
import com.comanda.platform.tenancy.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Groups and items have no {@code tenant_id} column (design.md Decision 1): ownership is
 * validated by walking item → group → product → tenant, either through {@link ProductRepository}
 * (tenant-filtered) for the product itself, or through the explicit joins in
 * {@link AdditionalGroupRepository#findByIdAndTenantId} / {@link AdditionalItemRepository#findByIdAndTenantId}.
 */
@Service
public class AdditionalService {

    private final ProductRepository productRepository;
    private final AdditionalGroupRepository groupRepository;
    private final AdditionalItemRepository itemRepository;

    public AdditionalService(
            ProductRepository productRepository,
            AdditionalGroupRepository groupRepository,
            AdditionalItemRepository itemRepository) {
        this.productRepository = productRepository;
        this.groupRepository = groupRepository;
        this.itemRepository = itemRepository;
    }

    public List<AdditionalGroupResponse> listGroups(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(ProductNotFoundException::new);
        return groupRepository.findAllByProductId(product.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdditionalGroupResponse createGroup(Long productId, AdditionalGroupRequest request) {
        Product product = productRepository.findById(productId).orElseThrow(ProductNotFoundException::new);
        validateCoherence(request.selectionType(), request.minSelections(), request.maxSelections());
        AdditionalGroup group = new AdditionalGroup(
                product, request.name().trim(), request.required(), request.selectionType(),
                request.minSelections(), request.maxSelections());
        return toResponse(groupRepository.save(group));
    }

    @Transactional
    public AdditionalGroupResponse updateGroup(Long groupId, AdditionalGroupRequest request) {
        AdditionalGroup group = groupRepository.findByIdAndTenantId(groupId, TenantContext.get())
                .orElseThrow(AdditionalGroupNotFoundException::new);
        validateCoherence(request.selectionType(), request.minSelections(), request.maxSelections());
        group.setName(request.name().trim());
        group.setRequired(request.required());
        group.setSelectionType(request.selectionType());
        group.setMinSelections(request.minSelections());
        group.setMaxSelections(request.maxSelections());
        return toResponse(group);
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        AdditionalGroup group = groupRepository.findByIdAndTenantId(groupId, TenantContext.get())
                .orElseThrow(AdditionalGroupNotFoundException::new);
        itemRepository.deleteAllByAdditionalGroupId(group.getId());
        groupRepository.delete(group);
    }

    @Transactional
    public AdditionalItemResponse createItem(Long groupId, AdditionalItemRequest request) {
        AdditionalGroup group = groupRepository.findByIdAndTenantId(groupId, TenantContext.get())
                .orElseThrow(AdditionalGroupNotFoundException::new);
        AdditionalItem item = new AdditionalItem(group, request.name().trim(), request.additionalPrice());
        if (request.available() != null) {
            item.setAvailable(request.available());
        }
        return toResponse(itemRepository.save(item));
    }

    @Transactional
    public AdditionalItemResponse updateItem(Long itemId, AdditionalItemRequest request) {
        AdditionalItem item = itemRepository.findByIdAndTenantId(itemId, TenantContext.get())
                .orElseThrow(AdditionalItemNotFoundException::new);
        item.setName(request.name().trim());
        item.setAdditionalPrice(request.additionalPrice());
        if (request.available() != null) {
            item.setAvailable(request.available());
        }
        return toResponse(item);
    }

    @Transactional
    public void deleteItem(Long itemId) {
        AdditionalItem item = itemRepository.findByIdAndTenantId(itemId, TenantContext.get())
                .orElseThrow(AdditionalItemNotFoundException::new);
        itemRepository.delete(item);
    }

    private void validateCoherence(SelectionType selectionType, Integer min, Integer max) {
        if (max != null && min > max) {
            throw new IllegalArgumentException("Mínimo de seleções não pode ser maior que o máximo.");
        }
        if (selectionType == SelectionType.SINGLE) {
            if (min > 1) {
                throw new IllegalArgumentException("Seleção única permite no máximo 1 seleção obrigatória.");
            }
            if (max != null && max > 1) {
                throw new IllegalArgumentException("Seleção única permite no máximo 1 item selecionável.");
            }
        }
    }

    private AdditionalGroupResponse toResponse(AdditionalGroup group) {
        List<AdditionalItemResponse> items = itemRepository.findAllByAdditionalGroupId(group.getId()).stream()
                .map(this::toResponse)
                .toList();
        return new AdditionalGroupResponse(
                group.getId(), group.getProduct().getId(), group.getName(), group.isRequired(),
                group.getSelectionType(), group.getMinSelections(), group.getMaxSelections(), items);
    }

    private AdditionalItemResponse toResponse(AdditionalItem item) {
        return new AdditionalItemResponse(
                item.getId(), item.getAdditionalGroup().getId(), item.getName(), item.getAdditionalPrice(), item.isAvailable());
    }
}
