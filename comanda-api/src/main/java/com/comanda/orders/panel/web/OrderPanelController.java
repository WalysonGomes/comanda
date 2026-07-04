package com.comanda.orders.panel.web;

import com.comanda.orders.domain.OrderStatus;
import com.comanda.orders.panel.AdvanceOrderRequest;
import com.comanda.orders.panel.CancelOrderRequest;
import com.comanda.orders.panel.OrderDetailResponse;
import com.comanda.orders.panel.OrderPanelService;
import com.comanda.orders.panel.OrdersBoardResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/painel/orders")
public class OrderPanelController {

    private final OrderPanelService orderPanelService;

    public OrderPanelController(OrderPanelService orderPanelService) {
        this.orderPanelService = orderPanelService;
    }

    @GetMapping
    public OrdersBoardResponse board(@RequestParam(required = false) OrderStatus status) {
        return orderPanelService.board(status);
    }

    @GetMapping("/{id}")
    public OrderDetailResponse detail(@PathVariable Long id) {
        return orderPanelService.detail(id);
    }

    @PostMapping("/{id}/advance")
    public OrderDetailResponse advance(@PathVariable Long id, @Valid @RequestBody AdvanceOrderRequest request) {
        return orderPanelService.advance(id, request.fromStatus());
    }

    @PostMapping("/{id}/cancel")
    public OrderDetailResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelOrderRequest request) {
        return orderPanelService.cancel(id, request.reason());
    }
}
