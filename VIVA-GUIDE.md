# MediSys team guide (viva)

Where every **Create / Read / Update / Delete** is, where the **validation** is, which
**design patterns** are used, which **files each member owns**, and what each member must be
able to explain. Find your module, open the files it names, and practise the demo and the
viva questions.

**Live system:** see the link at the top of `README.md`. **Run it yourself:** `SETUP.md`.

> The spec says: *"Any work that cannot be adequately explained by the student will be
> treated as unauthorised assistance."* Know your code.

**IntelliJ tips:** `Ctrl+Shift+N` opens a file by name (type `CartDAO`). `Ctrl+F12` lists
the methods of the open file, so you can jump straight to, for example, `addToCart`.

---

## 1. How every module is built

Every module is **one folder**: `src/main/java/com/medisys/<module>/`. Its pages are in
`src/main/webapp/WEB-INF/views/<module>/`, with the same folder name.

```
 JSP form ──POST──> AuthFilter ──> Servlet ──────────────> DAO ─────────────> PostgreSQL (Supabase)
 (views/)           login + role    1. reads the form       the SQL, with
                    check           2. VALIDATES it         PreparedStatement
                                    3. calls the DAO
 JSP page <─forward── Servlet <──────────────── returns objects (Medicine, Order ...)
```

Each folder has three kinds of class:

| Kind | Example | What it does |
|------|---------|--------------|
| **Model** | `Medicine.java` | One row of a table as a Java object (fields + getters/setters) |
| **DAO** | `MedicineDAO.java` | **All the SQL** for its tables. Methods are grouped under `// ===== CREATE`, `READ`, `UPDATE`, `DELETE` headings |
| **Servlet** | `MedicineServlet.java` | Handles the URLs. The comment at the top lists every URL and whether it is Create, Read, Update or Delete. Validation is in a `validate...()` method or a `// ---- validation` block |

**Where to look when the examiner asks...**

| "Show me..." | Open |
|--------------|------|
| ...your CRUD (the SQL) | your module's `...DAO.java`, the `CREATE` / `READ` / `UPDATE` / `DELETE` sections |
| ...where the form is handled | your module's `...Servlet.java` (`doGet` = show a page, `doPost` = change data) |
| ...your validation | the servlet's `validate...()` method / `// ---- validation` block; shared checks (name, phone, password, NIC, card) are in `common/Validator.java` |
| ...the database connection | `common/DBConnection.java` (Singleton + connection pool) |
| ...login and role checks | `common/AuthFilter.java` (every protected URL) and `user/LoginServlet.java` |
| ...the tables | `database/schema.sql`, your module's section |
| ...the page | `WEB-INF/views/<module>/...jsp` |

### Design patterns (the spec asks for at least 2)

| Pattern | Where (open this file) | Why it is used |
|---------|------------------------|----------------|
| **MVC** | Model = `medicine/Medicine.java` etc., View = `WEB-INF/views/**.jsp`, Controller = `...Servlet.java` | Each kind of code has one job: servlets decide, JSPs only show, models carry data. JSPs are in `WEB-INF`, so the browser can only reach them through their servlet. |
| **DAO** | every `...DAO.java` (e.g. `medicine/MedicineDAO.java`) | All SQL for a table group is in one class, under `// ===== CREATE / READ / UPDATE / DELETE` headings. Servlets never write SQL. We proved its value: moving from SQL Server to PostgreSQL changed only the DAOs. |
| **Singleton** | `common/DBConnection.java` | Private constructor + `getInstance()`: one shared **connection pool** (Tomcat DBCP) for the whole app, because opening a cloud database connection takes a few hundred ms. |
| **Strategy** | `common/StorageStrategy.java` (interface), `CloudinaryStorage.java`, `LocalStorage.java` | Where uploaded files live is swappable: Cloudinary in the live system, a local folder offline. `FileStorage.strategy()` picks one at startup from the settings; no servlet changes. |
| **Facade** | `common/FileStorage.java` | Servlets call simple `FileStorage.save()/open()/delete()`; the facade checks the key and hides which strategy does the work. |
| **Intercepting Filter** | `common/AuthFilter.java` | Every protected URL passes this filter first: login check + role check in one place instead of in every servlet. |
| **Post / Redirect / Get** | every `doPost()` ends with `response.sendRedirect(...)` | Refreshing a page never submits a form twice; `SessionUtil.flash()` carries the message. |

Say at least **Singleton + DAO + Strategy** in the viva and open the files while you explain.

### Where things live in the cloud

