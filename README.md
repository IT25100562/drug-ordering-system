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
2. Database: create the database with the scripts in [database/](database/) (being
   written in the next step).
3. Copy `src/main/resources/db.properties.example` to `db.properties` in the same folder
   and put in your own SQL Server login. `db.properties` is git-ignored.
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
| 03 | `Medicine`, `Category` | `MedicineDAO` | `MedicineService` | `medicine/` |
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
| 03 | `/admin/medicines`, `/admin/medicines/edit`, `/admin/medicines/discontinue` | admin |
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
