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

-- ------------------------------------------------------------------ drop
-- (child tables first, parent tables last)
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