| What | Service | Where in the code |
|------|---------|-------------------|
| The database (all tables) | **Supabase** (PostgreSQL) | `common/DBConnection.java`, settings `DB_URL`, `DB_USER`, `DB_PASSWORD` |
| Uploaded files: prescriptions, profile photos (private), product photos (public CDN) | **Cloudinary** | `common/CloudinaryStorage.java`, setting `CLOUDINARY_URL` |
| The Java app (Tomcat) | **Render** (Docker) | `Dockerfile`, `render.yaml` |
| The `*.vercel.app` address | **Vercel** (forwards every request to Render) | `vercel/vercel.json` |

Settings are read by `common/AppConfig.java`: environment variables on the server,
`db.properties` / `app.properties` on a laptop (both git-ignored, never pushed).

### Who owns which files

| # | Module | Owner | Java (`src/main/java/com/medisys/...`) | Pages (`src/main/webapp/WEB-INF/views/...`) + JS |
|---|--------|-------|-----------------------------------------|---------------------------------------------------|
| 01 | Shopping Cart and Wishlist | Amadini G. G. A. | `cart/` Cart, CartItem, **CartDAO**, CartServlet, WishlistItem, **WishlistDAO**, WishlistServlet | `cart/cart.jsp`, `cart/wishlist.jsp`, `common/heart-button.jspf`, `js/cart.js` |
| 02 | Order Placement and Checkout | Hewage B. H. A. S. | `order/` Order, OrderItem, OrderStatus, OrderStatusChange, Payment, **OrderDAO**, CheckoutServlet, OrderServlet, ManageOrdersServlet | `order/*.jsp`, `order/order-parts.jspf`, `common/checkout-fields.jspf` |
| 03 | Medicine Catalog and Inventory | Divisekara A. W. D. M. D. M. B. | `medicine/` Medicine, Category, InventorySummary, **MedicineDAO**, **CategoryDAO**, CatalogServlet, MedicineServlet, CategoryServlet | `medicine/*.jsp`, `js/medicine.js` |
| 04 | Reports and Analytics | Kaweesha P. M. G. S. | `report/` Report, ReportPeriod, ReportRow, ReportSummary, SavedReport, **ReportDAO**, **SavedReportDAO**, ReportServlet, SavedReportServlet | `report/*.jsp` |
| 05 | Prescription Upload and Verification | Perera D. A. A. N. S. | `prescription/` Prescription, PrescriptionItem, PrescriptionStatus, **PrescriptionDAO**, PrescriptionServlet, PharmacistServlet, PrescriptionPaymentServlet | `prescription/*.jsp`, `prescription/*.jspf`, `js/prescription.js` |
| 06 | Delivery Tracking and Notification | Deshabhi R. G. S. | `delivery/` Delivery, DeliveryStatus, DeliveryUpdate, Notification, **DeliveryDAO**, **NotificationDAO**, DeliveryServlet, TrackDeliveryServlet, NotificationServlet | `delivery/*.jsp`, `delivery/delivery-parts.jspf` |
| - | Minor functions (users, login) | Whole team | `user/` User, Role, **UserDAO**, LoginServlet, RegisterServlet, ProfileServlet, ForgotPasswordServlet, ManageUsersServlet | `user/*.jsp` |
| - | Shared | Whole team | `common/` (everything) | `common/header.jspf`, `footer.jspf`, `home.jsp`, `error.jsp`, `css/style.css`, `js/app.js` |

Your **tables** are in your module's section of `database/schema.sql`, and your demo rows in
the same section of `database/sample-data.sql`.

---

## 2. Module 01: Shopping Cart and Wishlist

**Owner:** Amadini G. G. A. **Folder:** `cart/`, `views/cart/`, `js/cart.js`
**Tables:** `cart_items`, `wishlist_items`

| | What the user does | URL | Servlet method | DAO method |
|---|---|---|---|---|
| **C** | Add to cart | `POST /cart/add` | `CartServlet.addToCart()` | `CartDAO.addItem()` |
| **C** | Save to wishlist (heart) | `POST /wishlist/action` (`add` / `toggle`) | `WishlistServlet.addToWishlist()` | `WishlistDAO.addItem()` |
| **R** | Cart page | `GET /cart` | `CartServlet.doGet()` | `CartDAO.getCart()` |
| **R** | Wishlist page | `GET /wishlist` | `WishlistServlet.doGet()` | `WishlistDAO.getWishlist()` |
| **U** | Change the quantity (- / +) | `POST /cart/update` | `CartServlet.updateQuantity()` | `CartDAO.updateQuantity()` |
| **D** | Remove a line / empty the cart | `POST /cart/remove` | `CartServlet.removeItem()` | `CartDAO.removeItem()`, `clearCart()` |
| **D** | Remove from wishlist | `POST /wishlist/action` (`remove`) | `WishlistServlet.doPost()` | `WishlistDAO.removeItem()` |

