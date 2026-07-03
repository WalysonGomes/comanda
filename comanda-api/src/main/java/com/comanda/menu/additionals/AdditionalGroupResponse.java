package com.comanda.menu.additionals;

import com.comanda.menu.domain.SelectionType;
import java.util.List;

public record AdditionalGroupResponse(
        Long id,
        Long productId,
        String name,
        boolean required,
        SelectionType selectionType,
        Integer minSelections,
        Integer maxSelections,
        List<AdditionalItemResponse> items) {
}
