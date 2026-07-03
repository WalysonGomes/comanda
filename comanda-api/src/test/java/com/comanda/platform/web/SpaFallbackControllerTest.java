package com.comanda.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * D5 / spec `platform-runtime`: an app route 404 forwards to index.html and resets the status to
 * 200; an API route 404 and a missing-asset-looking 404 keep their original status untouched.
 */
class SpaFallbackControllerTest {

    private final SpaFallbackController controller = new SpaFallbackController();

    @Test
    void appRouteNotFoundForwardsToIndexHtmlAndResetsStatusToOk() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object result = controller.handleError(notFoundRequestFor("/painel/pedidos"), response);

        assertThat(result).isEqualTo("forward:/index.html");
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void apiRouteNotFoundIsNotForwarded() {
        Object result = controller.handleError(notFoundRequestFor("/api/orders/999"), new MockHttpServletResponse());

        assertThat(result).isInstanceOf(ResponseEntity.class);
        assertThat(((ResponseEntity<?>) result).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void missingAssetLookingPathIsNotForwarded() {
        Object result = controller.handleError(notFoundRequestFor("/assets/missing.js"), new MockHttpServletResponse());

        assertThat(result).isInstanceOf(ResponseEntity.class);
        assertThat(((ResponseEntity<?>) result).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private MockHttpServletRequest notFoundRequestFor(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.NOT_FOUND.value());
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, path);
        return request;
    }
}
