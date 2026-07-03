package com.comanda.menu.additionals.web;

import com.comanda.menu.additionals.AdditionalGroupResponse;
import com.comanda.menu.additionals.AdditionalService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdditionalGroupController {

    private final AdditionalService additionalService;

    public AdditionalGroupController(AdditionalService additionalService) {
        this.additionalService = additionalService;
    }

    @GetMapping("/api/painel/products/{productId}/additional-groups")
    public List<AdditionalGroupResponse> list(@PathVariable Long productId) {
        return additionalService.listGroups(productId);
    }

    @PostMapping("/api/painel/products/{productId}/additional-groups")
    public ResponseEntity<AdditionalGroupResponse> create(
            @PathVariable Long productId, @Valid @RequestBody AdditionalGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(additionalService.createGroup(productId, request));
    }

    @PutMapping("/api/painel/additional-groups/{groupId}")
    public AdditionalGroupResponse update(
            @PathVariable Long groupId, @Valid @RequestBody AdditionalGroupRequest request) {
        return additionalService.updateGroup(groupId, request);
    }

    @DeleteMapping("/api/painel/additional-groups/{groupId}")
    public ResponseEntity<Void> delete(@PathVariable Long groupId) {
        additionalService.deleteGroup(groupId);
        return ResponseEntity.noContent().build();
    }
}