Also: **Move to cart** and **Save for later** (`WishlistServlet.doPost`, actions `move` /
`save-for-later`).

**Validation** (`CartServlet.addToCart()` and `updateQuantity()`)
- quantity 1 to min(stock, 10) (`Cart.MAX_PER_ITEM`); adding again raises it, capped
- the medicine must be on sale: not discontinued, not expired, in stock
- a prescription-only medicine is refused with `PrescriptionNeededException`, which sends
  the customer to `/prescriptions/upload` (module 05)
- `CartItem.getProblem()` checks every line again each time the cart is shown, because the
  price or stock may have changed; `Cart.isReadyForCheckout()` blocks checkout until fixed

**Know this**
- `cart_items` has `UNIQUE (user_id, medicine_id)`. If two clicks arrive at the same moment,
  PostgreSQL error `23505` (unique_violation, `e.getSQLState()`) is caught and the quantity
  is raised instead.
- Every query uses the logged-in user's id, so nobody can see another customer's cart.
- Smooth updates: every button is a normal HTML form. `cart.js` sends it with `fetch()`, the
  servlet sees "wants JSON" (`JsonUtil.wantsJson`) and `CartServlet.reply()` answers in JSON.
- "Buy again" (module 02) and "Move to cart" both call `CartServlet.addToCart()`, so the
  rules are the same everywhere.

**Demo (2 min):** log in as `nimal@example.com` → Medicines → Add to Cart → heart a
medicine → Cart: + / -, Save for later, Remove → Wishlist: Move to cart. Try "Add" on
Amoxicillin (prescription only): you are sent to the upload page.

**Viva questions**
- *Why can't prescription medicines go in the cart?* A pharmacist must check the
  prescription first (module 05); the customer then pays for the pharmacist's list.
- *What stops quantity 50 when there are 8 in stock?* `addToCart()` allows 1..min(stock, 10),
  and checkout takes stock with `WHERE stock_quantity >= ?`.
- *What happens on a double click?* The UNIQUE constraint + catching SQLState 23505: the
  quantity goes up once.

---

## 3. Module 02: Order Placement and Checkout

**Owner:** Hewage B. H. A. S. **Folder:** `order/`, `views/order/`, `views/common/checkout-fields.jspf`
**Tables:** `orders`, `order_items`, `payments`, `order_status_history`

| | What the user does | URL | Servlet method | DAO method |
|---|---|---|---|---|
| **C** | Pay and place the order | `POST /checkout` | `CheckoutServlet.placeOrder()` | `OrderDAO.placeOrder()` (one transaction) |
| **R** | My orders | `GET /orders` | `OrderServlet.doGet()` | `OrderDAO.getOrdersByUser()` |
| **R** | One order (timeline) | `GET /orders/view?id=` | `OrderServlet.doGet()` | `OrderDAO.getOrderById()` |
| **R** | Admin: all orders, search, tabs | `GET /admin/orders` | `ManageOrdersServlet.doGet()` | `OrderDAO.getOrders()`, `countByStatus()` |
| **U** | Admin: mark as packed | `POST /admin/orders/status` (`advance`) | `ManageOrdersServlet.markPacked()` | `OrderDAO.moveToStatus()` |
| **U** | Cancel (customer) | `POST /orders/cancel` | `OrderServlet.cancel()` | `OrderDAO.cancelOrder()` |
| **U** | Cancel with a reason (admin) | `POST /admin/orders/status` (`cancel`) | `ManageOrdersServlet.cancel()` | `OrderDAO.cancelOrder()` |
| **D** | - | | | Orders are **never deleted** (they are the record of a sale). Cancelling is the "delete". |

Also: **Buy again**: `POST /orders/reorder`, `OrderServlet.reorder()`.

**Validation** (`CheckoutServlet.placeOrder()`)
- `expectedTotal`: the page sends the total it showed. If a price changed meanwhile,
  nothing is charged and the new total is shown.
- delivery name 2-100, address 5-255, phone 9-15 digits, note up to 300
- the test card: `Validator.card()` (Luhn check, MM/YY not in the past, 3-4 digit CVV)
- cancel: customer only while PAID; pharmacy while PAID or PROCESSING with a reason (5-300)

**Know this**
- Status flow (`OrderStatus`): PAID → PROCESSING (packed) → SHIPPED → DELIVERED, or
  CANCELLED. The admin only does PAID → PROCESSING; the rider (module 06) does the rest.
- Delivery Rs. 300, free from Rs. 2,500 (`Order.DELIVERY_FEE`, `Order.deliveryFeeFor()`).
- `OrderDAO.placeOrder()` is **one transaction** (`setAutoCommit(false)` ... `commit()` /
  `rollback()`): (1) lock the prescription if paying one, (2) take stock with
  `UPDATE ... WHERE stock_quantity >= ?` (0 rows → `StockShortageException`), (3) the order,
  (4) its lines, (5) the payment, (6) the first history row and the delivery (module 06),
  (7) link the prescription or remove the bought lines from the cart. If anything fails,
  nothing is saved.
