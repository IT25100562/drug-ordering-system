/*
    MediSys - step 3 of 3: demo data for testing and the presentation.
    Shared file - agree with the team before changing it.

        sqlcmd -S localhost -E -C -d MediSysDB -i database\sample-data.sql

    Run it right after schema.sql (the tables must be empty).
    Each module adds its own demo rows in its own section.
*/

USE MediSysDB;
GO

-- Recommended SQL Server setting (sqlcmd turns it off by default).
SET QUOTED_IDENTIFIER ON;
GO

-- =================================================================
-- Module 04 - User and Role Management
-- =================================================================
-- Demo logins (email / password):
--   admin@medisys.lk       / Admin@123      ADMIN
--   pharmacist@medisys.lk  / Pharma@123     PHARMACIST
--   nimal@example.com      / Customer@123   CUSTOMER
--   kasuni@example.com     / Customer@123   CUSTOMER
--   delivery@medisys.lk    / Delivery@123   DELIVERY_STAFF
-- The hashes were made with:  java com.medisys.util.PasswordUtil <password>

INSERT INTO users (full_name, email, phone, address, password_hash, role) VALUES
    (N'System Admin', N'admin@medisys.lk', N'0112345678', N'MediSys Pharmacy, Colombo 03',
     'pbkdf2$120000$5lv47c8EzGChdL+BXd+AfQ==$t2Q4pjt1X50WSBXJXm5hR2NPDFd4VpwvIhj8bwqLNhU=', 'ADMIN'),
    (N'Dr. Sunil Fernando', N'pharmacist@medisys.lk', N'0112345679', N'MediSys Pharmacy, Colombo 03',
     'pbkdf2$120000$BbgedbippGnZMTbhF2Qt6w==$Fvyv9/EOMBDs5vjz50tVukVUSaJhUicpnY0Q0TfrTuY=', 'PHARMACIST'),
    (N'Nimal Perera', N'nimal@example.com', N'0771234567', N'12 Temple Road, Maharagama',
     'pbkdf2$120000$UAbhe9vXLT/vc3JkTryxAA==$eg07Xv88WmKfyx+tKFxuEoYQXOwL6Iow+YbKqHoympU=', 'CUSTOMER'),
    (N'Kasuni Silva', N'kasuni@example.com', N'0719876543', N'45 Lake Drive, Kandy',
     'pbkdf2$120000$UAbhe9vXLT/vc3JkTryxAA==$eg07Xv88WmKfyx+tKFxuEoYQXOwL6Iow+YbKqHoympU=', 'CUSTOMER'),
    (N'Ruwan Jayasinghe', N'delivery@medisys.lk', N'0751112223', N'MediSys Pharmacy, Colombo 03',
     'pbkdf2$120000$Aar7NMzQshP8R5PoZgOAXw==$16sWh8jVxtRuD2juDgWJKOB9FZE7ow+C8bvn0o7jA2E=', 'DELIVERY_STAFF');
GO

-- =================================================================
-- Module 03 - Medicine Catalog and Inventory
-- =================================================================

INSERT INTO categories (name, description) VALUES
    (N'Pain Relief',      N'Painkillers and fever reducers'),
    (N'Antibiotics',      N'Medicines for bacterial infections (prescription only)'),
    (N'Cold and Flu',     N'Cough syrups, decongestants and lozenges'),
    (N'Vitamins',         N'Vitamins and dietary supplements'),
    (N'Diabetes',         N'Blood sugar control'),
    (N'Heart and Blood Pressure', N'Cardiovascular medicines'),
    (N'Skin Care',        N'Creams and ointments'),
    (N'First Aid',        N'Antiseptics and wound care');

-- Look the category ids up by name so this script does not depend on
-- the identity numbers.
INSERT INTO medicines
    (name, category_id, manufacturer, dosage_form, strength, description,
     price, stock_quantity, reorder_level, requires_prescription, expiry_date)
SELECT m.name, c.id, m.manufacturer, m.dosage_form, m.strength, m.description,
       m.price, m.stock_quantity, m.reorder_level, m.requires_prescription, m.expiry_date
