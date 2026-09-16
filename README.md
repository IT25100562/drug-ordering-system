# MediSys: Online Medicine Ordering System

SE2030 Software Engineering group project. MediSys is a Java web app where customers order
medicines online. Prescription-only medicines are checked by a senior pharmacist before
the order is released.

> **Status:** project skeleton. Every Java class and JSP page already exists as a stub
> with its owner and a TODO list. Modules are filled in one at a time.

## Tech stack

| Part | Choice |
|------|--------|
| Language | Java 17 (any newer JDK also works) |
| Web | Jakarta Servlets + JSP (`jakarta.servlet`, **not** `javax`) |
| Server | Apache Tomcat 11 |
| Database | Microsoft SQL Server, plain JDBC (`mssql-jdbc` driver) |
| Build | Maven (`pom.xml`). IntelliJ has Maven built in. |
| UI | Plain HTML + one shared CSS file. No frameworks. |

Design patterns used: **Singleton** (`DBConnection`) and **DAO** (a `dao` interface plus
a `dao/impl` JDBC class for every table group).

## Getting started

1. Clone the repo and open the folder in IntelliJ IDEA. When IntelliJ asks, load it as a
   Maven project. It downloads the dependencies itself.
2. **Database** (SQL Server must be running, with TCP port 1433 enabled). From the project
   folder, run the three scripts in order:

   ```
   sqlcmd -S localhost -E -C -i database\create-database.sql
   sqlcmd -S localhost -E -C -d MediSysDB -i database\schema.sql
   sqlcmd -S localhost -E -C -d MediSysDB -i database\sample-data.sql
   ```

   The first script creates the `MediSysDB` database and a `medisys_app` login.
   `schema.sql` **drops and recreates** every table, so run it again (followed by
   `sample-data.sql`) whenever the tables change or you want fresh demo data.
   You can also open the scripts in IntelliJ's Database tool and run them there.
3. Copy `src/main/resources/db.properties.example` to `db.properties` in the same folder.
   It already has the `medisys_app` login. `db.properties` is git-ignored.
4. Add a **Tomcat Server → Local** run configuration (Tomcat 11). On the Deployment tab,
   add the artifact `medisys:war exploded` and set the context path to `/medisys`.
5. Run it and open <http://localhost:8080/medisys/>.

From the command line: `mvn package` builds `target/medisys.war`.

## Folder layout

```
drug-ordering-system/
├── pom.xml
├── database/                      SQL scripts (schema + sample data)
└── src/main/
    ├── java/com/medisys/
    │   ├── config/                DBConnection (shared)
    │   ├── model/                 plain data classes + enums
    │   ├── dao/                   DAO interfaces (what the database can do)
    │   │   └── impl/              JDBC code (how it is done in SQL Server)
    │   ├── service/               business rules, one class per feature
    │   ├── servlet/               controllers, one folder per module
    │   │   ├── user/        (04)
    │   │   ├── medicine/    (03)
    │   │   ├── cart/        (01)
    │   │   ├── order/       (02)
    │   │   ├── prescription/(05)
    │   │   └── delivery/    (06)
    │   ├── filter/                AuthFilter: login + role checks (shared)
    │   └── util/                  SessionUtil, TextUtil, PasswordUtil, AppInitListener
    ├── resources/                 db.properties (your own, not in git)
    └── webapp/
        ├── index.jsp
        ├── css/style.css          one stylesheet for every page (shared)
        ├── js/                    app.js (shared) + one script per module
        └── WEB-INF/
            ├── web.xml
            └── views/             JSP pages, one folder per module
                └── common/        header.jspf, footer.jspf, error.jsp (shared)
```

**Request flow:** browser → `AuthFilter` → servlet → service (rules) → DAO → SQL Server.
The servlet then forwards to a JSP in `WEB-INF/views/`. JSPs are never opened directly.

## Modules and owners

| # | Module | Owner |
|---|--------|-------|
| 01 | Shopping Cart and Wishlist | Amadini G. G. A. |
| 02 | Order Placement and Checkout | Hewage B. H. A. S. |
| 03 | Medicine Catalog and Inventory | Divisekara A. W. D. M. D. M. B. |
| 04 | User and Role Management | Kaweesha P. M. G. S. |
| 05 | Prescription Upload and Verification | Perera D. A. A. N. S. |
| 06 | Delivery Tracking and Notification | Deshabhi R. G. S. |

### Files per module

| Module | model | dao (+ impl) | service | servlet/ + views/ folder |
|--------|-------|--------------|---------|--------------------------|
| 01 | `CartItem`, `WishlistItem` | `CartDAO`, `WishlistDAO` | `CartService`, `WishlistService` | `cart/` |
| 02 | `Order`, `OrderItem`, `OrderStatus`, `Payment` | `OrderDAO`, `PaymentDAO` | `OrderService`, `PaymentService` | `order/` |
| 03 | `Medicine`, `Category`, `InventorySummary` | `MedicineDAO`, `CategoryDAO` | `MedicineService` | `medicine/` |
| 04 | `User`, `Role` | `UserDAO` | `UserService` (+ `util/PasswordUtil`) | `user/` |
| 05 | `Prescription`, `PrescriptionStatus` | `PrescriptionDAO` | `PrescriptionService` | `prescription/` |
| 06 | `Delivery`, `DeliveryStatus`, `Notification` | `DeliveryDAO`, `NotificationDAO` | `DeliveryService`, `NotificationService` | `delivery/` |