- `cancelOrder()` is one transaction too: stock back, payment REFUNDED, prescription freed,
  delivery cancelled.
- Only the last 4 card digits are stored. Name and price are **copied** into
  `order_items`, so old orders stay correct when the catalog changes.
- Someone else's order gives 404 (`OrderServlet.findOwnOrder()`).

**Demo (2 min):** nimal → Cart → Proceed to checkout → card `4242 4242 4242 4242`, any
future MM/YY, CVV 123 → thank-you page → Orders → Cancel. Admin → Orders → Mark: Being packed.

**Viva questions**
- *What is a transaction and where do you use one?* Several SQL statements that succeed or
  fail together: `placeOrder()` and `cancelOrder()` in `OrderDAO`.
- *Two people order the last box at the same moment?* The conditional
  `UPDATE ... AND stock_quantity >= ?`: the second one changes 0 rows → rollback → message.
- *Why are orders never deleted?* They are financial records; a cancelled order keeps its
  history and refund.

---

## 4. Module 03: Medicine Catalog and Inventory

**Owner:** Divisekara A. W. D. M. D. M. B. **Folder:** `medicine/`, `views/medicine/`, `js/medicine.js`
**Tables:** `medicines`, `categories`

| | What the user does | URL | Servlet method | DAO method |
|---|---|---|---|---|
| **C** | Add a medicine | `POST /admin/medicines/edit` (id = 0) | `MedicineServlet.saveMedicine()` | `MedicineDAO.addMedicine()` |
| **C** | Add a category | `POST /admin/categories` (`add`) | `CategoryServlet.addCategory()` | `CategoryDAO.addCategory()` |
| **R** | Catalog + search + category filter | `GET /medicines` | `CatalogServlet.showCatalog()` | `MedicineDAO.searchMedicines()` |
| **R** | Medicine details | `GET /medicines/view?id=` | `CatalogServlet.showMedicine()` | `MedicineDAO.getCatalogMedicine()` |
| **R** | Inventory (tabs + totals) | `GET /admin/medicines` | `MedicineServlet.showInventory()` | `searchMedicines()`, `getSummary()` |
| **R** | Categories | `GET /admin/categories` | `CategoryServlet.doGet()` | `CategoryDAO.getAllCategories()` |
| **R** | Product photo | `GET /medicines/image?id=` (or the Cloudinary CDN link) | `CatalogServlet.sendImage()` | `MedicineDAO.getMedicineById()` |
| **U** | Edit a medicine | `POST /admin/medicines/edit` (id > 0) | `MedicineServlet.saveMedicine()` | `MedicineDAO.updateMedicine()` |
| **U** | Upload / remove the product photo | `POST /admin/medicines/edit` (`image`, `removeImage`) | `MedicineServlet.saveMedicine()` | `MedicineDAO.updateImage()` |
| **U** | Rename a category | `POST /admin/categories` (`update`) | `CategoryServlet.updateCategory()` | `CategoryDAO.updateCategory()` |
| **U** | Add stock | `POST /admin/medicines/restock` | `MedicineServlet.restock()` | `MedicineDAO.addStock()` |
| **D** | Discontinue (soft delete) / Restore | `POST /admin/medicines/discontinue` | `MedicineServlet.discontinueOrRestore()` | `discontinueMedicine()`, `restoreMedicine()` |
| **D** | Delete a category | `POST /admin/categories` (`delete`) | `CategoryServlet.deleteCategory()` | `CategoryDAO.deleteCategory()` |

**Validation** (`MedicineServlet.validate()`, repeated in `medicine.js` for quick feedback)
- name 2-150; category and dosage form must be from the lists
- price > 0, at most Rs. 1,000,000, at most 2 decimals
- stock 0-100000, reorder level 0-10000, restock 1-10000
- expiry date not in the past (an already saved past date may stay when editing)
- no two medicines with the same name **and** strength (`MedicineDAO.medicineExists()`)
- category name 2-100 and unique (ignoring capitals); a category can only be deleted when no
  medicine uses it
- product photo: JPG or PNG only (real type from the first bytes), at most 2 MB

**Know this**
- **Soft delete:** a medicine is never really deleted because old orders and prescriptions
  point to it. `is_discontinued = TRUE` hides it from the catalog.
- The catalog hides discontinued and expired medicines; opening one directly gives 404.
- Low stock = stock <= reorder level.
- Search is safe from SQL injection: `PreparedStatement` with `?`. It uses `ILIKE` (ignores
  capitals) and `TextUtil.likePattern()` escapes the LIKE special characters `\ % _`.
