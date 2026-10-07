# Part G — Viva and demonstration preparation

The marking schema allocates 10 marks individually for the viva/demo. It rewards being
able to type and execute SQL accurately, explain results, and reason confidently without
assistance. Use this sheet to rehearse, not as a script to memorize word for word.

## 1. Short project explanation

> MediSys is a Java 17, Jakarta Servlet/JSP online pharmacy application using JDBC and
> Microsoft SQL Server. Its 14-table relational schema separates users, catalog,
> cart/wishlist, orders/payments, prescription review, and delivery tracking. Primary and
> foreign keys preserve relationships; CHECK and UNIQUE constraints enforce domain
> rules; the application uses transactions for multi-table operations such as order
> placement. A user role discriminator represents customer and staff roles in one user
> table.

Adjust the count if the team adds the proposed stock audit table.

## 2. Concepts to be ready to explain

### Keys and relationships

- Why does each table need its PK?
- Which child rows cascade on delete, and why?
- Why is `payments.order_id` unique?
- Why does `deliveries.order_id` have a unique constraint?
- Why does `cart_items` use a unique pair of user and medicine?
- Why is `users.nic` a filtered unique index instead of a regular unique constraint?

### Design choices and refinement

- Why are role variants kept in `users` with a `role` discriminator?
- Why does `order_items` keep medicine name/form/price snapshots?
- Why are prescriptions linked to the medicine catalog rather than storing free-text
  medicine names?
- Why keep discontinued medicines instead of deleting them?
- Which rules are DDL constraints and which live in application services?
- What anomalies could occur if order lines did not snapshot price/name?

### Integrity and transactions

- How does the order total CHECK prevent inconsistent amounts?
- Why must stock reduction, order insertion, payment insertion, and delivery creation
  commit or roll back together?
- What is the purpose of a foreign key versus a CHECK constraint?
- Why does a trigger need to use `inserted` and `deleted` as sets rather than assuming a
  one-row update?

### Query reasoning

- Which joins are required for each query and why?
- What is the difference between `WHERE` and `HAVING`?
- Why use a subquery/aggregate to compare each customer's order count against an average?
- Why is the order-status value query not necessarily net revenue?
- What does an actual execution plan show, and why should you avoid guessing whether an
  index is used?

## 3. Practice SQL from memory

Practice running these against the assignment database:

```sql
SELECT name, stock_quantity, reorder_level
FROM dbo.medicines
WHERE is_discontinued = 0
  AND stock_quantity <= reorder_level
ORDER BY stock_quantity, name;
```

```sql
SELECT o.id, u.full_name, o.status, o.total
FROM dbo.orders AS o
JOIN dbo.users AS u ON u.id = o.user_id
ORDER BY o.id;
```

```sql
SELECT status, COUNT(*) AS order_count, SUM(total) AS order_value
FROM dbo.orders
GROUP BY status
ORDER BY status;
```

Also practice the exact procedure call and trigger demonstration from
`05-routine-and-trigger-proposals.md` after those additions have been created.

## 4. Suggested live demo sequence

1. Show the schema/table relationship diagram and explain the `users.role` mapping.
2. Run one SELECT and one multi-table JOIN.
3. Run an aggregate and explain the grouping level.
4. Run the HAVING example and distinguish it from WHERE.
5. Run the subquery and explain the scalar average.
6. Execute the stored procedure with a customer selected by email.
7. Update one medicine stock value and show the trigger's before/after audit row.
8. Explain one database constraint and one application transaction.

## 5. Last checks

- Every group member can explain the tables they contributed to and the overall design.
- Assignment SQL is tested on SQL Server, not inferred from another database engine.
- The sample database satisfies five-or-more rows in **every** table.
- Query/routine/trigger outputs shown in the PDF are real captured outputs from the final
  database state.
- The EER diagram and relational mapping agree with the actual DDL.
- The SQL script order and clean-database setup are known by each presenter.
