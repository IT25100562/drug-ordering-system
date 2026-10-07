# Design Context

## 1. USE CASES
- **Customer** — register/sign in, browse medicines, manage cart and wishlist, place or cancel orders, upload and track prescriptions, track deliveries, manage profile, receive notifications.
- **Administrator** — manage staff and users, medicine catalog, categories and stock; manage orders, assign delivery staff, and oversee deliveries.
- **Pharmacist** — review uploaded prescriptions, approve/reject/request corrections, define approved medicine lines, and flag customers.
- **Delivery staff / rider** — view assigned deliveries, update delivery status and record failed-delivery reasons.
- **Guest** — browse the catalog and register or sign in to access customer actions.

## 2. CLASS DATA
- **User** — `id`, name, email, phone/WhatsApp, NIC/date of birth, address/photo key, role, active/flag state. Helpers: `getAge()`, `getFirstName()`, `isCustomer()` and role checks. A user has many orders, prescriptions, notifications and cart/wishlist lines; users may also reference a flagging user.
- **Medicine** — `id`, name, `categoryId`, manufacturer, dosage form/strength, description, price, stock/reorder level, prescription requirement, expiry and discontinued state. Helpers: `isAvailable()`, `isLowStock()`, `isExpired()`, `getDisplayName()`. Category 1:N medicines; medicines relate N:M to users through cart/wishlist lines and to orders/prescriptions through their line entities.
- **Cart / CartItem** — cart is a computed list of items; each line stores user, medicine, quantity and timestamps. `Cart`: `getSubtotal()`, `isReadyForCheckout()`, `getItemCount()`; `CartItem`: `getLineTotal()`, `hasProblem()`. Users and medicines are N:M through `cart_items` (unique user/medicine line); wishlist uses a parallel `wishlist_items` relation.
- **Order** — `id`, user, source/status, subtotal/fee/total, copied delivery details, cancellation reason and timestamps; aggregates items, payment and status history. Helpers: `getReference()`, `isFromPrescription()`, `getPackCount()`, `getTimeOf()`. User 1:N orders; order 1:N items/history, 1:1 payment and delivery; an order may be linked to a prescription.
- **OrderItem** — order/medicine IDs, copied medicine name/dosage form and unit price, quantity, optional dosage instructions. `getLineTotal()`. Order 1:N order items; each item references one medicine.
- **Payment** — order ID, amount, test-card method, last four digits, reference, status and paid/refunded timestamps. `isRefunded()`. Order 1:1 payment, enforced by a unique order constraint.
- **Prescription** — customer, file key/name/type/size, status and notes, reviewer, correction count, timestamps, optional linked order and approved medicine items. Helpers: `isExpired()`, `isPayable()`, `isDeletable()`, `getTotal()`. User 1:N prescriptions; prescription 1:N prescription items; each prescription item references one medicine; a paid prescription links to an order.
- **Delivery** — order, assigned staff, status, attempts, estimated/delivered dates and delivery updates. Helpers: `hasRider()`, `isLate()`, `getTimeOf()`. One delivery per order; a delivery may reference one staff user and has many status updates.

## 3. ACTIVITY FLOWS
1. **Customer registration** — Customer submits form -> `RegisterServlet` collects fields -> `UserService.register()` validates email/NIC uniqueness, age, contact details and password -> `PasswordUtil` hashes the password -> `UserDAO` inserts with a prepared statement and loads the new user -> servlet creates the session and redirects.
2. **Medicine/catalog and inventory CRUD** — Admin opens or submits the medicine form/restock action -> `MedicineFormServlet` or `RestockMedicineServlet` reads the request -> `MedicineService` validates fields, category, price, expiry and quantity -> `MedicineDAO`/`CategoryDAO` uses JDBC -> SQL Server inserts/updates medicines or categories; catalog and inventory pages read through the same service/DAOs.
3. **Cart and wishlist changes** — Customer posts add/update/remove/save-for-later -> cart or wishlist servlet -> `CartService`/`WishlistService` checks ownership, current availability, prescription restriction and quantity limits -> `CartDAO`/`WishlistDAO` writes the user-medicine line -> SQL Server enforces unique lines and foreign keys.
4. **Drug ordering and checkout** — Customer submits checkout -> `CheckoutServlet` reads delivery/card/expected-total fields -> `OrderService` rechecks cart readiness, totals and form/card shape -> `OrderDAOImpl` transaction conditionally reduces stock, inserts order/items/payment/history and delivery, then removes purchased cart lines -> commit all or rollback; servlet redirects to order details.
5. **Prescription upload and verification** — Customer uploads -> `UploadPrescriptionServlet` reads bounded multipart data -> `PrescriptionService` validates size and file signature, creates a generated storage key, and saves metadata -> `PrescriptionDAO` inserts pending record. Pharmacist submits review -> `ReviewPrescriptionServlet` -> `PrescriptionService.decide()` validates pending status, notes and approved medicine lines -> `PrescriptionDAO` stores decision/items -> customer notification is created.
6. **Delivery tracking and updates** — Admin assigns a rider or rider submits a status -> delivery servlet -> `DeliveryService` checks role/assignment, legal next status, order readiness and required failure note -> `DeliveryDAO` transaction updates delivery and corresponding order/history -> SQL Server commits; `NotificationService` notifies the customer/rider and customer tracking reads the order's delivery.

## 4. SECURITY & ETHICS
- Authentication uses HTTP sessions; login regenerates the session ID, logout invalidates it, and account/role routes are guarded by `AuthFilter`.
- Role checks separate admin, pharmacist, delivery staff and customer routes; services also enforce ownership for customer orders, prescriptions, photos and delivery records.
- Passwords use salted PBKDF2-HMAC-SHA256 (120,000 iterations); hash comparison uses `MessageDigest.isEqual`. Login uses a consistent error for unknown email/wrong password and checks a dummy hash for unknown users.
- Server-side validation covers required fields, lengths, numeric ranges, dates, contact details, state transitions and role/ownership constraints. SQL uses prepared statements; database constraints enforce uniqueness, foreign keys, checks and quantity/price limits.
- Prescription uploads are limited to JPG/PNG/PDF and 5 MB, checked by file signature, stored under generated keys outside the public web root, and served only after ownership/pharmacist authorization.
- JSP output is HTML-escaped; post-login/return redirects are restricted to safe local paths. Checkout stores only card last-four digits and uses a test payment flow.

## 5. SPRINT STATUS
- **Completed:** Modules 01–06 have corresponding model/DAO/service/servlet/JSP implementations: cart/wishlist, checkout/orders, catalog/inventory, user/roles, prescription verification, delivery tracking/notifications.
- **Pending / not implemented:** production payment gateway, outbound email/password-reset delivery, and cloud file storage; payment is simulated, password recovery uses customer identity details, and storage is local. No `src/test` Java test suite was found.
