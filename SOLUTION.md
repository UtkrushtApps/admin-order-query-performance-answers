# Solution Steps

1. Identify the source of the slow endpoint: returning Order entities lets JSON serialization traverse lazy Customer and OrderItem/Product associations while Open Session in View is enabled, causing N+1 SQL and leaking persistence internals into the API response.

2. Disable Open Session in View in application.yml so the web layer cannot accidentally trigger lazy database access during serialization.

3. Keep the controller and service boundaries, but change the public return type from Page<Order> to Page<OrderSummaryDto>. This makes the response contract explicit and limits each order row to orderId, customerEmail, totalAmount, createdAt, and itemCount.

4. Replace the derived repository method that loads Order entities with a JPQL constructor projection. Join Customer only for the email, left join OrderItem only to compute count(i.id), group by order fields, and supply a separate countQuery so pagination still reports the correct total number of matching orders.

5. Continue using the existing controller pagination defaults and Sort.by(DESC, "createdAt") so the status filter, page, size, and ordering semantics are preserved.

6. Add a PostgreSQL index on orders(status, created_at DESC, id DESC) including customer_id and total_amount. This supports the common admin query shape: filter by status, order by created_at, and paginate efficiently. Keep the order_items(order_id) index for item counts.

7. Keep the existing Hibernate StatementInspector-based SQL counter and expose the per-request count via logs and an X-SQL-Statement-Count response header so reviewers can observe that the endpoint uses a bounded number of SQL statements.

8. Set hibernate.query.fail_on_pagination_over_collection_fetch=true as a guardrail against future regressions that try to paginate over fetched collections.

9. Expand endpoint tests to seed representative data, assert that only the summary fields are serialized, verify paging/sorting behavior, verify invalid status handling, and assert the bounded SQL statement count for the first PAID page.

