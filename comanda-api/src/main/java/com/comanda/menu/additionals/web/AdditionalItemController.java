package com.comanda.menu.additionals.web;

import com.comanda.menu.additionals.AdditionalItemResponse;
import com.comanda.menu.additionals.AdditionalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdditionalItemController {

    private final AdditionalService additionalService;

    public AdditionalItemController(AdditionalService additionalService) {
        this.additionalService = additionalService;
    }

    @PostMapping("/api/painel/additional-groups/{groupId}/items")
    public ResponseEntity<AdditionalItemResponse> create(
            @PathVariable Long groupId, @Valid @RequestBody AdditionalItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(additionalService.createItem(groupId, request));
    }

    @PutMapping("/api/painel/additional-items/{itemId}")
    public AdditionalItemResponse update(@PathVariable Long itemId, @Valid @RequestBody AdditionalItemRequest request) {
        return additionalService.updateItem(itemId, request);
    }

    @DeleteMapping("/api/painel/additional-items/{itemId}")
    public ResponseEntity<Void> delete(@PathVariable Long itemId) {
        additionalService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