FROM (VALUES
    (N'Panadol',        N'Pain Relief',  N'GSK',             N'Tablet',   N'500 mg',
     N'Paracetamol for headache, toothache and fever.',              12.50, 500, 50, 0, '2028-06-30'),
    (N'Brufen',         N'Pain Relief',  N'Abbott',          N'Tablet',   N'400 mg',
     N'Ibuprofen for pain and inflammation. Take after meals.',      18.00,   8, 20, 0, '2027-12-31'),
    (N'Amoxicillin',    N'Antibiotics',  N'State Pharmaceuticals', N'Capsule', N'500 mg',
     N'Broad-spectrum antibiotic. Complete the full course.',        35.00, 120, 30, 1, '2027-09-30'),
    (N'Azithromycin',   N'Antibiotics',  N'Pfizer',          N'Tablet',   N'250 mg',
     N'Antibiotic for respiratory and skin infections.',             95.00,   0, 10, 1, '2027-11-30'),
    (N'Piriton',        N'Cold and Flu', N'GSK',             N'Tablet',   N'4 mg',
     N'Chlorphenamine for allergies and hay fever.',                  6.00, 300, 40, 0, '2028-01-31'),
    (N'Benadryl Cough Syrup', N'Cold and Flu', N'Johnson & Johnson', N'Syrup', N'100 ml',
     N'Relief for dry and chesty cough.',                           420.00,  45, 15, 0, '2027-08-31'),
    (N'Vitamin C',      N'Vitamins',     N'Nature''s Bounty', N'Tablet',  N'1000 mg',
     N'Supports the immune system.',                                 22.00, 250, 30, 0, '2028-12-31'),
    (N'Metformin',      N'Diabetes',     N'Merck',           N'Tablet',   N'500 mg',
     N'Controls blood sugar in type 2 diabetes.',                    15.00,  90, 25, 1, '2027-10-31'),
    (N'Losartan',       N'Heart and Blood Pressure', N'Cipla', N'Tablet', N'50 mg',
     N'Treats high blood pressure.',                                 28.00,  12, 20, 1, '2027-07-31'),
    (N'Hydrocortisone Cream', N'Skin Care', N'Glenmark',     N'Cream',    N'1%',
     N'Relieves itching, rashes and insect bites.',                 380.00,  30, 10, 0, '2027-05-31'),
    (N'Dettol Antiseptic', N'First Aid', N'Reckitt',         N'Liquid',   N'250 ml',
     N'Antiseptic liquid for cuts and grazes.',                     650.00,  60, 15, 0, '2029-01-31'),
    (N'Old Cough Mixture', N'Cold and Flu', N'Local Labs',   N'Syrup',    N'100 ml',
     N'Past its expiry date - demo for the Expired filter.',          250.00,  5,  5, 0, '2025-12-31'),
    (N'Codeine Linctus', N'Cold and Flu', N'Local Labs',     N'Syrup',    N'100 ml',
     N'No longer sold - demo for the Discontinued filter.',          300.00, 10,  5, 1, '2027-06-30')
) AS m (name, category_name, manufacturer, dosage_form, strength, description,
        price, stock_quantity, reorder_level, requires_prescription, expiry_date)
JOIN categories c ON c.name = m.category_name;

-- One discontinued medicine to demo the Discontinued filter and Restore.
UPDATE medicines SET is_discontinued = 1 WHERE name = N'Codeine Linctus';
GO

-- =================================================================
-- Module 01 - Shopping Cart and Wishlist
-- =================================================================
-- The demo customer Nimal starts with two cart items and one saved medicine.

INSERT INTO cart_items (user_id, medicine_id, quantity)
SELECT u.id, m.id, v.quantity
FROM (VALUES (N'Panadol', 2), (N'Vitamin C', 1)) AS v (medicine_name, quantity)
JOIN medicines m ON m.name = v.medicine_name
JOIN users u ON u.email = N'nimal@example.com';

INSERT INTO wishlist_items (user_id, medicine_id)
SELECT u.id, m.id
FROM medicines m
JOIN users u ON u.email = N'nimal@example.com'
WHERE m.name = N'Benadryl Cough Syrup';
GO

-- =================================================================
-- Module 02 - Order Placement and Checkout
-- =================================================================
--   ORD 1  Nimal   cart          DELIVERED   6 days ago
--   ORD 2  Kasuni  prescription  SHIPPED     paid for RX 6 (linked in the module 05 section)
--   ORD 3  Nimal   cart          PAID        new - waiting to be packed
--   ORD 4  Kasuni  cart          CANCELLED   refunded
--   ORD 5  Kasuni  cart          PROCESSING  free delivery (over Rs. 2,500)
-- Totals follow the rule: delivery Rs. 300, free from Rs. 2,500.

INSERT INTO orders (user_id, source, status, subtotal, delivery_fee, total, delivery_name,
                    delivery_address, delivery_phone, delivery_note, cancel_reason, created_at, updated_at)
SELECT u.id, v.source, v.status, v.subtotal, v.fee, v.subtotal + v.fee, u.full_name, u.address, u.phone,
       v.note, v.cancel_reason, DATEADD(hour, -v.hours_ago, SYSDATETIME()), DATEADD(hour, -v.hours_ago, SYSDATETIME())
