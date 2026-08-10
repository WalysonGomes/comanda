package com.comanda.settings;

import com.comanda.menu.UnsupportedImageTypeException;
import com.comanda.menu.images.ImageTypeDetector;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/painel/business-settings")
public class BusinessSettingsController {
    private static final long MAX_LOGO_BYTES = 5L * 1024 * 1024;
    private final BusinessSettingsService service;
    public BusinessSettingsController(BusinessSettingsService service) { this.service = service; }
    @GetMapping public BusinessSettingsResponse get() { return service.get(); }
    @PutMapping public BusinessSettingsResponse update(@Valid @RequestBody BusinessSettingsRequest request) { return service.update(request); }
    @PostMapping("/logo") @ResponseStatus(HttpStatus.CREATED)
    public BusinessSettingsResponse logo(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_LOGO_BYTES) throw new IllegalArgumentException("Logo deve ter no máximo 5 MB.");
        try {
            byte[] content = file.getBytes();
            var type = ImageTypeDetector.detect(content).orElseThrow(UnsupportedImageTypeException::new);
            return service.updateLogo(content, type);
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
