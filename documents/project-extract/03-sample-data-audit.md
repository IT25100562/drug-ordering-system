# Part C — Sample-data audit and completion plan

## 1. What the project seeds

`database/sample-data.sql` seeds plausible demo workflows across the application:
customers and staff, catalog products/categories, an active cart and wishlist, several
orders and payments, prescription review states, notifications, and delivery tracking.
The seed script assumes a fresh schema and relies on identity values being assigned in
the documented insertion order for several sample relationships.

## 2. Current row counts represented by the seed script

Counts below are read from the inserted values/statements in `sample-data.sql`; they are
not a claim that a live SQL Server database was queried.

| Table | Seed rows | Meets minimum 5? | Seed source/notes |
|---|---:|:---:|---|
| `users` | 7 | Yes | 5 staff/customer account types represented; 3 customers |
| `categories` | 8 | Yes | Eight medicine categories |
| `medicines` | 13 | Yes | Includes low/out-of-stock, prescription-only, expired, discontinued examples |
| `cart_items` | 2 | **No** | Two rows, both for Nimal |
| `wishlist_items` | 1 | **No** | One saved medicine for Nimal |
| `orders` | 5 | Yes | Delivered, shipped, paid, cancelled, processing |
| `order_items` | 8 | Yes | Lines associated with five orders |
| `payments` | 5 | Yes | One per order; one is refunded |
| `order_status_history` | 12 | Yes | Multiple lifecycle events across orders |
| `prescriptions` | 7 | Yes | Pending, correction requested, rejected, approved/unpaid, expired, approved/paid |
| `prescription_items` | 3 | **No** | Two items on one prescription and one on another |
| `notifications` | 10 | Yes | Six prescription/order messages plus four delivery messages |
| `deliveries` | 5 | Yes | One per seeded order |
| `delivery_updates` | 16 | Yes | Progress and failed-attempt history |

The assignment explicitly requires **at least five records per table**. The current seed
data therefore does not yet meet Part C for `cart_items`, `wishlist_items`, or
`prescription_items`. These are definite gaps even though the overall sample demonstrates
useful workflows.

## 3. Safe ways to complete the three short tables

Add rows that remain valid and coherent; do not create orphan records or duplicate unique
keys.

- **`cart_items`:** add at least three rows across existing users and active, in-stock
  medicines. Keep each `(user_id, medicine_id)` unique and `quantity > 0`. Ensure any
  row described as an ordinary cart item is not for a prescription-only medicine.
- **`wishlist_items`:** add at least four rows, using distinct `(user_id, medicine_id)`
  pairs. Existing cart membership is not itself forbidden, but varied users/products
  make the sample more convincing.
- **`prescription_items`:** add at least two rows for suitable existing prescriptions,
  with valid medicine IDs, quantities 1–100, unique medicine per prescription, positive
  unit prices, and nonempty dosage instructions. Keep lines consistent with the
  prescription status and narrative. At least one approved prescription is the clearest
  target for prescription order lines.

For a strong submission, increase the row count in all tables beyond the bare minimum
where it improves query variety. Add seed inserts to `database/sample-data.sql` only
after coordinating because it is a shared project file. Re-run from a clean schema to
prove the script is repeatable.

## 4. Demonstrated sample cases

The current seed supports these useful demonstrations:

- `users`: customer, admin, pharmacist, delivery staff; a flagged customer.
- `medicines`: regular and prescription-only products, low stock, zero stock, expired
  medicine, and discontinued medicine.
- `orders`/`payments`: several lifecycle states, paid vs refunded, cart vs prescription
  source, free-delivery threshold example.
- `prescriptions`: pharmacist decision outcomes, correction request, approved unpaid,
  approved paid, and an old pending/expired example.
- `deliveries`/`delivery_updates`: assigned and unassigned riders, delivered parcel,
  repeat failed attempt, and cancelled delivery.

Sample-data comments document the intended fixed order IDs and some later references.
Run the script against the clean schema in the documented sequence; do not manually run
individual insert sections out of order.

## 5. Screenshot checklist

For every one of the 14 tables:

1. Run `SELECT * FROM dbo.<table>;` and sort only if a stable display is useful.
2. Capture the query and all result rows in one readable screenshot (split large tables
   into multiple screenshots if needed).
3. Include enough columns/rows to show IDs, FK links, statuses, and meaningful values.
4. In the report, identify the table and briefly state how its rows respect key/check
   constraints.
5. Keep an evidence log of any added rows and the final count per table.

Useful count check:

```sql
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM dbo.users
UNION ALL SELECT 'categories', COUNT(*) FROM dbo.categories
UNION ALL SELECT 'medicines', COUNT(*) FROM dbo.medicines
UNION ALL SELECT 'cart_items', COUNT(*) FROM dbo.cart_items
UNION ALL SELECT 'wishlist_items', COUNT(*) FROM dbo.wishlist_items
UNION ALL SELECT 'orders', COUNT(*) FROM dbo.orders
UNION ALL SELECT 'order_items', COUNT(*) FROM dbo.order_items
UNION ALL SELECT 'payments', COUNT(*) FROM dbo.payments
UNION ALL SELECT 'order_status_history', COUNT(*) FROM dbo.order_status_history
UNION ALL SELECT 'prescriptions', COUNT(*) FROM dbo.prescriptions
UNION ALL SELECT 'prescription_items', COUNT(*) FROM dbo.prescription_items
UNION ALL SELECT 'notifications', COUNT(*) FROM dbo.notifications
UNION ALL SELECT 'deliveries', COUNT(*) FROM dbo.deliveries
UNION ALL SELECT 'delivery_updates', COUNT(*) FROM dbo.delivery_updates
ORDER BY table_name;
```

This count check is an evidence aid, not a substitute for the required `SELECT *`
screenshots.