- **Product photos live in Cloudinary** as public images; the catalog links straight to
  Cloudinary's CDN, which resizes them and sends WebP/AVIF (`Medicine.getImageUrl()` →
  `CloudinaryStorage.publicUrl()`). The table only stores the key (`image_key`).

**Demo (2 min):** as a guest: Medicines → search "pan" → category filter → open one.
Admin → Inventory: tabs, add a medicine with a wrong price (see the errors), restock
Brufen, discontinue + restore. Categories: add one, try to delete a used one.

**Viva questions**
- *Why soft delete?* Orders and prescriptions reference the medicine (foreign keys).
- *Where is validation done, browser or server?* Both; the server (`validate()`) is the one
  that counts. JavaScript is only for quick feedback.

---

## 5. Module 04: Reports and Analytics

**Owner:** Kaweesha P. M. G. S. **Folder:** `report/`, `views/report/`
**Tables:** `saved_reports`. The reports **read** the other modules' tables; nothing is copied.

| | What the user does | URL | Servlet method | DAO method |
|---|---|---|---|---|
| **C** | Save this report | `POST /admin/reports/saved` (`create`) | `SavedReportServlet.create()` | `SavedReportDAO.addSavedReport()` |
| **R** | Reports dashboard | `GET /admin/reports?range=30` | `ReportServlet.buildReport()` | `ReportDAO.getSummary()`, `getDailySales()`, `getTopMedicines()`, ... |
| **R** | Download CSV | `GET /admin/reports/export?type=` | `ReportServlet.exportCsv()` | same as above |
| **R** | Saved reports / one saved report | `GET /admin/reports/saved`, `/saved/view?id=` | `SavedReportServlet.doGet()` | `getAllSavedReports()`, `getSavedReportById()` |
| **U** | Edit title and notes | `POST /admin/reports/saved` (`update`) | `SavedReportServlet.doPost()` | `SavedReportDAO.updateSavedReport()` |
| **D** | Delete a saved report | `POST /admin/reports/saved` (`delete`) | `SavedReportServlet.doPost()` | `SavedReportDAO.deleteSavedReport()` |

**Validation**
- `ReportServlet.resolvePeriod()`: custom dates must be real dates, from <= to, to not in
  the future, from 2020 or later, at most 366 days. Bad dates → message + the last 30 days.
- `SavedReportServlet.checkTitle()` (3-100 characters) and `checkNotes()` (up to 500).

**Know this**
- The adding up is done in SQL (`ReportDAO`): `COUNT`, `SUM`, `AVG`, `GROUP BY`,
  `CASE WHEN` inside `SUM`, sub-queries, `LIMIT ?`, `EXTRACT(EPOCH FROM ...)` for hours.
- Dates: `created_at >= from AND created_at < the day after to`, so the whole last day is
  included whatever the time.
- "Sales" = orders not cancelled. Approval rate = approved / (approved + rejected).
- `fillMissingDays()`: the database only returns days with sales; Java adds the empty days.
- Charts are plain HTML + CSS bars (no library), with keyboard tooltips and a table view.
- CSV: values quoted, UTF-8 mark for Excel, and **CSV injection** protection (a value
  starting with `= + - @` gets a `'` in front).
- A saved report keeps the numbers of the day it was saved; its page shows "when saved"
  next to "today".

**Demo (3 min):** admin → Reports → 7 / 30 / 90 days → hover the bars → Show as table →
Download CSV → Save this report → place an order as nimal → open the saved report again.

**Viva questions**
- *Why calculate in SQL and not in Java?* The database is built for it, and only the totals
  travel over the network.
- *Why "< the day after" instead of "<= to"?* `created_at` has a time; `<= 2026-09-17` would
  miss 17 Sep 10:00.

---

## 6. Module 05: Prescription Upload and Verification

**Owner:** Perera D. A. A. N. S. **Folder:** `prescription/`, `views/prescription/`, `js/prescription.js`
**Tables:** `prescriptions`, `prescription_items`

