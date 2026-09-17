# MediSys: Online Medicine Ordering System

SE2030 Software Engineering group project. MediSys is a Java web app where customers order
medicines online. Prescription-only medicines are checked by a senior pharmacist before
the order is released.

> **Status:** modules 01 (cart and wishlist), 03 (catalog and inventory) and
> 05 (prescription upload and verification) are done, and login and notifications work. The other classes and pages are stubs with their owner and a TODO list.
> See [Module status](#module-status).

## Tech stack

| Part | Choice |
|------|--------|
| Language | Java 17 (any newer JDK also works) |
| Web | Jakarta Servlets + JSP (`jakarta.servlet`, **not** `javax`) |
| Server | Apache Tomcat 11 |
| Database | Microsoft SQL Server, plain JDBC (`mssql-jdbc` driver) |
| Build | Maven (`pom.xml`). IntelliJ has Maven built in. |
| UI | JSP pages (HTML) + one shared CSS file + plain JavaScript. No frameworks. |
| File uploads | `FileStorage` interface. Now: a local folder. Later: a cloud storage (see below). |

Design patterns used:
- **Singleton**: `DBConnection` (which also holds the connection pool) and the storage in `StorageFactory`
- **DAO**: a `dao` interface plus a `dao/impl` JDBC class for every table group
- **Strategy + Factory**: `FileStorage` with `LocalFileStorage`, chosen by `StorageFactory`
  from `app.properties`

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
   *Optional:* copy `app.properties.example` to `app.properties` to change where uploaded
   files are kept. The default folder is `<your home folder>/medisys-uploads`.
4. Add a **Tomcat Server → Local** run configuration (Tomcat 11). On the Deployment tab,
   add the artifact `medisys:war exploded` and set the context path to `/medisys`.
5. Run it and open <http://localhost:8080/medisys/>.

From the command line: `mvn package` builds `target/medisys.war`.

### Demo accounts (from `sample-data.sql`)

| Role | Email | Password |
|------|-------|----------|
| Customer | nimal@example.com | Customer@123 |
| Customer | kasuni@example.com | Customer@123 |
| Admin | admin@medisys.lk | Admin@123 |
| Senior Pharmacist | pharmacist@medisys.lk | Pharma@123 |
| Delivery Staff | delivery@medisys.lk | Delivery@123 |

To make a hash for a new password: `java -cp target/classes com.medisys.util.PasswordUtil MyPassword`.

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
    │   ├── storage/               FileStorage, LocalFileStorage, StorageFactory (shared)
    │   ├── filter/                AuthFilter: login + role checks (shared)
    │   └── util/                  SessionUtil, TextUtil, PasswordUtil, AppInitListener
    ├── resources/                 db.properties, app.properties (your own, not in git)
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
| 01 | `CartItem`, `Cart`, `WishlistItem` | `CartDAO`, `WishlistDAO` | `CartService`, `WishlistService` | `cart/` |
| 02 | `Order`, `OrderItem`, `OrderStatus`, `Payment` | `OrderDAO`, `PaymentDAO` | `OrderService`, `PaymentService` | `order/` |
| 03 | `Medicine`, `Category`, `InventorySummary` | `MedicineDAO`, `CategoryDAO` | `MedicineService` | `medicine/` |
| 04 | `User`, `Role` | `UserDAO` | `UserService` (+ `util/PasswordUtil`) | `user/` |
| 05 | `Prescription`, `PrescriptionItem`, `PrescriptionStatus` | `PrescriptionDAO` | `PrescriptionService` (+ `PrescriptionRequiredException`) | `prescription/` |
| 06 | `Delivery`, `DeliveryStatus`, `Notification` | `DeliveryDAO`, `NotificationDAO` | `DeliveryService`, `NotificationService` | `delivery/` |

**Shared files (agree with the team before changing them):** `pom.xml`, `web.xml`,
`DBConnection`, `SessionUtil`, `TextUtil`, `AppInitListener`, `ValidationException`,
`AuthFilter`, `JsonUtil`, `storage/*`, `common/*.jspf`, `css/style.css`, `js/app.js`, `database/*.sql`.

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
| 05 | `/prescriptions`, `/prescriptions/upload`, `/prescriptions/view?id=`, `/prescriptions/pay?id=`, `/prescriptions/correct`, `/prescriptions/delete` | customer |
| 05 | `/prescriptions/file?id=` | the customer who uploaded it, or a pharmacist |
| 05 | `/pharmacist/dashboard`, `/pharmacist/review`, `/pharmacist/delete` | pharmacist |
| 06 | `/deliveries/track?orderId=` | customer |
| 06 | `/staff/deliveries`, `/staff/deliveries/update` | delivery staff / admin |
| 06 | `/notifications` | logged in |

`AuthFilter` protects `/admin/*` for ADMIN, `/pharmacist/*` for PHARMACIST and `/staff/*`
for DELIVERY_STAFF.

### How the modules connect

- **01 → 05:** a prescription-only medicine never goes in the cart. The customer is sent
  to `/prescriptions/upload`, and pays for the medicines the pharmacist lists.
- **01 → 02:** checkout turns the cart into an order.
- **02 → 03:** placing an order reduces stock through `MedicineService`.
- **02 → 06:** a successful payment creates a delivery through `DeliveryService`.
- **05, 02, 06 → 06:** anything that needs to tell a user something calls
  `NotificationService.notify(...)`.

## Module status

| # | Module | Status |
|---|--------|--------|
| 01 | Shopping Cart and Wishlist | **done** (see below) |
| 02 | Order Placement and Checkout | not started |
| 03 | Medicine Catalog and Inventory | **done** (see below) |
| 04 | User and Role Management | login / logout / roles done early; register, profile, manage users still to do |
| 05 | Prescription Upload and Verification | **done** (see below) |
| 06 | Delivery Tracking and Notification | notifications done early (needed by 05); delivery tracking still to do |

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

The admin pages need an ADMIN login (`AuthFilter`).

### Module 01: Shopping Cart and Wishlist

**Pages** (customer login required; guests are sent to login and brought back afterwards)
- `/cart`: cart lines with a - / + quantity stepper, line totals, Save for later, Remove,
  Empty cart, and an order summary with Proceed to checkout. Lines that can't be bought
  right now (out of stock, more than the stock, discontinued, prescription not approved)
  are highlighted, and checkout is blocked until they are fixed.
- `/wishlist`: saved medicines with Move to cart and Remove.
- On the catalog and details pages: Add to Cart, a heart (save / unsave), "N in your cart",
  and menu badges for the cart and wishlist counts.

**Smooth updates:** every button is a normal form, so it works without JavaScript.
`js/cart.js` sends those forms in the background and updates the page in place (toast
messages, badges, totals). The servlets answer with JSON when the request asks for it
(`JsonUtil`, `CartReply`).

**Rules (all in `CartService` / `WishlistService`)**
- only medicines on sale can be added (not discontinued / expired / out of stock)
- quantity per medicine: 1 to min(stock, 10); adding again raises the quantity (capped)
- prescription-only medicines need an APPROVED prescription:
  `PrescriptionService.hasApprovedPrescription()`. Until module 05 is built it always
  returns false, so those medicines show "Upload prescription" instead of Add to Cart.
- the cart is checked again each time it is shown, because prices and stock can change
- a customer can only see or change their own cart (every query uses the logged-in user id)

**For other modules**
- 02 (checkout): `cartService.getCart(userId)` gives the lines and the subtotal; check
  `cart.isReadyForCheckout()` first. After the order, call `cartService.clearCart(userId)`.
- 05 (prescriptions): prescription-only medicines are refused with
  `PrescriptionRequiredException`, which sends the customer to the upload page.

**Performance:** `DBConnection` keeps a pool of open connections (Tomcat's built-in DBCP),
because opening a SQL Server connection takes about 250 ms.

### Module 05: Prescription Upload and Verification

Customers can't always read a doctor's handwriting, so **they only upload the
prescription**. The senior pharmacist reads it and writes down the medicines, and the
customer pays for that list.

```
 customer uploads file ──> PENDING ──pharmacist lists medicines + approves──> APPROVED ──customer pays──> PAID
                             │  ▲                                                          (stock taken out)
             reject / ask    │  │  customer sends a new copy
             for correction  ▼  │
                REJECTED / CORRECTION_REQUESTED
```

**Customer pages**
- `/prescriptions/upload`: drag and drop or browse for a JPG, PNG or PDF (with a
  preview), add an optional note, and tick "issued to me". Prescription-only medicines in
  the catalog link here instead of showing Add to Cart.
- `/prescriptions`: every prescription with a progress bar (Uploaded, Pharmacist check,
  Medicines listed, Paid) and the right button: View medicines & pay, View receipt,
  Send corrected copy, Upload a new prescription, View file, or Delete.
- `/prescriptions/view?id=`: once approved, the medicines with **how to use each one**,
  the quantities, prices and total, the pharmacist's note, and the **Pay** button.
  After payment the same page is the receipt (reference, amount, card ending, delivery
  address, and a Print button).
- `/prescriptions/pay?id=`: the test payment page. Delivery name, address and phone are
  filled in from the account, plus the card form. The order summary is shown alongside.
- `/prescriptions/correct?id=`: shows the pharmacist's note and takes the new copy.
- `/notifications`: messages about each decision and the payment.

**Senior pharmacist pages**
- `/pharmacist/dashboard`: totals, and tabs for waiting (oldest first), correction
  requested, approved but not paid, paid, rejected, expired, and all.
- `/pharmacist/review?id=`: the image or PDF next to the customer details, and a
  **medicine lines editor**. Each line has a medicine (from the catalog, grouped by
  category, out-of-stock ones disabled), a quantity, and "how to use" (with
  suggestions). Lines can be added and removed, and the line totals and grand total
  update as you type. Approve only unlocks when the 5 checks are ticked. Reject and
  Request correction need a note.
- Delete an invalid or expired prescription from the dashboard or the review page.

**Rules (all in `PrescriptionService`)**
- JPG, PNG or PDF only, at most 5 MB. The type is read from the file's first bytes
  ("magic numbers"), so a renamed `.exe` or `.txt` is refused.
- The file is stored under a new random name (`prescriptions/<uuid>.png`). The
  customer's file name is only displayed, after removing any folder parts.
- A customer can have at most 5 prescriptions waiting at the same time.
- A decision can only be made while the prescription is PENDING, and two pharmacists
  can't both decide.
- **Approve** needs at least one medicine line. Each line needs a medicine that is on
  sale, a quantity from 1 to 100 that isn't more than the stock, and "how to use" text
  (3 to 300 characters). The same medicine can't appear twice, and there are at most 20
  lines. The price is copied onto the line at approval time.
- Reject and Correction need a note of at least 5 characters. An expired prescription
  can't be approved.
- **Pay:** only an approved, unpaid, not expired prescription, by its own customer.
  Delivery details and the card format are checked (`PaymentService`). Payment runs as
  **one database transaction**: every medicine's stock is reduced, or nothing happens
  if one is short, and "paid" is recorded. A prescription can't be paid twice. Only the
  card's last 4 digits are stored.
- **Expired:** uploaded more than 30 days ago and not paid. A corrected copy starts the
  30 days again.
- A paid prescription can never be deleted. Deleting also removes the stored file, and
  the customer is notified.
- The cart never accepts a prescription-only medicine. It sends the customer to the
  upload page instead.

**Who can open a file:** `/prescriptions/file` sends the file only to the customer who
uploaded it, or to a pharmacist. Anyone else gets 404. Files are never inside the web
folder, so they can't be opened by typing their address.

**Demo data:** `sample-data.sql` adds six prescriptions: pending, correction requested,
rejected, approved and waiting for payment (Nimal: Metformin + Panadol), expired, and
paid (Kasuni: Losartan). Their files are in `src/main/webapp/WEB-INF/sample-uploads`, and
`AppInitListener` copies them into the storage when the app starts.

**Test card:** `4242 4242 4242 4242`, any future expiry date (MM/YY), and any 3-digit CVV.

**For other modules**
- 02 (checkout): `PaymentService.checkTestCard()` can be reused for the cart checkout.
  The prescription payment is kept separate from cart orders.
- 06 (delivery): a paid prescription has `delivery_name`, `delivery_address` and
  `delivery_phone`. Create the delivery at the `TODO (module 06)` in
  `PrescriptionService.pay()`.

### File storage and moving to the cloud

Uploaded files go through the `FileStorage` interface (`com.medisys.storage`).
`app.properties` chooses the implementation:

```
storage.type=local          # the only one for now
storage.local.dir=          # empty = <home folder>/medisys-uploads
```

To use a cloud storage later (Cloudinary, Supabase Storage, ...):
1. Write `CloudinaryFileStorage implements FileStorage` (save / put / exists / open / delete).
2. Add a `case "cloudinary"` in `StorageFactory` and put the keys in `app.properties`
   (never in git).
3. Keep sending files through `PrescriptionFileServlet`, or use short-lived signed URLs.
   Prescriptions are private medical data, so they must not be public links.

**Hosting note:** Vercel can't run a Java/Tomcat app. It hosts static sites and
serverless functions for Node, Python and similar. A Tomcat WAR needs a host such as
Render, Railway or Fly.io (with a Dockerfile), or Azure App Service. Supabase's database
is PostgreSQL, while `schema.sql` is written for SQL Server, so moving to Supabase would
mean converting the SQL (for example `IDENTITY` to `GENERATED ... AS IDENTITY`, `TOP` to
`LIMIT`, `SYSDATETIME()` to `now()`).

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
