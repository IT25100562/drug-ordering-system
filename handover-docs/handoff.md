# Handoff prompt — paste this into Claude Code in the team repo

---

I am Perera D. A. A. N. S., a student in a 6-person SE2030 group project building an
**Online Medicine Ordering System in Java**. My assigned module is **05 — Prescription
Upload and Verification**. I need you to build my module inside this repo.

We have a week 10 progress evaluation coming up, so it has to actually run and demo.

## STEP 1 — Read this repo first. Do not write any code yet.

My teammates have already committed work here. Before you design anything, read the
existing code and match it. Specifically, find out and tell me:

1. **Build system** — Maven (`pom.xml`), Gradle, or a plain IDE project with a `lib/`
   folder? Use whatever is already there. Do not introduce a new one.
2. **Application type** — Servlet + JSP web app, Spring Boot, JavaFX, or Swing desktop?
   Build my module in the same style, even if it differs from what is described below.
3. **If it is a servlet app**: is it `jakarta.servlet` (Tomcat 10/11) or `javax.servlet`
   (Tomcat 9)? Match it exactly — mixing them will not compile.
4. **Package naming** — what is the root package, and how are packages organised
   (by layer, like `model` / `dao` / `service` / `servlet`, or by feature)? Follow it.
5. **Persistence** — is there a real database (MySQL + JDBC), a DAO interface layer, or
   plain text files? Use the existing approach. If there is a `User` DAO or a
   `DBConnection` class, use it instead of writing my own.
6. **Existing login / session / roles** — teammate 04 owns User and Role Management.
   If a `User` class, a login servlet, a session helper or a role enum already exists,
   use theirs. Do not build a second login.
7. **Existing medicine / product model** — teammate 03 owns Medicine Catalog and
   Inventory. If a `Medicine` or `Product` class with prices exists, link my
   prescription to it instead of storing a plain medicine-name string.
8. **Existing order / checkout code** — teammate 02 owns Order Placement and Checkout.
   If a checkout or payment flow already exists, hook into it rather than duplicating it.
9. **UI conventions** — existing JSP layout, header/footer includes, a shared CSS file,
   JSTL vs scriptlets, URL patterns. My pages must look like the rest of the app.
10. **Git** — which branch should I work on? Check the branches and recent commits.

Then **report what you found and ask me about anything that conflicts** with the
requirements below, before you start building. Do not ask about things you can answer
by reading the repo — just read it.

## STEP 2 — There is a working reference implementation you can read

I already built this module standalone in a separate practice repo:

```
C:\Users\navee\Documents\GitHub\SE-online-medicine-ordering-system-javabased
```

It is a Servlet + JSP + Tomcat 11 app using `jakarta.servlet`, plain text file storage,
no build tool. It compiles, runs, and the whole flow was tested end to end.

Read it for the **design, the rules and the page layouts** — but do **not** copy it in
blindly. This repo's conventions win. If this repo uses Maven, or `javax.servlet`, or a
real database, or a different package name, re-fit the code accordingly.

Useful files there:
- `src/com/medisys/service/PrescriptionService.java` — all the verification rules
- `src/com/medisys/model/Prescription.java` — the entity and its status logic
- `src/com/medisys/storage/` — the text file storage layer
- `web/WEB-INF/views/` — the JSP pages
- `README.md` — full write-up, including the integration notes for my teammates
- `project-details/activity_diagram.png` — the official activity diagram
- `project-details/prescription-tasks.txt` — the official task list

## STEP 3 — What my module has to do

From my assignment sheet, the four required tasks are:

1. Upload a prescription for restricted medicines
2. View submitted prescriptions
3. Approve, reject **or request a correction**
4. Delete an invalid or expired prescription

Purpose: *keeps restricted medicines behind a pharmacist's check before the order is
released.*

### The official activity flow (from the activity diagram)

Three swimlanes — Customer, System, Senior Pharmacist:

1. Customer tries to add a prescription-only medicine to the cart.
2. The system prompts the customer to upload a digital copy of the prescription.
3. Customer uploads the file and submits.
4. The system marks the order status **"Verification Pending"**.
5. The Senior Pharmacist opens the **Verification Dashboard** and opens a pending
   prescription.
6. The pharmacist reviews the document for **authenticity and correct dosage**.
7. Decision — is the prescription valid?
   - **Yes** → pharmacist clicks Approve → system sets status
     **"Approved for Checkout"** → end state: **Ready for Payment**
   - **No** → pharmacist clicks Reject and **attaches a rejection note** → system
     notifies the customer → end state: **Order Cancelled**

Plus the third outcome from the task list: **Request a correction** — the copy is
unclear, so the customer is asked to upload a better one and it goes back to pending.

### Status model

```
        upload                 approve
 (new) --------> PENDING ----------------> APPROVED --> (pay) --> order created
                   |  ^
          reject   |  |  customer uploads a new copy
                   v  |
              REJECTED / CORRECTION_REQUESTED
```

Display labels: PENDING = "Verification Pending", APPROVED = "Approved for Checkout".

