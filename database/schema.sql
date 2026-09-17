/*
    MediSys - step 2 of 3: create all tables.
    Shared file - agree with the team before changing it.

        sqlcmd -S localhost -E -C -d MediSysDB -i database\schema.sql

    WARNING: this script drops the tables first, so it deletes all data.
    Run sample-data.sql afterwards to get the demo data back.

    The tables are added module by module as each module is built.
    When you add a table:
      1. add its CREATE TABLE in your module's section below
      2. add a DROP line in the "drop" section - tables that point to other
         tables (foreign keys) must be dropped FIRST, so put yours above the
         tables it references.
*/

USE MediSysDB;
GO

-- Recommended SQL Server setting (sqlcmd turns it off by default).
SET QUOTED_IDENTIFIER ON;
GO

-- ------------------------------------------------------------------ drop
-- (child tables first, parent tables last)
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS delivery_updates;
DROP TABLE IF EXISTS deliveries;
DROP TABLE IF EXISTS prescription_items;
DROP TABLE IF EXISTS prescriptions;
DROP TABLE IF EXISTS order_status_history;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS wishlist_items;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS medicines;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;
GO


-- =================================================================
-- Module 04 - User and Role Management (Kaweesha P. M. G. S.)
-- =================================================================

-- Everyone who can log in. role decides which pages they can open.
-- password_hash is a salted PBKDF2 hash (see PasswordUtil) - never the password.
CREATE TABLE users (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    full_name     NVARCHAR(100) NOT NULL,
    email         NVARCHAR(150) NOT NULL,
    phone         NVARCHAR(20)  NULL,
    address       NVARCHAR(255) NULL,
    password_hash VARCHAR(255)  NOT NULL,
    role          VARCHAR(20)   NOT NULL DEFAULT 'CUSTOMER',
    is_active     BIT           NOT NULL DEFAULT 1,
    created_at    DATETIME2     NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role  CHECK (role IN ('CUSTOMER', 'PHARMACIST', 'ADMIN', 'DELIVERY_STAFF'))
);
GO


-- =================================================================
-- Module 03 - Medicine Catalog and Inventory (Divisekara A. W. D. M. D. M. B.)
-- =================================================================

-- A group of medicines, e.g. "Pain Relief" or "Antibiotics".
CREATE TABLE categories (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL,
    description NVARCHAR(255) NULL,
    created_at  DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT uq_categories_name UNIQUE (name)
);

-- A medicine sold by the pharmacy.
-- is_discontinued = 1 hides it from the catalog without deleting it, so old
-- orders that point to it still work (a "soft delete").
CREATE TABLE medicines (
    id                    INT IDENTITY(1,1) PRIMARY KEY,
    name                  NVARCHAR(150)  NOT NULL,
    category_id           INT            NOT NULL,
    manufacturer          NVARCHAR(150)  NULL,
    dosage_form           NVARCHAR(30)   NOT NULL,          -- Tablet, Capsule, Syrup ...
    strength              NVARCHAR(50)   NULL,              -- e.g. 500 mg
    description           NVARCHAR(2000) NULL,
    price                 DECIMAL(10,2)  NOT NULL,
    stock_quantity        INT            NOT NULL DEFAULT 0,
    reorder_level         INT            NOT NULL DEFAULT 10, -- "low stock" at or below this
    requires_prescription BIT            NOT NULL DEFAULT 0,  -- 1 = prescription-only (module 05)
    expiry_date           DATE           NULL,
    is_discontinued       BIT            NOT NULL DEFAULT 0,
    created_at            DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_at            DATETIME2      NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT fk_medicines_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_medicines_price    CHECK (price > 0),
    CONSTRAINT ck_medicines_stock    CHECK (stock_quantity >= 0),
    CONSTRAINT ck_medicines_reorder  CHECK (reorder_level >= 0)
);

CREATE INDEX ix_medicines_name ON medicines (name);
GO


-- =================================================================
-- Module 01 - Shopping Cart and Wishlist (Amadini G. G. A.)
-- =================================================================

-- One line per medicine in a customer's cart. Adding the same medicine again
-- increases quantity instead of adding a second line (unique user + medicine).
CREATE TABLE cart_items (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    user_id     INT       NOT NULL,
    medicine_id INT       NOT NULL,
    quantity    INT       NOT NULL,
    added_at    DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT fk_cart_user      FOREIGN KEY (user_id)     REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_medicine  FOREIGN KEY (medicine_id) REFERENCES medicines (id),
    CONSTRAINT uq_cart_line      UNIQUE (user_id, medicine_id),
    CONSTRAINT ck_cart_quantity  CHECK (quantity > 0)
);

-- Medicines a customer saved for later.
CREATE TABLE wishlist_items (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    user_id     INT       NOT NULL,
    medicine_id INT       NOT NULL,
    added_at    DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT fk_wishlist_user     FOREIGN KEY (user_id)     REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id),
    CONSTRAINT uq_wishlist_line     UNIQUE (user_id, medicine_id)
);
GO


-- =================================================================
-- Module 02 - Order Placement and Checkout (Hewage B. H. A. S.)
-- =================================================================

