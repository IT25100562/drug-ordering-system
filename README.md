# MediSys: Online Medicine Ordering System

SE2030 Software Engineering group project. MediSys is a Java web app where customers order
medicines online. Prescription-only medicines are checked by a pharmacist before they can be
paid for, and every order is tracked until the rider delivers it.

**Live system:** <https://medisys.vercel.app> *(the address is set when the Vercel project is
created; see [DEPLOYMENT.md](DEPLOYMENT.md))*

| Document | For |
|----------|-----|
| [VIVA-GUIDE.md](VIVA-GUIDE.md) | **Every member.** Each module's Create / Read / Update / Delete (file, method, URL), the validation, the design patterns, and **which files you own** |
| [SETUP.md](SETUP.md) | Running the project on your own laptop |
| [DEPLOYMENT.md](DEPLOYMENT.md) | How the live system is hosted (Supabase, Cloudinary, Render, Vercel) |

## The six major functions

| # | Module | Owner | Java package | CRUD in |
|---|--------|-------|--------------|---------|
| 01 | Shopping Cart and Wishlist | Amadini G. G. A. | `com.medisys.cart` | `CartDAO`, `WishlistDAO` |
| 02 | Order Placement and Checkout | Hewage B. H. A. S. | `com.medisys.order` | `OrderDAO` |
| 03 | Medicine Catalog and Inventory | Divisekara A. W. D. M. D. M. B. | `com.medisys.medicine` | `MedicineDAO`, `CategoryDAO` |
| 04 | Reports and Analytics | Kaweesha P. M. G. S. | `com.medisys.report` | `ReportDAO`, `SavedReportDAO` |
| 05 | Prescription Upload and Verification | Perera D. A. A. N. S. | `com.medisys.prescription` | `PrescriptionDAO` |
| 06 | Delivery Tracking and Notification | Deshabhi R. G. S. | `com.medisys.delivery` | `DeliveryDAO`, `NotificationDAO` |

**Minor functions** (used by every module): register, login / logout, forgot password,
profile with photo and history, roles and page protection, staff accounts, red flags on
customers, notifications. Package `com.medisys.user`.

## Tech stack

| Part | Choice |
|------|--------|
| Language | Java 17+ |
| Web | Jakarta Servlets + JSP on Apache Tomcat 11 (MVC, no frameworks) |
| Database | PostgreSQL on **Supabase**, plain JDBC + connection pool |
| Files and images | **Cloudinary**: private prescriptions / profile photos, public product photos on its CDN |
| Hosting | **Render** (Docker) runs Tomcat; **Vercel** gives the `*.vercel.app` address |
| Build | Maven (`pom.xml`) |
| UI | JSP + one CSS file (`css/style.css`) + plain JavaScript, responsive down to phones |

Design patterns: **MVC, DAO, Singleton, Strategy, Facade, Intercepting Filter,
Post/Redirect/Get**. Each one is explained, with the file to open, in
[VIVA-GUIDE.md](VIVA-GUIDE.md#design-patterns-the-spec-asks-for-at-least-2).

## Folder layout

```
src/main/java/com/medisys/
    cart/  order/  medicine/  report/  prescription/  delivery/   one folder per module:
                                                                  model + DAO + servlets
    user/                                                         minor functions
    common/                                                       shared: DB connection, login filter,
                                                                  file storage, validation
src/main/webapp/
    WEB-INF/views/<module>/*.jsp                                  the pages (same folder names)
    css/style.css   js/*.js   images/
    WEB-INF/sample-uploads/                                       demo files (copied to Cloudinary at startup)
database/
    schema.sql        all tables, one section per module
    sample-data.sql   demo data, one section per module
Dockerfile  render.yaml  vercel/                                   live hosting
```

## Demo logins

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@medisys.lk` | `Admin@123` |
| Pharmacist | `pharmacist@medisys.lk` | `Pharma@123` |
| Customer | `nimal@example.com` | `Customer@123` |
| Customer (red-flagged) | `tharindu@example.com` | `Customer@123` |
| Rider | `delivery@medisys.lk` / `rider2@medisys.lk` | `Delivery@123` |

Test card: `4242 4242 4242 4242`, any future MM/YY, any 3-digit CVV. No real money moves.

## Coding rules

- Everything for a module is in its own folder (`com.medisys.<module>`), its pages in
  `WEB-INF/views/<module>/`.
- SQL lives only in the **DAO** classes, under `// ===== CREATE / READ / UPDATE / DELETE`
  headings. Always `PreparedStatement` with `?`, never string concatenation.
- Validation lives in the **servlet** (`validate...()` method or `// ---- validation` block);
  shared checks are in `common/Validator`.
- Escape all user text in a JSP with `TextUtil.html(...)`.
- Every page includes `common/header.jspf` and `common/footer.jspf`.
- Never commit secrets: `db.properties` and `app.properties` are git-ignored.

## Git workflow

There is one branch: **`main`**, and it is always the live system. Render redeploys every
push to `main` automatically.

1. `git pull` before you start.
2. Make a small change, run it locally (SETUP.md), then commit and push to `main`.
3. Open the live site a few minutes later and check your change.

For a bigger change, make a short-lived branch, open a pull request into `main`, and delete
the branch after merging.