**Shared files (agree with the team before changing them):** `pom.xml`, `web.xml`,
`DBConnection`, `SessionUtil`, `TextUtil`, `AppInitListener`, `ValidationException`,
`AuthFilter`, `common/*.jspf`, `css/style.css`, `database/*.sql`.

### URL map

| Module | URL | Who |
|--------|-----|-----|
| 04 | `/login`, `/logout`, `/register` | everyone |
| 04 | `/account/profile` | logged in |
| 04 | `/admin/users` | admin |
| 03 | `/medicines`, `/medicines/view?id=` | everyone |
| 03 | `/admin/medicines`, `/admin/medicines/edit`, `/admin/medicines/restock`, `/admin/medicines/discontinue`, `/admin/categories` | admin |
| 01 | `/cart`, `/cart/add`, `/cart/update`, `/cart/remove` | customer |
| 01 | `/wishlist`, `/wishlist/action` | customer |
| 02 | `/checkout`, `/checkout/payment` | customer |
| 02 | `/orders`, `/orders/view?id=`, `/orders/cancel` | customer |
| 02 | `/admin/orders` | admin |
| 05 | `/prescriptions`, `/prescriptions/upload`, `/prescriptions/correct`, `/prescriptions/file` | customer |
| 05 | `/pharmacist/dashboard`, `/pharmacist/review`, `/pharmacist/delete` | pharmacist |
| 06 | `/deliveries/track?orderId=` | customer |
| 06 | `/staff/deliveries`, `/staff/deliveries/update` | delivery staff / admin |
| 06 | `/notifications` | logged in |

`AuthFilter` protects `/admin/*` for ADMIN, `/pharmacist/*` for PHARMACIST and `/staff/*`
for DELIVERY_STAFF.

### How the modules connect

- **01 → 05:** adding a prescription-only medicine to the cart sends the customer to
  `/prescriptions/upload`. The cart accepts it only after the prescription is APPROVED.
- **01 → 02:** checkout turns the cart into an order.
- **02 → 03:** placing an order reduces stock through `MedicineService`.
- **02 → 06:** a successful payment creates a delivery through `DeliveryService`.
- **05, 02, 06 → 06:** anything that needs to tell a user something calls
  `NotificationService.notify(...)`.

## Module status

| # | Module | Status |
|---|--------|--------|
| 01 | Shopping Cart and Wishlist | not started |
| 02 | Order Placement and Checkout | not started |
| 03 | Medicine Catalog and Inventory | **done** (see below) |
| 04 | User and Role Management | not started |
| 05 | Prescription Upload and Verification | not started |
| 06 | Delivery Tracking and Notification | not started |

### Module 03: Medicine Catalog and Inventory

**Customer pages**
- `/medicines`: catalog cards with search (name or manufacturer), category filter, price,
  stock badge and a "Prescription" badge. Discontinued and expired medicines are hidden.
- `/medicines/view?id=`: full details. Unknown, discontinued or expired medicines give a 404.
  The Add to Cart and Add to Wishlist buttons post to module 01.

**Admin pages**
- `/admin/medicines`: inventory table with totals (active, low stock, out of stock,
  expired), filter tabs, search and category filter. Per row: add stock, Edit,
  Discontinue (with a confirm box), or Restore on the Discontinued tab.
- `/admin/medicines/edit`: add / edit form. All errors are shown together, and the
  typed values are kept.
- `/admin/categories`: list, add, and delete categories. A category can only be deleted
  when no medicine uses it.

**Rules (all in `MedicineService`, repeated in `js/medicine.js` for quick feedback)**
- name 2-150 characters; category and dosage form must be from the lists
- price more than 0, at most Rs. 1,000,000, at most 2 decimals
- stock 0-100000, reorder level 0-10000, restock quantity 1-10000
- expiry date cannot be in the past (an already saved past date may stay)
- no two medicines with the same name and strength
- "Delete" is a soft delete (`is_discontinued`), so old orders keep their medicine
- a discontinued medicine must be restored before stock can be added

**For other modules**
- 01 (cart): `medicineService.getAvailableMedicine(id)` returns the medicine or throws a
  `ValidationException` with a message (not available / out of stock).
  `medicine.isRequiresPrescription()` tells you to send the customer to module 05.
- 02 (orders): `medicineService.reduceStock(id, qty)` takes stock out safely. If two
  orders arrive at the same time, the stock still can't go below 0. It throws a
  `ValidationException` when there is not enough stock.

**Not yet:** the admin pages are open to everyone until module 04 adds login. After that,
`AuthFilter` must allow `/admin/*` only for ADMIN.

## Coding rules

- Simple, readable, commented code. No Spring, Hibernate or Lombok.
- Business rules live in the **service** class, not in servlets or JSPs.
- SQL lives only in **dao/impl** classes. Always use `PreparedStatement`, never string
  concatenation.
- Escape all user text before printing it in a JSP (`TextUtil.html(...)`).
- Every page includes `common/header.jspf` and `common/footer.jspf` and uses
  `css/style.css`.

## Git workflow

- `main`: stable, demo-ready code.
- `Dev`: integration branch. Merge feature branches here first.
- `feature/<module-name>`: one branch per module, e.g. `feature/prescription-module`.
  Open a PR into `Dev`.
