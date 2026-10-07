# Project Data Extract

## Project identification

| Field | Extracted information |
|---|---|
| **Group ID** | Not stated in the README or the project documentation inspected. |
| **Topic Name** | **MediSys: Online Medicine Ordering System** |
| **Project summary** | Java web application for browsing and ordering medicines, with prescription review by a senior pharmacist and delivery tracking. |

## Group members, use cases, and classes

The repository identifies six module owners. The class categories below follow the project's architecture: **boundary** means request-handling servlets and JSP views; **control** means business services; **entity** means domain/model classes. DAO interfaces and implementations are listed separately as persistence classes. The project does not explicitly label classes as boundary/control/entity, so these categories are mapped from the documented request flow and module ownership.

### 1. Amadini G. G. A. — Shopping Cart and Wishlist

- **Main use-case scenarios:** add medicines to a personal cart; change quantities; remove items, empty the cart, or save items for later; move wishlist items into the cart or remove them; review prices and availability and proceed to checkout. Prescription-only medicines are routed to prescription upload rather than added to the cart.
- **Boundary classes:** `CartServlet`, `AddToCartServlet`, `UpdateCartServlet`, `RemoveFromCartServlet`, `WishlistServlet`, `WishlistActionServlet`; `cart/cart.jsp`, `cart/wishlist.jsp`, `common/heart-button.jspf`.
- **Control classes:** `CartService`, `WishlistService`; shared response helper `CartReply`.
- **Entity classes:** `Cart`, `CartItem`, `WishlistItem`.
- **Persistence classes:** `CartDAO`, `WishlistDAO`, `CartDAOImpl`, `WishlistDAOImpl`.

### 2. Hewage B. H. A. S. — Order Placement and Checkout

- **Main use-case scenarios:** check out a ready cart with delivery and test-payment details; place an order from an approved prescription; view order history and details; reorder eligible medicines; cancel an eligible order and process its refund; administer orders and advance them to packing.
- **Boundary classes:** `CheckoutServlet`, `MyOrdersServlet`, `OrderDetailsServlet`, `CancelOrderServlet`, `ReorderServlet`, `ManageOrdersServlet`, `AdminOrderDetailsServlet`, `UpdateOrderStatusServlet`; `order/checkout.jsp`, `order/my-orders.jsp`, `order/order-details.jsp`, `order/manage-orders.jsp`, `order/admin-order-details.jsp`, `order/order-parts.jspf`, `common/checkout-fields.jspf`.
- **Control classes:** `OrderService`, `PaymentService`.
- **Entity classes:** `Order`, `OrderItem`, `OrderStatus`, `OrderStatusChange`, `Payment`.
- **Persistence classes:** `OrderDAO`, `OrderDAOImpl`; `StockShortageException` represents an ordering failure.

### 3. Divisekara A. W. D. M. D. M. B. — Medicine Catalog and Inventory

- **Main use-case scenarios:** browse/search/filter the medicine catalog and view medicine details; administer medicines and categories; add or edit medicine information, restock, discontinue or restore medicines; review inventory and stock/expiry indicators.
- **Boundary classes:** `CatalogServlet`, `MedicineDetailsServlet`, `InventoryServlet`, `MedicineFormServlet`, `RestockMedicineServlet`, `DiscontinueMedicineServlet`, `CategoryServlet`; `medicine/catalog.jsp`, `medicine/medicine-details.jsp`, `medicine/inventory.jsp`, `medicine/medicine-form.jsp`, `medicine/categories.jsp`.
- **Control classes:** `MedicineService`.
- **Entity classes:** `Medicine`, `Category`, `InventorySummary`.
- **Persistence classes:** `MedicineDAO`, `CategoryDAO`, `MedicineDAOImpl`, `CategoryDAOImpl`.

### 4. Kaweesha P. M. G. S. — User and Role Management