FROM (VALUES
    (1, N'nimal@example.com',  'CART',         'DELIVERED',  91.00,   300.00, N'Please ring the bell twice', NULL,                                          144),
    (2, N'kasuni@example.com', 'PRESCRIPTION', 'SHIPPED',    84.00,   300.00, NULL,                          NULL,                                           90),
    (3, N'nimal@example.com',  'CART',         'PAID',       1070.00, 300.00, NULL,                          NULL,                                            2),
    (4, N'kasuni@example.com', 'CART',         'CANCELLED',  380.00,  300.00, NULL,                          N'Cancelled by the customer: Ordered by mistake', 72),
    (5, N'kasuni@example.com', 'CART',         'PROCESSING', 2532.00, 0.00,   N'Leave with the security guard', NULL,                                 26)
) AS v (sort_order, email, source, status, subtotal, fee, note, cancel_reason, hours_ago)
JOIN users u ON u.email = v.email
ORDER BY v.sort_order;

-- The lines (name and price copied from the catalog).
INSERT INTO order_items (order_id, medicine_id, medicine_name, dosage_form, unit_price, quantity, dosage_instructions)
SELECT v.order_id, m.id, CONCAT(m.name, ' ', m.strength), m.dosage_form, m.price, v.quantity, v.dosage
FROM (VALUES
    (1, N'Panadol',              2, NULL),
    (1, N'Vitamin C',            3, NULL),
    (2, N'Losartan',             3, N'1 tablet every morning'),
    (3, N'Dettol Antiseptic',    1, NULL),
    (3, N'Benadryl Cough Syrup', 1, NULL),
    (4, N'Hydrocortisone Cream', 1, NULL),
    (5, N'Benadryl Cough Syrup', 6, NULL),
    (5, N'Piriton',              2, NULL)
) AS v (order_id, medicine_name, quantity, dosage)
JOIN medicines m ON m.name = v.medicine_name;

-- One (test) card payment per order.
INSERT INTO payments (order_id, amount, card_last4, reference, status, paid_at, refunded_at)
SELECT o.id, o.total, v.last4, v.reference, v.status, o.created_at,
       CASE WHEN v.status = 'REFUNDED' THEN DATEADD(minute, 20, o.created_at) END
FROM (VALUES
    (1, '4242', 'PAY-20260911-A3KD7Q', 'PAID'),
    (2, '4242', 'PAY-20260913-K7Q2XD', 'PAID'),
    (3, '4242', 'PAY-20260917-M8TR2E', 'PAID'),
    (4, '1881', 'PAY-20260914-Q4WZ9N', 'REFUNDED'),
    (5, '4242', 'PAY-20260916-H2VB6P', 'PAID')
) AS v (order_id, last4, reference, status)
JOIN orders o ON o.id = v.order_id;

-- The history behind each order's timeline.
INSERT INTO order_status_history (order_id, status, note, changed_by, changed_at)
SELECT o.id, v.status, v.note, CASE WHEN v.by_admin = 1 THEN a.id END,
       DATEADD(minute, v.minutes_after, o.created_at)
FROM (VALUES
    (1, 'PAID',       N'Order placed and paid', 0, 0),
    (1, 'PROCESSING', NULL,                     1, 90),
    (1, 'SHIPPED',    N'Rider Kamal, 077 123 4567', 1, 600),
    (1, 'DELIVERED',  NULL,                     1, 1000),
    (2, 'PAID',       N'Order placed and paid', 0, 0),
    (2, 'PROCESSING', NULL,                     1, 60),
    (2, 'SHIPPED',    N'Rider Nuwan, 071 555 0101', 1, 300),
    (3, 'PAID',       N'Order placed and paid', 0, 0),
    (4, 'PAID',       N'Order placed and paid', 0, 0),
    (4, 'CANCELLED',  N'Cancelled by the customer: Ordered by mistake', 0, 20),
    (5, 'PAID',       N'Order placed and paid', 0, 0),
    (5, 'PROCESSING', NULL,                     1, 45)
) AS v (order_id, status, note, by_admin, minutes_after)
JOIN orders o ON o.id = v.order_id
CROSS JOIN (SELECT id FROM users WHERE email = N'admin@medisys.lk') a;
GO

-- =================================================================
-- Module 05 - Prescription Upload and Verification
-- =================================================================
-- The files behind these rows are in src/main/webapp/WEB-INF/sample-uploads.
-- The app copies them into the file storage (key "samples/...") when it starts.
--
--   RX 1  Nimal   PENDING               waiting in the dashboard
--   RX 2  Kasuni  CORRECTION_REQUESTED  blurry photo
--   RX 3  Kasuni  REJECTED              not signed
--   RX 4  Nimal   APPROVED, not paid    Metformin x5 + Panadol x2 -> Nimal can pay
--   RX 5  Kasuni  PENDING, 40 days old  expired - the pharmacist should delete it
--   RX 6  Kasuni  APPROVED and PAID     Losartan x3, paid by order 2 -> shows the receipt

INSERT INTO prescriptions
    (user_id, customer_note, file_key, original_file_name, content_type, file_size,
     status, pharmacist_note, reviewed_by, reviewed_at, uploaded_at, updated_at)
