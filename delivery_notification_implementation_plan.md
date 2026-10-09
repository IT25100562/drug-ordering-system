# Ultimate Delivery & Notification Upgrade Prompt

මේ තියෙන්නේ ඔයාගේ GitHub එක්ක Connect කරලා තියෙන අලුත් Development Folder එකේ ඉන්න AI ට කෙලින්ම Copy-Paste කරලා දෙන්න පුළුවන් **"Best Prompt"** එක. 

මේකේ ඔයාගේ Email එකයි, App Password එකයි, Database වෙනස්කම්, CRUD අදහස් ඔක්කොම එකතු කරලා එකම සම්පූර්ණ Prompt එකක් විදිහට හදලා තියෙන්නේ. මේක දුන්නම අනිත් AI ට මුළු වැඩේම එකපාර තේරුම් අරන් Code ටික ලියන්න පුළුවන්.

---

### මෙන්න මේ සම්පූර්ණ කොටස Copy කරලා අනිත් IDE එකේ AI ට දෙන්න:

```text
I need to upgrade the "Delivery and Notification" modules in this Java Servlet (Maven) project to make it enterprise-grade. I need you to implement Full CRUD for deliveries/notifications, add actual Email sending via JavaMail API, and secure the delivery handover with OTP validation.

Please provide the code and SQL queries for the following 4 steps:

**Step 1: Database & Dependencies**
1. Add the `javax.mail` dependency to `pom.xml`.
2. Give me the SQL ALTER queries to:
   - Add a `delivery_otp` (VARCHAR 255) column to the `delivery` table.
   - Add an `is_active` (BOOLEAN DEFAULT TRUE) column to both `delivery` and `notification` tables for Soft Deletion (Archiving).
3. Update `DeliveryDAOImpl.java` and `NotificationDAOImpl.java` SELECT queries to only fetch records where `is_active = TRUE`.

**Step 2: Real Email Integration (Async)**
1. Create a `MailSenderUtil.java` using the JavaMail API to send emails via `smtp.gmail.com` (TLS, Port 587). 
   - Hardcode these credentials for now: 
     Username: `deshabhi00@gmail.com`
     App Password: `twlhfjnalnkivstj`
2. Modify `NotificationService.java` so that when `notify()` is called, it saves the notification to the database AND asynchronously calls `MailSenderUtil` to send the actual email without blocking the main thread (use `ExecutorService` or a new Thread).

**Step 3: OTP Security for Delivery Handover**
1. In `DeliveryService.java`, create private helper methods to generate a 6-digit OTP (using `SecureRandom`) and to hash strings using `SHA-256`.
2. Update the status change logic: When a delivery status changes to `DISPATCHED`, generate the OTP, send the PLAIN OTP to the customer via `NotificationService`, and save the HASHED OTP to the DB.
3. Create a `confirmDeliveryWithOtp(int deliveryId, String providedOtp)` method. It should hash the `providedOtp`, verify it against the DB, and only if it matches, update the status to `DELIVERED`. 

**Step 4: Complete the Missing CRUD Operations**
To satisfy university requirements, we need full CRUD logic.
1. **Manual Create (Delivery):** Create a new Servlet `CreateDeliveryServlet` that allows an Admin to manually insert a new Delivery record.
2. **Soft Delete (Delivery):** Create a new Servlet `DeleteDeliveryServlet` that updates `is_active = false` for a given delivery ID instead of a hard SQL DELETE.
3. **Soft Delete (Notifications):** Modify the existing delete functionality in `NotificationService` and `DeleteNotificationServlet` to update `is_active = false` instead of executing a hard DELETE.

Please give me the modified Java Classes and Servlets step-by-step.
```