- **Main use-case scenarios:** register, sign in/out, recover a password, and manage a personal profile and photo; administer user/staff accounts and roles; let authorized staff flag or remove a customer flag. Role and login checks protect staff areas.
- **Boundary classes:** `LoginServlet`, `LogoutServlet`, `RegisterServlet`, `ForgotPasswordServlet`, `ProfileServlet`, `ProfilePhotoServlet`, `UserPhotoServlet`, `FlagUserServlet`, `ManageUsersServlet`; `user/login.jsp`, `user/register.jsp`, `user/forgot-password.jsp`, `user/profile.jsp`, `user/manage-users.jsp`.
- **Control classes:** `UserService`, `PasswordUtil`; shared `AuthFilter` and `SessionUtil`.
- **Entity classes:** `User`, `Role`.
- **Persistence classes:** `UserDAO`, `UserDAOImpl`.

### 5. Perera D. A. A. N. S. — Prescription Upload and Verification

- **Main use-case scenarios:** upload and view a prescription; replace a copy when a correction is requested; let a pharmacist inspect pending submissions, enter approved medicine/quantity/dosage lines, and approve, reject, or request a correction; delete invalid or expired unpaid prescriptions; pay for an approved prescription, creating a regular order.
- **Boundary classes:** `UploadPrescriptionServlet`, `MyPrescriptionsServlet`, `ViewPrescriptionServlet`, `PrescriptionPaymentServlet`, `CorrectionUploadServlet`, `CancelPrescriptionServlet`, `PrescriptionFileServlet`, `PharmacistDashboardServlet`, `ReviewPrescriptionServlet`, `DeletePrescriptionServlet`; `prescription/upload.jsp`, `prescription/my-prescriptions.jsp`, `prescription/correct.jsp`, `prescription/dashboard.jsp`, `prescription/review.jsp`, `prescription/view.jsp`, `prescription/pay.jsp`, `prescription/items-table.jspf`, `prescription/file-field.jspf`.
- **Control classes:** `PrescriptionService`; `OrderService` and `PaymentService` are reused for prescription payment; `NotificationService` is used for decision/payment notifications.
- **Entity classes:** `Prescription`, `PrescriptionItem`, `PrescriptionStatus`.
- **Persistence classes:** `PrescriptionDAO`, `PrescriptionDAOImpl`.

### 6. Deshabhi R. G. S. — Delivery Tracking and Notification

- **Main use-case scenarios:** let customers track delivery progress, expected delivery, rider details, and status history; let an administrator assign/change riders and oversee deliveries; let delivery staff see assigned parcels and advance delivery status or report a failed attempt with its reason; let users view and delete their notifications.
- **Boundary classes:** `TrackDeliveryServlet`, `ManageDeliveriesServlet`, `DeliveryDetailsServlet`, `UpdateDeliveryStatusServlet`, `NotificationsServlet`, `DeleteNotificationServlet`; `delivery/track.jsp`, `delivery/manage-deliveries.jsp`, `delivery/delivery-details.jsp`, `delivery/delivery-parts.jspf`, `delivery/notifications.jsp`.
- **Control classes:** `DeliveryService`, `NotificationService`.
- **Entity classes:** `Delivery`, `DeliveryStatus`, `DeliveryUpdate`, `Notification`.
- **Persistence classes:** `DeliveryDAO`, `NotificationDAO`, `DeliveryDAOImpl`, `NotificationDAOImpl`.

## Sources and extraction notes

- `README.md` — project title, module owner list, module scenarios, URLs, and class/module mapping.
- `handover-docs/file-ownership.txt` — detailed module file ownership.
- `documents/se-extract/design_context.md` — actor/use-case overview, entity summaries, and end-to-end activity flows.

The documentation does not provide a Group ID or a formal UML boundary/control/entity classification. The Group ID is therefore recorded as unavailable rather than inferred, and class-role categories above are inferred from the documented architecture and actual module filenames.
