package com.comanda.platform.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * D5: the SPA build is served as static resources; client-side routes (e.g. {@code /painel})
 * have no server-side handler and 404 through Spring Boot's {@code /error}. Here, a 404 that is
 * neither an API call nor an asset-looking path (has a file extension) is forwarded to {@code
 * index.html} so React Router takes over; everything else keeps its original status.
 */
@Controller
public class SpaFallbackController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request, HttpServletResponse response) {
        Integer status = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String path = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        boolean isNotFound = status != null && status == HttpStatus.NOT_FOUND.value();
        boolean isApiRoute = path != null && path.startsWith("/api/");
        boolean looksLikeAsset = path != null && path.substring(path.lastIndexOf('/') + 1).contains(".");

        if (isNotFound && !isApiRoute && !looksLikeAsset) {
            // A forward keeps whatever status the container already set for the original 404;
            // reset it so React Router sees a normal 200 page instead of an error response.
            response.setStatus(HttpStatus.OK.value());
            return "forward:/index.html";
        }
        return ResponseEntity.status(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR.value()).build();
    }
}