### Screens I need

**Customer**
- Upload a prescription: medicine name, quantity, optional note, file picker.
  Accept JPG / PNG / PDF only, max 5 MB. Reject anything else with a clear message.
- My Prescriptions: list of everything I submitted with status, the pharmacist's note,
  and the right action button per row.
- Send a corrected copy (only when status is CORRECTION_REQUESTED).
- Notifications: the messages the system sent me.
- Pay Now (only when status is APPROVED and it is not already paid).

**Senior Pharmacist**
- Verification Dashboard: queue of pending prescriptions, filterable by status.
- Review page: shows the uploaded image or PDF, the customer details, the dosage and
  quantity, with three buttons — **Approve**, **Reject**, **Request Correction**, plus
  a note field.
- Delete an invalid or expired prescription (also deletes the stored file).
  Treat a prescription as **expired** if it was uploaded more than 30 days ago and has
  not been paid for.

### Rules that must hold

- A rejection or a correction request **requires a note** — reject the action otherwise.
- A prescription can only be decided once, while it is PENDING.
- A customer must **never** be able to open another customer's prescription file.
  Serve uploaded files through a servlet that checks ownership, not as a static file
  inside the web folder. The pharmacist may open any of them.
- Never trust the uploaded filename — generate a safe stored name yourself, so a name
  like `..\..\something` cannot write outside the uploads folder.
- A paid prescription cannot be deleted.
- Payment is only possible after approval.
- Escape everything before printing it in a page (a note containing `<script>` must not
  execute).

## STEP 4 — Scope, and the boundaries with my teammates

Build my module fully. Also build these **only if this repo does not already have
them**, and mark them clearly in comments as placeholders owned by someone else:

- **Test payment screen** — a card form (name, number, expiry, CVV, delivery address,
  contact number) with a Pay Now button. It is not a real gateway: any card in a valid
  shape is accepted and the payment always succeeds. No card details are ever stored.
  *(Owned by teammate 02 — Order Placement and Checkout.)*
- **Delivery handoff** — after payment, append the medicine, quantity, customer and
  address to a delivery queue that the inventory member can read.
  *(Consumed by teammate 03 — Medicine Catalog and Inventory, and 06 — Delivery Tracking.)*
- **Role login** — only if there is no login in the repo yet. Hardcoded demo accounts
  are fine: one customer, one senior pharmacist. *(Owned by teammate 04.)*

Do **not** build the shopping cart, the medicine catalog, the inventory, or the
delivery tracking — those belong to other members.

The six modules and their owners:

| # | Module | Member |
|---|--------|--------|
| 01 | Shopping Cart and Wishlist | Amadini G. G. A. |
| 02 | Order Placement and Checkout | Hewage B. H. A. S. |
| 03 | Medicine Catalog and Inventory | Divisekara A. W. D. M. D. M. B. |
| 04 | User and Role Management | Kaweesha P. M. G. S. |
| 05 | **Prescription Upload and Verification** | **Perera D. A. A. N. S. (me)** |
| 06 | Delivery Tracking and Notification | Deshabhi R. G. S. |

## STEP 5 — How to write it

- **Undergraduate level code.** Simple, readable, well commented. No Spring, no
  Hibernate, no Lombok, no design-pattern showmanship unless the repo already uses it.
- **Simple UI.** Plain HTML and CSS matching the existing pages. No React, no Bootstrap
  unless the repo already includes it.
- Keep the business rules in **one service class**, not scattered across the servlets,
  so I can explain it in the evaluation.
- Keep the storage code in its **own layer**, so swapping text files for a real database
  later only touches those classes.
- If we are still on text files: one record per line, fields separated by a delimiter,
  and **escape the delimiter and newlines** inside values or the records will corrupt.

## STEP 6 — Prove it works before you tell me it is done

Do not just say it compiles. Actually:

1. Compile everything and show me the result.
2. Run the app and walk the full flow: login as a customer, upload, log in as the
   pharmacist, request a correction, upload a corrected copy, approve, pay, and confirm
   the order reached the delivery queue.
3. Check the negative cases too: a `.txt` file rejected, rejection without a note
   refused, one customer blocked (404) from another customer's file, a paid
   prescription not deletable.
4. Tell me honestly what passed and what did not.

Then write a short README section covering: what the module does, how to run it, where
the data is stored, and what my teammates need to change when they merge their modules
in — with the exact file or class names.

## My machine, if it helps

- Windows 11, IntelliJ IDEA **Ultimate** 2026.2.1
- No standalone JDK on PATH, but IntelliJ bundles JDK 25 at
  `C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr` — usable for `javac`/`java`
- **No Maven and no Gradle installed** on PATH. If this repo is a Maven project, tell
  me — IntelliJ can download Maven itself, or I can install it.
- Apache **Tomcat 11.0.26** already installed at `C:\Users\navee\tomcat11`
- Target Java 17 syntax so my teammates' older JDKs can still compile it.

Start with STEP 1. Read the repo, report what you found, ask me about any conflicts,
and only then start building.
