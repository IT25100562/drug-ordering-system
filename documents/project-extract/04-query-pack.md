# Part D — SQL query pack for MediSys

These five SQL Server queries cover the required query categories. They use existing
tables/columns and the current sample-data design. Expected observations are reasoned
from `database/sample-data.sql`; **execute the queries against the final seeded database
and capture actual outputs before including them as results**.

## Query 1 — Simple SELECT: active medicines at/below reorder level

```sql
SELECT id, name, stock_quantity, reorder_level, expiry_date
FROM dbo.medicines
WHERE is_discontinued = 0
  AND stock_quantity <= reorder_level
  AND (expiry_date IS NULL OR expiry_date >= CAST(SYSDATETIME() AS date))
ORDER BY stock_quantity ASC, name ASC;
```

**Type:** simple selection, predicates, projection, ordering.
**Explanation:** identifies current active, non-expired medicines at or below their
reorder point. `stock_quantity <= reorder_level` captures both low and zero stock. The
sample is intended to include Azithromycin, Brufen, and Losartan; validate against the
database date and final sample rows.

## Query 2 — JOIN: orders with customer and payment details

```sql
SELECT o.id AS order_id,
       u.full_name AS customer,
       o.source,
       o.status AS order_status,
       o.total,
       p.status AS payment_status,
       p.reference AS payment_reference
FROM dbo.orders AS o
JOIN dbo.users AS u
  ON u.id = o.user_id
LEFT JOIN dbo.payments AS p
  ON p.order_id = o.id
ORDER BY o.created_at DESC, o.id DESC;
```

**Type:** inner and left joins.
**Explanation:** joins orders to their owner and optional payment. `LEFT JOIN` preserves an
order even if no payment row exists. `payments.order_id` is unique, so payment data cannot
multiply an order in this result. With the current seed, five order rows are expected.

## Query 3 — Aggregation: totals by order status

```sql
SELECT status,
       COUNT(*) AS order_count,
       SUM(total) AS order_value,
       AVG(total) AS average_order_value
FROM dbo.orders
GROUP BY status
ORDER BY status;
```

**Type:** aggregate functions and `GROUP BY`.
**Explanation:** summarizes the number and value of orders at each workflow state.
`order_value` is gross order value, not net collected revenue: the cancelled sample has a
refunded payment and is still included. For a collected-revenue report, join `payments`
and filter `p.status = 'PAID'`.

## Query 4 — GROUP BY / HAVING: popular medicines in non-cancelled orders

```sql
SELECT oi.medicine_id,
       oi.medicine_name,
       SUM(oi.quantity) AS units_ordered,
       COUNT(DISTINCT oi.order_id) AS order_count
FROM dbo.order_items AS oi
JOIN dbo.orders AS o
  ON o.id = oi.order_id
WHERE o.status <> 'CANCELLED'
GROUP BY oi.medicine_id, oi.medicine_name
HAVING SUM(oi.quantity) >= 3
ORDER BY units_ordered DESC, oi.medicine_name;
```

**Type:** join, `GROUP BY`, aggregate filtering via `HAVING`.
**Explanation:** the `WHERE` clause removes cancelled orders before aggregation; `HAVING`
then keeps medicines ordered in quantities of at least three. The sample is designed to
include Benadryl Cough Syrup, Losartan, and Vitamin C. The query groups by both medicine
ID and the order-time name snapshot, avoiding accidental merging of distinct catalog
medicines with the same label.

## Query 5 — Subquery: customers with more orders than the average customer

```sql
WITH customer_order_counts AS (
    SELECT u.id,
           u.full_name,
           COUNT(o.id) AS order_count
    FROM dbo.users AS u
    JOIN dbo.orders AS o
      ON o.user_id = u.id
    WHERE u.role = 'CUSTOMER'
    GROUP BY u.id, u.full_name
)
SELECT customer_id, full_name, order_count
FROM customer_order_counts
WHERE order_count > (
    SELECT AVG(CAST(order_count AS decimal(10,2)))
    FROM customer_order_counts
)
ORDER BY order_count DESC, full_name;
```

**Type:** subquery (scalar aggregate subquery), with a CTE.
**Explanation:** first counts orders for customers who have at least one order, then
returns customers whose count is above the average for that same population. With the
current five-order seed, Nimal has 2 and Kasuni has 3; the average is 2.5, so Kasuni is
expected. If adding customer accounts with zero orders, decide whether they belong in
the average and adjust the CTE to use `LEFT JOIN`.

## Optional performance/evidence extension

The schema has indexes on `orders(user_id, created_at)` and `orders(status, created_at)`,
plus supporting primary-key/unique indexes. Query 3 groups all orders and may scan the
table; that is reasonable for this small demo. Do not claim an index makes every query
optimal without checking the actual execution plan.

For one or two representative statements, enable **Include Actual Execution Plan** in
SSMS (or use the IntelliJ database console) and optionally run:

```sql
SET STATISTICS IO ON;
SET STATISTICS TIME ON;
-- Run one query here, then turn the settings off:
SET STATISTICS IO OFF;
SET STATISTICS TIME OFF;
```

Capture query text, result grid, and plan/statistics separately if they are too crowded
for one screenshot. Describe a plan based on the plan actually observed, not a guessed
index seek/scan.

## Screenshot and explanation template

For each query include:

1. Query number and category.
2. The SQL statement.
3. One or two sentences on the question answered and why the joins/filters/grouping are
   correct.
4. A screenshot showing the query and the real output.
5. A brief check against the sample rows (for example, why cancelled orders are excluded
   or why the average is 2.5).
