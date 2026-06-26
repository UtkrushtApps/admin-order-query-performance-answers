package com.company;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:orders;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "logging.level.org.hibernate.SQL=INFO"
        })
@AutoConfigureMockMvc
class AdminOrderEndpointTest {
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2024-01-10T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedData() {
        jdbcTemplate.update("delete from order_items");
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("delete from products");
        jdbcTemplate.update("delete from customers");

        for (long customerId = 1; customerId <= 80; customerId++) {
            jdbcTemplate.update(
                    "insert into customers(id, email, full_name, created_at) values (?, ?, ?, ?)",
                    customerId,
                    "customer" + customerId + "@example.com",
                    "Customer " + customerId,
                    BASE_TIME.minusDays(customerId));
        }

        for (long productId = 1; productId <= 30; productId++) {
            jdbcTemplate.update(
                    "insert into products(id, sku, name, price) values (?, ?, ?, ?)",
                    productId,
                    "SKU-" + String.format("%04d", productId),
                    "Product " + productId,
                    BigDecimal.valueOf(10 + (productId * 1.75)).setScale(2));
        }

        for (long orderId = 1; orderId <= 180; orderId++) {
            String status = switch ((int) (orderId % 4)) {
                case 0 -> "CANCELLED";
                case 1, 3 -> "PAID";
                case 2 -> "FULFILLED";
                default -> throw new IllegalStateException("Unexpected modulo");
            };
            jdbcTemplate.update(
                    "insert into orders(id, customer_id, status, total_amount, created_at) values (?, ?, ?, ?, ?)",
                    orderId,
                    ((orderId - 1) % 80) + 1,
                    status,
                    BigDecimal.valueOf(100 + orderId).setScale(2),
                    BASE_TIME.minusHours(orderId));

            for (int itemNo = 1; itemNo <= 4; itemNo++) {
                long productId = ((orderId + itemNo) % 30) + 1;
                jdbcTemplate.update(
                        "insert into order_items(order_id, product_id, quantity, unit_price) values (?, ?, ?, ?)",
                        orderId,
                        productId,
                        ((orderId + itemNo) % 3) + 1,
                        BigDecimal.valueOf(10 + (productId * 1.75)).setScale(2));
            }
        }
    }

    @Test
    void adminOrderSearchReturnsStableSummaryContractWithBoundedSql() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/orders")
                        .param("status", "PAID")
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(50))
                .andExpect(jsonPath("$.content[0].orderId").value(1))
                .andExpect(jsonPath("$.content[0].customerEmail").value("customer1@example.com"))
                .andExpect(jsonPath("$.content[0].totalAmount").value(101.00))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].itemCount").value(4))
                .andExpect(jsonPath("$.content[0].id").doesNotExist())
                .andExpect(jsonPath("$.content[0].status").doesNotExist())
                .andExpect(jsonPath("$.content[0].customer").doesNotExist())
                .andExpect(jsonPath("$.content[0].items").doesNotExist())
                .andReturn();

        String statementCount = result.getResponse().getHeader("X-SQL-Statement-Count");
        assertThat(statementCount).as("SQL statement-count response header").isNotBlank();
        assertThat(Integer.parseInt(statementCount)).isLessThanOrEqualTo(3);
    }

    @Test
    void paidOrderPagingAndSortingSemanticsArePreserved() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .param("status", "PAID")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[0].orderId").value(11));
    }

    @Test
    void invalidStatusReturnsClientError() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .param("status", "UNKNOWN")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