| | What the user does | URL | Servlet method | DAO method |
|---|---|---|---|---|
| **C** | Upload a prescription | `POST /prescriptions/upload` | `PrescriptionServlet.upload()` | `PrescriptionDAO.addPrescription()` |
| **R** | My prescriptions | `GET /prescriptions` | `PrescriptionServlet.doGet()` | `getPrescriptionsByUser()` |
| **R** | One prescription (medicines, how to use) | `GET /prescriptions/view?id=` | `PrescriptionServlet.doGet()` | `getPrescriptionById()` |
| **R** | Pharmacist dashboard (by status) | `GET /pharmacist/dashboard` | `PharmacistServlet.doGet()` | `getPrescriptions()`, `countForDashboard()` |
| **R** | Review page + the uploaded file | `GET /pharmacist/review?id=`, `/prescriptions/file?id=` | `PharmacistServlet.doGet()`, `PrescriptionServlet.sendFile()` | `getPrescriptionById()` |
| **U** | Approve (list medicines + how to use) | `POST /pharmacist/review` (`APPROVE`) | `PharmacistServlet.review()` | `approvePrescription()` (one transaction) |
| **U** | Reject / ask for a correction | `POST /pharmacist/review` (`REJECT` / `CORRECTION`) | `PharmacistServlet.review()` | `rejectOrAskCorrection()` |
| **U** | Send a clearer copy | `POST /prescriptions/correct` | `PrescriptionServlet.uploadCorrection()` | `replaceFile()` |
| **D** | Customer deletes an unpaid one | `POST /prescriptions/delete` | `PrescriptionServlet.delete()` | `deletePrescription()` |
| **D** | Pharmacist deletes an invalid / expired one | `POST /pharmacist/delete` | `PharmacistServlet.delete()` | `deletePrescription()` |

Also: **Pay**: `POST /prescriptions/pay`, `PrescriptionPaymentServlet` → `CheckoutServlet.placeOrder()`
(module 02) → `OrderDAO.placeOrder()`.

**Validation**
- `PrescriptionServlet.checkFile()`: JPG, PNG or PDF only, at most 5 MB. The **real type**
  comes from the first bytes (`FileStorage.detectType()`), not from the file name.
- `upload()`: note up to 500, at most 5 prescriptions waiting per customer, "issued to me" ticked.
- `PharmacistServlet.review()` + `readItems()`: decision only while PENDING; approve needs
  1-20 lines, each a medicine on sale, quantity 1-100 and not more than the stock, "how to
  use" 3-300, no medicine twice; reject / correction need a note (at least 5); an expired
  prescription (over 30 days, unpaid) can't be approved.

**Know this**
- Status (`PrescriptionStatus`): PENDING → APPROVED / REJECTED / CORRECTION_REQUESTED →
  (new copy) → PENDING. Paid = APPROVED + `order_id` set.
- Only registered customers can upload (`AuthFilter` sends guests to log in / register).
- The stored file gets a random name (UUID), so a name like `..\..\x` can't be used. The
  file is kept in **Cloudinary as a private ("authenticated") file**: it has no public
  address. `/prescriptions/file` downloads it with a link signed by our secret key and sends
  it only to the owner or a pharmacist; anyone else gets 404.
- Two pharmacists can't both decide: the UPDATE has `WHERE status = 'PENDING'`.
- A paid prescription can never be deleted (`deletePrescription()` has `AND order_id IS NULL`).

**Demo (3 min):** nimal → Upload prescription (try a .txt: refused) → pharmacist →
dashboard → review: Request correction → nimal sends a new copy → pharmacist lists 2
medicines with how to use → Approve → nimal → Pay → an order is created.

**Viva questions**
- *How do you know a file is really a PNG?* `detectType()` checks the first bytes
  (`89 50 4E 47 ...`), not the extension.
- *Can customer A open customer B's file?* No: the servlet checks ownership; 404 otherwise.
- *Why a random stored file name?* Path traversal protection and no name clashes.

---

## 7. Module 06: Delivery Tracking and Notification

**Owner:** Deshabhi R. G. S. **Folder:** `delivery/`, `views/delivery/`
**Tables:** `deliveries`, `delivery_updates`, `notifications`

| | What the user does | URL | Servlet method | DAO method |
|---|---|---|---|---|
| **C** | A delivery for every paid order (automatic) | (inside checkout) | `OrderDAO.placeOrder()` | `DeliveryDAO.addDeliveryForOrder()` |
| **C** | A notification (every module sends them) | - | e.g. `DeliveryServlet.pickUp()` | `NotificationDAO.addNotification()` |
| **R** | Rider's list (New / On the way / Completed) | `GET /staff/deliveries?tab=` | `DeliveryServlet.doGet()` | `getDeliveries()`, `countByStatus()` |
| **R** | One delivery | `GET /staff/deliveries/view?id=` | `DeliveryServlet.doGet()` | `getDeliveryById()` |
| **R** | Customer tracks an order | `GET /deliveries/track?orderId=` | `TrackDeliveryServlet.doGet()` | `getDeliveryByOrder()` |
| **R** | Notifications page | `GET /notifications` | `NotificationServlet.doGet()` | `getNotifications()` |
| **U** | "Got the package" (rider takes a new parcel) | `POST /staff/deliveries/update` (`pickup`) | `DeliveryServlet.pickUp()` | `DeliveryDAO.pickUp()` (one transaction) |
| **U** | Delivered / Could not deliver / Try again | `POST /staff/deliveries/update` (`status`) | `DeliveryServlet.updateStatus()` | `DeliveryDAO.updateStatus()` (one transaction) |
| **U** | Notifications marked read | `GET /notifications` | `NotificationServlet.doGet()` | `markAllRead()` |
| **D** | Delete a notification / clear read ones | `POST /notifications/delete` | `NotificationServlet.doPost()` | `deleteNotification()`, `deleteReadNotifications()` |