SELECT u.id, v.customer_note, v.file_key, v.original_file_name, v.content_type, v.file_size,
       v.status, v.pharmacist_note,
       CASE WHEN v.status = 'PENDING' THEN NULL ELSE ph.id END,
       CASE WHEN v.status = 'PENDING' THEN NULL ELSE DATEADD(hour, -v.hours_ago + 2, SYSDATETIME()) END,
       DATEADD(hour, -v.hours_ago, SYSDATETIME()),
       DATEADD(hour, -v.hours_ago + 2, SYSDATETIME())
FROM (VALUES
    (1, N'nimal@example.com',  N'For my throat infection.',
     'samples/rx-demo-1.png', N'prescription-city-medical.png', 'image/png', 59269,
     'PENDING', NULL, 5),
    (2, N'kasuni@example.com', NULL,
     'samples/rx-demo-2.jpg', N'IMG_20260910_0932.jpg', 'image/jpeg', 29912,
     'CORRECTION_REQUESTED', N'The photo is too blurry to read the dosage. Please take a clear photo in good light.', 30),
    (3, N'kasuni@example.com', NULL,
     'samples/rx-demo-3.png', N'azithromycin-rx.png', 'image/png', 56581,
     'REJECTED', N'The prescription is not signed by the doctor.', 72),
    (4, N'nimal@example.com',  N'Monthly refill.',
     'samples/rx-demo-4.pdf', N'diabetes-clinic-prescription.pdf', 'application/pdf', 1343,
     'APPROVED', N'Take Metformin with food to avoid an upset stomach.', 48),
    (5, N'kasuni@example.com', NULL,
     'samples/rx-demo-5.png', N'old-prescription.png', 'image/png', 56228,
     'PENDING', NULL, 40 * 24),
    (6, N'kasuni@example.com', NULL,
     'samples/rx-demo-6.png', N'blood-pressure-rx.png', 'image/png', 55096,
     'APPROVED', NULL, 96)
) AS v (sort_order, email, customer_note, file_key, original_file_name, content_type, file_size,
        status, pharmacist_note, hours_ago)
JOIN users u ON u.email = v.email
CROSS JOIN (SELECT id FROM users WHERE email = N'pharmacist@medisys.lk') ph
ORDER BY v.sort_order;

-- The medicines the pharmacist listed (price copied from the catalog).
INSERT INTO prescription_items (prescription_id, medicine_id, quantity, dosage_instructions, unit_price)
SELECT p.id, m.id, v.quantity, v.dosage, m.price
FROM (VALUES
    ('samples/rx-demo-4.pdf', N'Metformin', 5, N'1 tablet twice daily with meals'),
    ('samples/rx-demo-4.pdf', N'Panadol',   2, N'1-2 tablets when needed for pain, at most 8 in 24 hours'),
    ('samples/rx-demo-6.png', N'Losartan',  3, N'1 tablet every morning')
) AS v (file_key, medicine_name, quantity, dosage)
JOIN prescriptions p ON p.file_key = v.file_key
JOIN medicines m ON m.name = v.medicine_name;

-- RX 6 was paid: order 2 (see the module 02 section) paid for it.
UPDATE prescriptions SET order_id = 2 WHERE file_key = 'samples/rx-demo-6.png';

-- The notifications those decisions sent.
INSERT INTO notifications (user_id, message, link, is_read, created_at)
SELECT u.id, v.message, v.link, v.is_read, DATEADD(hour, -v.hours_ago, SYSDATETIME())
FROM (VALUES
    (N'nimal@example.com', N'Your prescription RX-000004 was approved: 2 medicines, total Rs. 100.00. See how to use them and pay.',
     N'/prescriptions', 0, 46),
    (N'kasuni@example.com', N'Please upload a new copy of prescription RX-000002: The photo is too blurry to read the dosage. Please take a clear photo in good light.',
     N'/prescriptions', 0, 28),
    (N'kasuni@example.com', N'Your prescription RX-000003 was rejected: The prescription is not signed by the doctor.',
     N'/prescriptions', 1, 70),
    (N'kasuni@example.com', N'Thank you! Order ORD-000002 (Rs. 384.00) was placed and paid. Payment reference PAY-20260913-K7Q2XD.',
     N'/orders/view?id=2', 1, 90),
    (N'kasuni@example.com', N'Order ORD-000002 is out for delivery to 45 Lake Drive, Kandy. Note: Rider Nuwan, 071 555 0101',
     N'/orders/view?id=2', 0, 85),
    (N'nimal@example.com', N'Order ORD-000001 was delivered. Thank you for shopping with MediSys!',
     N'/orders/view?id=1', 1, 127)
) AS v (email, message, link, is_read, hours_ago)
JOIN users u ON u.email = v.email;
GO
