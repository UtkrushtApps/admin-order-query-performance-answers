package com.company.repository;

import com.company.domain.Order;
import com.company.domain.OrderStatus;
import com.company.dto.OrderSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query(
            value = """
                    select new com.company.dto.OrderSummaryDto(
                        o.id,
                        c.email,
                        o.totalAmount,
                        o.createdAt,
                        count(i.id)
                    )
                    from Order o
                    join o.customer c
                    left join o.items i
                    where o.status = :status
                    group by o.id, c.email, o.totalAmount, o.createdAt
                    """,
            countQuery = """
                    select count(o.id)
                    from Order o
                    where o.status = :status
                    """)
    Page<OrderSummaryDto> findOrderSummariesByStatus(@Param("status") OrderStatus status, Pageable pageable);
}
