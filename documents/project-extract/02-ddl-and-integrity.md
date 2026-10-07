# Part B — SQL DDL and integrity notes

## 1. Existing implementation

The project creates the `MediSysDB` database in `database/create-database.sql`; all 14
application tables, constraints, indexes, and drop/recreate instructions are in
`database/schema.sql`. The DDL is for **Microsoft SQL Server** and is organized by the
six application modules.

The schema script deliberately drops existing tables before recreating them. Running it
against a database with important data will destroy that data. It is intended for setup
or a disposable assignment/demo database and must be followed by `sample-data.sql`.

## 2. DDL integrity feature checklist

| Mechanism | Project examples | Why it matters |
|---|---|---|
| Primary keys | Identity `id` on all 14 tables | Stable row identity and referenced keys |
| Foreign keys | `medicines.category_id`, `orders.user_id`, `order_items.order_id`, `prescriptions.user_id`, etc. | Prevents orphan references |
| CHECK constraints | Order/payment/prescription/delivery statuses; positive prices; nonnegative stock; valid quantities | Enforces domain rules consistently at the database boundary |
| UNIQUE constraints | User email, category name, cart/wishlist user+medicine, payment/order, delivery/order, prescription/file key | Prevents duplicate identity or workflow rows |
| Filtered unique index | Unique `users.nic` only where NIC is not NULL | Allows staff without NIC but prevents duplicate supplied NICs |
| Indexes | Medicine name; orders by user/time and status/time; prescriptions by status/time and user; notification user/read; delivery staff/status and status/time; delivery-update delivery/time | Supports frequent lookup/filter/order patterns |
| Cascading deletes | Cart/wishlist by user; order children; prescription items; notification by user; delivery updates | Removes dependent rows where their lifecycle belongs to the parent |
| Non-cascading references | Medicine references from historic order/prescription lines | Keeps referenced product history from being casually deleted |

## 3. Important table-level constraints

| Table | Constraint coverage to call out in the report |
|---|---|
| `users` | email unique; `role` limited to four allowed values; customer NIC/DOB/phone required; flag requires a reason; NIC unique when present; optional self-FK for flagging user |
| `medicines` | category FK; price strictly positive; stock and reorder level nonnegative |
| `cart_items` | positive quantity and no duplicate medicine per user |
| `wishlist_items` | no duplicate medicine per user |
| `orders` | source and lifecycle status restricted; subtotal positive; fee nonnegative; total reconciles to subtotal + fee |
| `order_items` | quantity and unit price positive; valid order and catalog medicine |
| `payments` | one payment per order; unique payment reference; status PAID/REFUNDED |
| `prescriptions` | status and MIME type allowlists; unique storage key; a linked paid order requires APPROVED status |
| `prescription_items` | quantity 1–100, positive unit price, one line per medicine per prescription |
| `deliveries` | one delivery per order; valid status/nonnegative attempts; an assigned rider is required once status leaves PENDING/CANCELLED |

The application enforces additional rules that are not fully represented by DDL
constraints, such as status-transition sequencing, a minimum number of prescription
medicine lines before approval, customer ownership/authorization, and transaction
coordination. State clearly which layer enforces each rule.

## 4. Data types and consistency

- IDs and quantities use `INT`; money uses `DECIMAL(10,2)` rather than approximate
  floating-point types.
- Person-facing text uses `NVARCHAR` to support Unicode names/addresses/notes.
- Enumerated states are stored as bounded `VARCHAR` values, reinforced with CHECKs.
- Timestamps use `DATETIME2`; calendar-only values such as DOB, expiry, and estimated
  delivery use `DATE`.
- Boolean-like fields use SQL Server `BIT`.
- Prescription file bytes are held by file storage, not in SQL; the database stores a
  unique `file_key`, original name, MIME type, and size.

## 5. Transactional consistency beyond DDL

The project uses JDBC transactions for multi-table operations:

- `OrderDAOImpl.create(...)` coordinates stock decrement, order, order lines, payment,
  initial status history, delivery creation, and either prescription linkage or cart
  cleanup. On failure, it rolls back.
- `DeliveryDAOImpl` coordinates delivery state, delivery history, and relevant order
  state changes in a transaction.

These behaviors are visible in `src/main/java/com/medisys/dao/impl/OrderDAOImpl.java`
and `DeliveryDAOImpl.java`. Explain that the FK and CHECK constraints protect individual
values while transactions protect all-or-nothing multi-table operations.

## 6. How to reproduce the database

From the repository root in a SQL Server environment:

```text
sqlcmd -S localhost -E -C -i database\create-database.sql
sqlcmd -S localhost -E -C -d MediSysDB -i database\schema.sql
sqlcmd -S localhost -E -C -d MediSysDB -i database\sample-data.sql
```

Use the equivalent path separator for Linux or execute the scripts in SSMS/IntelliJ.
The first script creates the database and development login; the second drops/recreates
the tables; the third inserts demo data.

**Assignment evidence to capture:** show the DDL for all tables and constraints (not only
the ERD), then capture selected table definitions or a schema diagram in SQL Server.
If adding the proposed trigger audit table from document 05, update the final schema
diagram and DDL appendix as well.