**Validation** (`DeliveryServlet.updateStatus()` and `pickUp()`)
- only riders change deliveries (the admin can only look)
- the next status must be an allowed step (`DeliveryStatus.nextSteps()`)
- "Could not deliver" needs a reason (at least 5 characters); notes up to 300
- the form sends the status it showed (`current`), so a double click can't skip a step

**Know this**
- Flow: PENDING ("New") → rider presses **Got the package** → OUT_FOR_DELIVERY →
  DELIVERED, or FAILED (reason) → try again. Cancelling the order (while PENDING) cancels it.
- The delivery is created **inside the order's transaction**, so a paid order can never be
  missing its delivery. It is due 2 days later (`DeliveryDAO.DELIVERY_DAYS`).
- `pickUp()` and `updateStatus()` change the delivery **and** the order (SHIPPED /
  DELIVERED) in one transaction, so they stay in step.
- If two riders press "Got the package" together, only one gets it
  (`WHERE status = 'PENDING' AND (staff_id IS NULL OR staff_id = ?)`).
- A rider sees their own deliveries plus new ones nobody has taken; others give 404.
  A customer tracks only their own orders.
- `NotificationDAO.addNotification()` is used by every module. A failed notification is only
  logged: it must not undo the action that caused it. Deleting uses
  `WHERE id = ? AND user_id = ?`, so nobody can delete someone else's.

**Demo (3 min):** log in as `delivery@medisys.lk` → New deliveries → Got the package on
ORD-000003 → On the way → Could not deliver (reason) → Try again → Delivered → log in as
nimal → Orders → Track, and the bell.

**Viva questions**
- *Why create the delivery in the order's transaction?* So there is never a paid order
  without a delivery.
- *How do the order and delivery statuses stay in step?* Both change in one transaction in
  `DeliveryDAO`.

---

## 8. Minor functions: user accounts and roles

**Folder:** `user/`, `views/user/` (+ `common/AuthFilter`, `common/PasswordUtil`, `common/Validator`)
**Table:** `users`. Everyone must be able to explain login, roles and the session.

| | What the user does | URL | Servlet method | DAO method |
|---|---|---|---|---|
| **C** | Register (customer) | `POST /register` | `RegisterServlet.validate()` + `doPost()` | `UserDAO.addUser()` |
| **C** | Admin adds a staff account | `POST /admin/users` (`create`) | `ManageUsersServlet.createStaff()` | `UserDAO.addUser()` |
| **R** | Log in | `POST /login` | `LoginServlet.doPost()` | `getUserByEmail()`, `getPasswordHash()` |
| **R** | Profile page | `GET /account/profile` | `ProfileServlet.doGet()` | `getUserById()` |
| **R** | Admin users page | `GET /admin/users` | `ManageUsersServlet.showPage()` | `getUsers()`, `countUsers()` |
| **U** | Edit my details | `POST /account/profile` (`details`) | `ProfileServlet.updateDetails()` | `updateContactDetails()` |
| **U** | Change / forgot password | `POST /account/profile` (`password`), `POST /forgot-password` | `ProfileServlet.changePassword()`, `ForgotPasswordServlet.doPost()` | `updatePassword()` |
| **U** | Profile photo | `POST /account/photo` | `ProfileServlet.changePhoto()` | `updatePhoto()` |
| **U** | Staff account on / off, reset staff password | `POST /admin/users` | `ManageUsersServlet.setActive()`, `resetStaffPassword()` | `setActive()`, `updatePassword()` |
| **U** | Red flag on a customer | `POST /users/flag` | `ManageUsersServlet.changeFlag()` | `flagCustomer()`, `removeFlag()` |
| **D** | Remove my photo | `POST /account/photo` (`remove`) | `ProfileServlet.changePhoto()` | `updatePhoto(id, null)` |

Users are never deleted, because their orders and prescriptions must be kept. Staff who
leave are switched off; customers who misbehave get a red flag (never a ban).

**Validation:** `RegisterServlet.validate()` (email and NIC unique; NIC old `9 digits + V/X`
or new 12 digits, and its birth year must match the date of birth; 18 or older); the shared
`Validator.name()`, `phone()`, `password()` (8+ with a letter and a number); photos JPG/PNG
up to 2 MB, real type checked.