-- A placed (and paid) order.
--   source: CART          - checked out from the shopping cart
--           PRESCRIPTION  - paid for a prescription (prescriptions.order_id points here)
--   status: PAID -> PROCESSING -> SHIPPED -> DELIVERED, or CANCELLED (before shipping)
-- The delivery details are copied into the order, so later changes to the
-- customer's profile do not change old orders.
CREATE TABLE orders (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    user_id          INT            NOT NULL,
    source           VARCHAR(15)    NOT NULL,
    status           VARCHAR(15)    NOT NULL DEFAULT 'PAID',
    subtotal         DECIMAL(10,2)  NOT NULL,
    delivery_fee     DECIMAL(10,2)  NOT NULL,
    total            DECIMAL(10,2)  NOT NULL,
    delivery_name    NVARCHAR(100)  NOT NULL,
    delivery_address NVARCHAR(255)  NOT NULL,
    delivery_phone   NVARCHAR(20)   NOT NULL,
    delivery_note    NVARCHAR(300)  NULL,
    cancel_reason    NVARCHAR(300)  NULL,
    created_at       DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_at       DATETIME2      NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT fk_orders_user    FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_orders_source  CHECK (source IN ('CART', 'PRESCRIPTION')),
    CONSTRAINT ck_orders_status  CHECK (status IN ('PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT ck_orders_total   CHECK (total = subtotal + delivery_fee AND subtotal > 0 AND delivery_fee >= 0)
);

CREATE INDEX ix_orders_user   ON orders (user_id, created_at);
CREATE INDEX ix_orders_status ON orders (status, created_at);

-- One medicine in an order. Name and price are copied at order time, so the
-- order stays correct even if the medicine is renamed or its price changes.
CREATE TABLE order_items (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    order_id            INT            NOT NULL,
    medicine_id         INT            NOT NULL,
    medicine_name       NVARCHAR(210)  NOT NULL,        -- e.g. Panadol 500 mg
    dosage_form         NVARCHAR(30)   NOT NULL,
    unit_price          DECIMAL(10,2)  NOT NULL,
    quantity            INT            NOT NULL,
    dosage_instructions NVARCHAR(300)  NULL,            -- only for prescription orders

    CONSTRAINT fk_order_items_order    FOREIGN KEY (order_id)    REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_price    CHECK (unit_price > 0)
);

-- The (test) card payment of an order. Only the last 4 digits of the card are kept.
CREATE TABLE payments (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    order_id     INT            NOT NULL,
    amount       DECIMAL(10,2)  NOT NULL,
    method       VARCHAR(20)    NOT NULL DEFAULT 'TEST_CARD',
    card_last4   CHAR(4)        NOT NULL,
    reference    VARCHAR(30)    NOT NULL,
    status       VARCHAR(10)    NOT NULL DEFAULT 'PAID',
    paid_at      DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    refunded_at  DATETIME2      NULL,

    CONSTRAINT fk_payments_order     FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT uq_payments_order     UNIQUE (order_id),
    CONSTRAINT uq_payments_reference UNIQUE (reference),
    CONSTRAINT ck_payments_status    CHECK (status IN ('PAID', 'REFUNDED'))
);

-- Every status an order went through, for the timeline on the order page.
CREATE TABLE order_status_history (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    order_id   INT            NOT NULL,
    status     VARCHAR(15)    NOT NULL,
    note       NVARCHAR(300)  NULL,
    changed_by INT            NULL,                     -- NULL = the customer / the system
    changed_at DATETIME2      NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT fk_history_order FOREIGN KEY (order_id)   REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_history_user  FOREIGN KEY (changed_by) REFERENCES users (id)
);
GO


-- =================================================================
-- Module 05 - Prescription Upload and Verification (Perera D. A. A. N. S.)
-- =================================================================

-- A prescription a customer uploaded (a photo or PDF).
--   status: PENDING -> APPROVED / REJECTED / CORRECTION_REQUESTED
--           CORRECTION_REQUESTED -> PENDING (when a new copy is uploaded)
-- The customer only uploads the file. The pharmacist reads it and, when
-- approving, writes down the medicines (prescription_items).
-- The file itself is NOT in the database: file_key says where the file storage
-- (see com.medisys.storage) keeps it.
-- order_id is set when the customer pays: paying creates an order (module 02),
-- and the payment details live in orders / payments. A paid prescription can
-- no longer be deleted. If that order is cancelled, order_id goes back to NULL.
CREATE TABLE prescriptions (
    id                 INT IDENTITY(1,1) PRIMARY KEY,
    user_id            INT            NOT NULL,
    customer_note      NVARCHAR(500)  NULL,
    file_key           VARCHAR(200)   NOT NULL,
    original_file_name NVARCHAR(255)  NOT NULL,
    content_type       VARCHAR(50)    NOT NULL,
    file_size          INT            NOT NULL,
    status             VARCHAR(25)    NOT NULL DEFAULT 'PENDING',
    pharmacist_note    NVARCHAR(500)  NULL,
    reviewed_by        INT            NULL,
    reviewed_at        DATETIME2      NULL,
    correction_count   INT            NOT NULL DEFAULT 0,
    uploaded_at        DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    updated_at         DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    order_id           INT            NULL,             -- the order that paid for it

    CONSTRAINT fk_rx_user      FOREIGN KEY (user_id)     REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_rx_reviewer  FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT fk_rx_order     FOREIGN KEY (order_id)    REFERENCES orders (id),
    CONSTRAINT ck_rx_status    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CORRECTION_REQUESTED')),
    CONSTRAINT ck_rx_type      CHECK (content_type IN ('image/jpeg', 'image/png', 'application/pdf')),
    CONSTRAINT ck_rx_paid      CHECK (order_id IS NULL OR status = 'APPROVED'),
    CONSTRAINT uq_rx_file      UNIQUE (file_key)
);

CREATE INDEX ix_rx_status ON prescriptions (status, uploaded_at);
CREATE INDEX ix_rx_user   ON prescriptions (user_id);

-- The medicines the pharmacist wrote down for an approved prescription:
-- how many, how to use them, and the price at the time of approval.
CREATE TABLE prescription_items (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    prescription_id     INT            NOT NULL,
    medicine_id         INT            NOT NULL,
    quantity            INT            NOT NULL,
    dosage_instructions NVARCHAR(300)  NOT NULL,        -- e.g. 1 tablet twice daily after meals
    unit_price          DECIMAL(10,2)  NOT NULL,

    CONSTRAINT fk_rx_item_rx       FOREIGN KEY (prescription_id) REFERENCES prescriptions (id) ON DELETE CASCADE,
    CONSTRAINT fk_rx_item_medicine FOREIGN KEY (medicine_id)     REFERENCES medicines (id),
    CONSTRAINT uq_rx_item          UNIQUE (prescription_id, medicine_id),
    CONSTRAINT ck_rx_item_quantity CHECK (quantity BETWEEN 1 AND 100),
    CONSTRAINT ck_rx_item_price    CHECK (unit_price > 0)
);
GO


-- =================================================================
-- Module 06 - Delivery Tracking and Notification (Deshabhi R. G. S.)
-- =================================================================

-- A message the system sends to one user (prescription decisions, order updates ...).
CREATE TABLE notifications (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    user_id    INT           NOT NULL,
    message    NVARCHAR(500) NOT NULL,
    link       NVARCHAR(300) NULL,           -- page to open, e.g. /prescriptions
    is_read    BIT           NOT NULL DEFAULT 0,
    created_at DATETIME2     NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX ix_notifications_user ON notifications (user_id, is_read);

-- The delivery of one paid order. It is created together with the order, in
-- the same transaction (see OrderDAOImpl.create).
--   status: PENDING -> DISPATCHED -> OUT_FOR_DELIVERY -> DELIVERED
--           OUT_FOR_DELIVERY -> FAILED -> OUT_FOR_DELIVERY (the rider tries again)
--           PENDING -> CANCELLED (when the order is cancelled)
-- Dispatching moves the order to SHIPPED and delivering moves it to DELIVERED,
-- in the same transaction (see DeliveryDAOImpl.updateStatus).
CREATE TABLE deliveries (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    order_id       INT          NOT NULL,
    staff_id       INT          NULL,               -- the rider; NULL until the admin assigns one
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts       INT          NOT NULL DEFAULT 0, -- how many times a rider went out with it
    estimated_date DATE         NOT NULL,
    delivered_at   DATETIME2    NULL,
    created_at     DATETIME2    NOT NULL DEFAULT SYSDATETIME(),
    updated_at     DATETIME2    NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT fk_delivery_order    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_staff    FOREIGN KEY (staff_id) REFERENCES users (id),
    CONSTRAINT uq_delivery_order    UNIQUE (order_id),
    CONSTRAINT ck_delivery_status   CHECK (status IN ('PENDING', 'DISPATCHED', 'OUT_FOR_DELIVERY',
                                                      'DELIVERED', 'FAILED', 'CANCELLED')),
    CONSTRAINT ck_delivery_attempts CHECK (attempts >= 0),
    -- a delivery that left the pharmacy always has a rider
    CONSTRAINT ck_delivery_rider    CHECK (staff_id IS NOT NULL OR status IN ('PENDING', 'CANCELLED'))
);

CREATE INDEX ix_deliveries_staff  ON deliveries (staff_id, status);
CREATE INDEX ix_deliveries_status ON deliveries (status, created_at);

-- Every step of a delivery, for the tracking page ("Out for delivery" at 10:32 by Ruwan).
CREATE TABLE delivery_updates (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    delivery_id INT           NOT NULL,
    status      VARCHAR(20)   NOT NULL,
    note        NVARCHAR(300) NULL,
    updated_by  INT           NULL,                 -- NULL = the system
    created_at  DATETIME2     NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT fk_update_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries (id) ON DELETE CASCADE,
    CONSTRAINT fk_update_user     FOREIGN KEY (updated_by)  REFERENCES users (id)
);

CREATE INDEX ix_delivery_updates ON delivery_updates (delivery_id, created_at);
GO
