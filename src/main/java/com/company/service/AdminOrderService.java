package com.company.service;

import com.company.domain.OrderStatus;
import com.company.dto.OrderSummaryDto;
import com.company.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOrderService {
    private final OrderRepository orderRepository;

    public AdminOrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> searchOrders(OrderStatus status, Pageable pageable) {
        return orderRepository.findOrderSummariesByStatus(status, pageable);
    }
}