**Know this**
- Roles (`Role`): CUSTOMER, PHARMACIST, ADMIN, DELIVERY_STAFF. `AuthFilter.allowedRoles()`
  decides who may open which path.
- Passwords: `PasswordUtil` = PBKDF2 (120,000 rounds) with a random salt; compared in
  constant time; never stored in plain text.
- Login gives the **same message** for a wrong email or a wrong password, and checks a
  dummy hash for unknown emails, so attackers can't find out which emails exist.
- Login makes a **new session** (`SessionUtil.login`): session fixation protection. The
  `User` in the session never holds the hash.
- `returnTo` only accepts paths inside the app (`TextUtil.isSafeLocalPath`): no open redirect.

---

## 9. Whole team: everyone must be able to explain this

**Shared files** (agree with the team before changing them): `pom.xml`, `web.xml`,
everything in `common/` (`AppConfig`, `DBConnection`, `AuthFilter`, `Validator`, `FileStorage`,
`StorageStrategy`, `CloudinaryStorage`, `LocalStorage`, `HomeServlet`, `SessionUtil`, `TextUtil`,
`JsonUtil`, `PasswordUtil`, `ValidationException`, `AppStartupListener`), `views/common/*`,
`css/style.css`, `js/app.js`, `database/*.sql`, `Dockerfile`, `render.yaml`, `vercel/`.

**Technologies:** Java 17+, Jakarta Servlets + JSP on Tomcat 11, PostgreSQL (Supabase) with
plain JDBC, Cloudinary (file storage + image CDN), Maven, Docker (Render), Vercel (the public
address), HTML + one CSS file + plain JavaScript. No frameworks (no Spring, no Hibernate).

**Security: "how do you protect against...?"**

| Threat | How |
|--------|-----|
| SQL injection | `PreparedStatement` with `?` everywhere, never string `+` |
| XSS | `TextUtil.html()` escapes every value printed in a JSP |
| Passwords | PBKDF2 + salt (`PasswordUtil`), never plain text |
| Access control | `AuthFilter` checks login + role; servlets check ownership (other people's data = 404) |
| Session fixation | new session id at login |
| Uploads | type from the first bytes, size limit, random names, files outside the web folder |
| Double submits | conditional updates: `WHERE status = ?` |
| Open redirect | only local `returnTo` paths accepted |
| CSV injection | `'` in front of `= + - @` in exports |
| Payment data | only the last 4 card digits are stored |
| Secrets | environment variables on the server; `db.properties` / `app.properties` are git-ignored |
| Supabase REST API | Row Level Security is on for every table (`schema.sql`, last section), so the public key can read nothing |
| Private files | prescriptions and profile photos are "authenticated" Cloudinary files: no public link exists |

**Ideas every member should be able to explain**
- GET shows a page, POST changes data. After a POST the servlet **redirects**
  (Post/Redirect/Get), so refreshing doesn't submit twice; a "flash" message
  (`SessionUtil.flash`) survives the redirect.
- `@WebServlet({"/cart", "/cart/add", ...})` maps a servlet to its URLs (no web.xml needed).
  One servlet handles all the URLs of a feature; `request.getServletPath()` tells which one.
- JSPs live in `WEB-INF`, so they can only be reached through their servlet.
- `ValidationException` carries all the error messages of a form, so the user sees every
  problem at once.
- Transactions: `setAutoCommit(false)` ... `commit()`, `rollback()` on any error.
- Foreign keys, `UNIQUE` and `CHECK` constraints in `schema.sql` protect the data even if the
  Java code had a bug.
- Validation happens on the server; HTML/JavaScript checks are only for comfort.

**Ethical considerations**
- Privacy: NIC / date of birth / phone only for pharmacy staff, not on the admin users page;
  prescription files only for the owner and pharmacists.
- Fairness: a red flag is a warning, not a ban; customers can still get their medicines.
- Safety: prescription medicines only after a pharmacist's approval.
- Accessibility: labels on every field, keyboard-usable charts and tooltips, table views,
  works on phones.
- Honesty: the payment page says clearly it is a TEST card.

**Before the viva: checklist for each member**
- [ ] I can run the project (SETUP.md) and do my demo without help.
- [ ] I can open my DAO and point to each CREATE / READ / UPDATE / DELETE method.
- [ ] I can show where my form is validated and say each rule.
- [ ] I can draw my request flow on paper (form → servlet → DAO → table → JSP).
- [ ] I can explain my tables and their relationships (`schema.sql`).
- [ ] I can answer my viva questions without looking.
- [ ] I can explain the design patterns and the security table above.
