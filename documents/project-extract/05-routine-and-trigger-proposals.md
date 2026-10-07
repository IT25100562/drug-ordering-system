# Parts E and F — Stored procedure and trigger proposals

## Current implementation status

No `CREATE PROCEDURE`, `CREATE FUNCTION`, or `CREATE TRIGGER` statement was found in the
project's `database/` scripts or application source. The following are **proposed
assignment additions**, not existing MediSys features. Add them to a separate SQL
assignment script or the team's database scripts, then execute and capture their actual
results in SQL Server before describing them as functional.

These examples use SQL Server T-SQL and the current schema. Coordinate schema edits with
the team because `database/schema.sql` is a shared file.

## Part E proposal — customer order summary procedure

### Purpose

`dbo.usp_GetCustomerOrderSummary` returns each order for a selected customer, including
its status/value/payment reference and item-line/unit counts. It is relevant to customer
order history and combines the existing `users`, `orders`, `order_items`, and `payments`
relations without changing application behavior.

### Procedure

```sql
CREATE OR ALTER PROCEDURE dbo.usp_GetCustomerOrderSummary
    @CustomerId int
AS
BEGIN
    SET NOCOUNT ON;

    SELECT u.id AS customer_id,
           u.full_name AS customer_name,
           o.id AS order_id,
           o.created_at,
           o.source,
           o.status AS order_status,
           o.subtotal,
           o.delivery_fee,
           o.total,
           COUNT(oi.id) AS item_line_count,
           COALESCE(SUM(oi.quantity), 0) AS total_units,
           p.status AS payment_status,
           p.reference AS payment_reference
    FROM dbo.users AS u
    JOIN dbo.orders AS o
      ON o.user_id = u.id
    LEFT JOIN dbo.order_items AS oi
      ON oi.order_id = o.id
    LEFT JOIN dbo.payments AS p
      ON p.order_id = o.id
    WHERE u.id = @CustomerId
    GROUP BY u.id, u.full_name, o.id, o.created_at, o.source, o.status,
             o.subtotal, o.delivery_fee, o.total, p.status, p.reference
    ORDER BY o.created_at DESC, o.id DESC;
END;
GO
```

### Demonstration call

```sql
DECLARE @CustomerId int;

SELECT @CustomerId = id
FROM dbo.users
WHERE email = N'kasuni@example.com';

EXEC dbo.usp_GetCustomerOrderSummary @CustomerId = @CustomerId;
```

**Expected from the original seed:** Kasuni has three orders (IDs 2, 4, and 5 in the
documented fresh seed), with totals 384.00, 680.00, and 2532.00 respectively. Order 3
belongs to Nimal. Run the procedure and report the actual result grid; the inserted IDs
depend on running the sample script against a freshly created schema.

### What to explain

- The procedure uses a parameter instead of hardcoding one customer.
- `LEFT JOIN` retains an order if a payment or line is absent.
- The aggregate is grouped at order granularity; payment uniqueness prevents duplicate
  payment rows per order.
- `SET NOCOUNT ON` suppresses row-count messages so the result set is clear.

## Part F proposal — audit medicine stock changes

### Purpose and design

The current schema has inventory quantities but no dedicated stock-change audit table.
This trigger proposal adds `medicine_stock_audit` and records old/new stock values for
every medicine row whose stock quantity changes. SQL Server triggers must be set-based:
the implementation joins all changed rows in `inserted` and `deleted`, so multi-row
updates are handled correctly.

### Add the audit table and trigger

Run after the existing `medicines` table has been created:

```sql
CREATE TABLE dbo.medicine_stock_audit (
    audit_id   int IDENTITY(1,1) PRIMARY KEY,
    medicine_id int NOT NULL,
    old_stock  int NOT NULL,
    new_stock  int NOT NULL,
    changed_at datetime2 NOT NULL
        CONSTRAINT df_medicine_stock_audit_changed_at DEFAULT SYSDATETIME(),
    changed_by sysname NOT NULL
        CONSTRAINT df_medicine_stock_audit_changed_by DEFAULT ORIGINAL_LOGIN(),
    CONSTRAINT fk_medicine_stock_audit_medicine
        FOREIGN KEY (medicine_id) REFERENCES dbo.medicines(id),
    CONSTRAINT ck_medicine_stock_audit_values
        CHECK (old_stock >= 0 AND new_stock >= 0)
);
GO

CREATE OR ALTER TRIGGER dbo.trg_medicines_stock_audit
ON dbo.medicines
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.medicine_stock_audit
        (medicine_id, old_stock, new_stock)
    SELECT i.id, d.stock_quantity, i.stock_quantity
    FROM inserted AS i
    JOIN deleted AS d
      ON d.id = i.id
    WHERE i.stock_quantity <> d.stock_quantity;
END;
GO
```

If incorporating this into `database/schema.sql`, create the audit table after
`medicines` and drop the trigger/table before dropping `medicines` (the FK makes drop
order important). The trigger only audits stock changes; name/price/description edits do
not produce audit rows.

### Demonstrate the trigger without retaining test changes

The transaction below demonstrates the trigger while restoring the seeded inventory and
removing its test audit row at rollback. Capture the `SELECT` output **before** the
rollback in the same SQL editor session.

```sql
BEGIN TRANSACTION;

UPDATE dbo.medicines
SET stock_quantity = stock_quantity + 1,
    updated_at = SYSDATETIME()
WHERE name = N'Brufen';

SELECT a.audit_id,
       m.name AS medicine_name,
       a.old_stock,
       a.new_stock,
       a.changed_at,
       a.changed_by
FROM dbo.medicine_stock_audit AS a
JOIN dbo.medicines AS m
  ON m.id = a.medicine_id
WHERE m.name = N'Brufen'
ORDER BY a.audit_id DESC;

ROLLBACK TRANSACTION;
```

Expected effect for a fresh seed: one audit row with `old_stock = 8` and `new_stock = 9`.
Because the update and trigger insert are in the same transaction, rollback restores the
stock and removes the test audit row. If using a different sample database, compare the
old value to the actual initial stock.

### Trigger checks before final submission

- Execute the `CREATE TABLE` and trigger statements on the target SQL Server version.
- Confirm a stock update creates an audit record.
- Confirm an update that leaves stock unchanged creates no record.
- Test a multi-row stock update and verify one audit row per changed medicine.
- Demonstrate rollback or restore the changed stock after a committed test.
- Capture the trigger code, test `UPDATE`, audit result, and restored/unchanged inventory.
- If included in the final schema, update schema documentation, drop order, and schema
  diagram. Do not claim the application currently uses this audit table unless the
  application is changed to query it.
