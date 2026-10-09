/*
    MediSys - step 2 of 2: demo data for testing and the presentation.
    Shared file - agree with the team before changing it.

    Run it right after schema.sql (the tables must be empty):
        psql "<connection string>" -f database/schema.sql -f database/sample-data.sql
    or paste it into the Supabase dashboard -> SQL Editor -> Run.

    Each module adds its own demo rows in its own section.
    Times are Sri Lanka time, so "2 hours ago" means 2 hours ago in Colombo.
*/

SET TIME ZONE 'Asia/Colombo';

-- =================================================================
-- Minor functions - User accounts and roles
-- =================================================================
-- Demo logins (email / password):
--   admin@medisys.lk       / Admin@123      ADMIN
--   pharmacist@medisys.lk  / Pharma@123     PHARMACIST
--   nimal@example.com      / Customer@123   CUSTOMER
--   kasuni@example.com     / Customer@123   CUSTOMER
--   tharindu@example.com   / Customer@123   CUSTOMER (red-flagged)
--   delivery@medisys.lk    / Delivery@123   DELIVERY_STAFF
--   rider2@medisys.lk      / Delivery@123   DELIVERY_STAFF
-- The hashes were made with:  java com.medisys.common.PasswordUtil <password>

-- Customers must have a NIC, date of birth and phone (a CHECK in schema.sql),
-- so they are added with those columns filled straight away.
INSERT INTO users (full_name, email, phone, address, password_hash, role, nic, date_of_birth, whatsapp)
SELECT full_name, email, phone, address, password_hash, role,
       CASE email WHEN 'nimal@example.com'    THEN '901351234V'
                  WHEN 'kasuni@example.com'   THEN '199673512345'
                  WHEN 'tharindu@example.com' THEN '200106312345' END,
       CASE email WHEN 'nimal@example.com'    THEN DATE '1990-05-14'
                  WHEN 'kasuni@example.com'   THEN DATE '1996-08-22'
                  WHEN 'tharindu@example.com' THEN DATE '2001-03-03' END,
       CASE WHEN role = 'CUSTOMER' THEN phone END
FROM (VALUES
    (1, 'System Admin', 'admin@medisys.lk', '0112345678', 'MediSys Pharmacy, Colombo 03',
     'pbkdf2$120000$5lv47c8EzGChdL+BXd+AfQ==$t2Q4pjt1X50WSBXJXm5hR2NPDFd4VpwvIhj8bwqLNhU=', 'ADMIN'),
    (2, 'Dr. Sunil Fernando', 'pharmacist@medisys.lk', '0112345679', 'MediSys Pharmacy, Colombo 03',
     'pbkdf2$120000$BbgedbippGnZMTbhF2Qt6w==$Fvyv9/EOMBDs5vjz50tVukVUSaJhUicpnY0Q0TfrTuY=', 'PHARMACIST'),
    (3, 'Nimal Perera', 'nimal@example.com', '0771234567', '12 Temple Road, Maharagama',
     'pbkdf2$120000$UAbhe9vXLT/vc3JkTryxAA==$eg07Xv88WmKfyx+tKFxuEoYQXOwL6Iow+YbKqHoympU=', 'CUSTOMER'),
    (4, 'Kasuni Silva', 'kasuni@example.com', '0719876543', '45 Lake Drive, Kandy',
     'pbkdf2$120000$UAbhe9vXLT/vc3JkTryxAA==$eg07Xv88WmKfyx+tKFxuEoYQXOwL6Iow+YbKqHoympU=', 'CUSTOMER'),
    (7, 'Tharindu Fernando', 'tharindu@example.com', '0762223334', '8 Station Road, Galle',
     'pbkdf2$120000$UAbhe9vXLT/vc3JkTryxAA==$eg07Xv88WmKfyx+tKFxuEoYQXOwL6Iow+YbKqHoympU=', 'CUSTOMER'),
    (5, 'Ruwan Jayasinghe', 'delivery@medisys.lk', '0751112223', 'MediSys Pharmacy, Colombo 03',
     'pbkdf2$120000$Aar7NMzQshP8R5PoZgOAXw==$16sWh8jVxtRuD2juDgWJKOB9FZE7ow+C8bvn0o7jA2E=', 'DELIVERY_STAFF'),
    (6, 'Kamal Perera', 'rider2@medisys.lk', '0771234599', 'MediSys Pharmacy, Colombo 03',
     'pbkdf2$120000$Aar7NMzQshP8R5PoZgOAXw==$16sWh8jVxtRuD2juDgWJKOB9FZE7ow+C8bvn0o7jA2E=', 'DELIVERY_STAFF')
) AS v (sort_order, full_name, email, phone, address, password_hash, role)
ORDER BY sort_order;          -- fixed ids: admin 1, pharmacist 2, Nimal 3, Kasuni 4, riders 5-6, Tharindu 7

-- Profile photos (the files are in WEB-INF/sample-uploads, copied at startup).
UPDATE users SET photo_key = 'samples/avatar-nimal.png'  WHERE email = 'nimal@example.com';
UPDATE users SET photo_key = 'samples/avatar-kasuni.png' WHERE email = 'kasuni@example.com';

-- Tharindu sent a holiday photo as a "prescription" (RX 7), so the pharmacist flagged him.
UPDATE users
SET is_flagged = TRUE,
    flag_reason = 'Uploaded a holiday photo instead of a prescription. Check his uploads carefully.',
    flagged_by = (SELECT id FROM users WHERE email = 'pharmacist@medisys.lk'),
    flagged_at = (LOCALTIMESTAMP - INTERVAL '20 hours')
WHERE email = 'tharindu@example.com';
-- =================================================================
-- Module 03 - Medicine Catalog and Inventory
-- =================================================================

INSERT INTO categories (name, description) VALUES
    ('Pain Relief',      'Painkillers and fever reducers'),
    ('Antibiotics',      'Medicines for bacterial infections (prescription only)'),
    ('Cold and Flu',     'Cough syrups, decongestants and lozenges'),
    ('Vitamins',         'Vitamins and dietary supplements'),
    ('Diabetes',         'Blood sugar control'),
    ('Heart and Blood Pressure', 'Cardiovascular medicines'),
    ('Skin Care',        'Creams and ointments'),
    ('First Aid',        'Antiseptics and wound care');

-- Look the category ids up by name so this script does not depend on
-- the identity numbers.
INSERT INTO medicines
    (name, category_id, manufacturer, dosage_form, strength, description,
     price, stock_quantity, reorder_level, requires_prescription, expiry_date)
SELECT m.name, c.id, m.manufacturer, m.dosage_form, m.strength, m.description,
       m.price, m.stock_quantity, m.reorder_level, m.requires_prescription = 1, CAST(m.expiry_date AS DATE)
FROM (VALUES
    ('Panadol',        'Pain Relief',  'GSK',             'Tablet',   '500 mg',
     'Paracetamol for headache, toothache and fever.',              12.50, 500, 50, 0, '2028-06-30'),
    ('Brufen',         'Pain Relief',  'Abbott',          'Tablet',   '400 mg',
     'Ibuprofen for pain and inflammation. Take after meals.',      18.00,   8, 20, 0, '2027-12-31'),
    ('Amoxicillin',    'Antibiotics',  'State Pharmaceuticals', 'Capsule', '500 mg',
     'Broad-spectrum antibiotic. Complete the full course.',        35.00, 120, 30, 1, '2027-09-30'),
    ('Azithromycin',   'Antibiotics',  'Pfizer',          'Tablet',   '250 mg',
     'Antibiotic for respiratory and skin infections.',             95.00,   0, 10, 1, '2027-11-30'),
    ('Piriton',        'Cold and Flu', 'GSK',             'Tablet',   '4 mg',
     'Chlorphenamine for allergies and hay fever.',                  6.00, 300, 40, 0, '2028-01-31'),
    ('Benadryl Cough Syrup', 'Cold and Flu', 'Johnson & Johnson', 'Syrup', '100 ml',
     'Relief for dry and chesty cough.',                           420.00,  45, 15, 0, '2027-08-31'),
    ('Vitamin C',      'Vitamins',     'Nature''s Bounty', 'Tablet',  '1000 mg',
     'Supports the immune system.',                                 22.00, 250, 30, 0, '2028-12-31'),
    ('Metformin',      'Diabetes',     'Merck',           'Tablet',   '500 mg',
     'Controls blood sugar in type 2 diabetes.',                    15.00,  90, 25, 1, '2027-10-31'),
    ('Losartan',       'Heart and Blood Pressure', 'Cipla', 'Tablet', '50 mg',
     'Treats high blood pressure.',                                 28.00,  12, 20, 1, '2027-07-31'),
    ('Hydrocortisone Cream', 'Skin Care', 'Glenmark',     'Cream',    '1%',
     'Relieves itching, rashes and insect bites.',                 380.00,  30, 10, 0, '2027-05-31'),
    ('Dettol Antiseptic', 'First Aid', 'Reckitt',         'Liquid',   '250 ml',
     'Antiseptic liquid for cuts and grazes.',                     650.00,  60, 15, 0, '2029-01-31'),
    ('Old Cough Mixture', 'Cold and Flu', 'Local Labs',   'Syrup',    '100 ml',
     'Past its expiry date - demo for the Expired filter.',          250.00,  5,  5, 0, '2025-12-31'),
    ('Codeine Linctus', 'Cold and Flu', 'Local Labs',     'Syrup',    '100 ml',
     'No longer sold - demo for the Discontinued filter.',          300.00, 10,  5, 1, '2027-06-30')
) AS m (name, category_name, manufacturer, dosage_form, strength, description,
        price, stock_quantity, reorder_level, requires_prescription, expiry_date)
JOIN categories c ON c.name = m.category_name;

-- Product photos (the files are in WEB-INF/sample-uploads; at startup the app
-- copies them to the file storage - Cloudinary in the live system).
UPDATE medicines m SET image_key = 'medicines/med-' || v.file || '.png'
FROM (VALUES
    ('Brufen', 'brufen'), ('Panadol', 'panadol'), ('Azithromycin', 'azithromycin'),
    ('Amoxicillin', 'amoxicillin'), ('Codeine Linctus', 'codeine-linctus'),
    ('Old Cough Mixture', 'old-cough-mixture'), ('Benadryl Cough Syrup', 'benadryl'),
    ('Piriton', 'piriton'), ('Vitamin C', 'vitamin-c'), ('Metformin', 'metformin'),
    ('Losartan', 'losartan'), ('Hydrocortisone Cream', 'hydrocortisone'), ('Dettol Antiseptic', 'dettol')
) AS v (name, file)
WHERE m.name = v.name;

-- One discontinued medicine to demo the Discontinued filter and Restore.
UPDATE medicines SET is_discontinued = TRUE WHERE name = 'Codeine Linctus';
-- =================================================================
-- Module 01 - Shopping Cart and Wishlist
-- =================================================================
-- The demo customer Nimal starts with two cart items and one saved medicine.

INSERT INTO cart_items (user_id, medicine_id, quantity)
SELECT u.id, m.id, v.quantity
FROM (VALUES ('Panadol', 2), ('Vitamin C', 1)) AS v (medicine_name, quantity)
JOIN medicines m ON m.name = v.medicine_name
JOIN users u ON u.email = 'nimal@example.com';

INSERT INTO wishlist_items (user_id, medicine_id)
SELECT u.id, m.id
FROM medicines m
JOIN users u ON u.email = 'nimal@example.com'
WHERE m.name = 'Benadryl Cough Syrup';
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
       v.note, v.cancel_reason, (LOCALTIMESTAMP - v.hours_ago * INTERVAL '1 hour'), (LOCALTIMESTAMP - v.hours_ago * INTERVAL '1 hour')
FROM (VALUES
    (1, 'nimal@example.com',  'CART',         'DELIVERED',  91.00,   300.00, 'Please ring the bell twice', NULL,                                          144),
    (2, 'kasuni@example.com', 'PRESCRIPTION', 'SHIPPED',    84.00,   300.00, NULL,                          NULL,                                           90),
    (3, 'nimal@example.com',  'CART',         'PAID',       1070.00, 300.00, NULL,                          NULL,                                            2),
    (4, 'kasuni@example.com', 'CART',         'CANCELLED',  380.00,  300.00, NULL,                          'Cancelled by the customer: Ordered by mistake', 72),
    (5, 'kasuni@example.com', 'CART',         'PROCESSING', 2532.00, 0.00,   'Leave with the security guard', NULL,                                 26)
) AS v (sort_order, email, source, status, subtotal, fee, note, cancel_reason, hours_ago)
JOIN users u ON u.email = v.email
ORDER BY v.sort_order;

-- The lines (name and price copied from the catalog).
INSERT INTO order_items (order_id, medicine_id, medicine_name, dosage_form, unit_price, quantity, dosage_instructions)
SELECT v.order_id, m.id, CONCAT(m.name, ' ', m.strength), m.dosage_form, m.price, v.quantity, v.dosage
FROM (VALUES
    (1, 'Panadol',              2, NULL),
    (1, 'Vitamin C',            3, NULL),
    (2, 'Losartan',             3, '1 tablet every morning'),
    (3, 'Dettol Antiseptic',    1, NULL),
    (3, 'Benadryl Cough Syrup', 1, NULL),
    (4, 'Hydrocortisone Cream', 1, NULL),
    (5, 'Benadryl Cough Syrup', 6, NULL),
    (5, 'Piriton',              2, NULL)
) AS v (order_id, medicine_name, quantity, dosage)
JOIN medicines m ON m.name = v.medicine_name;

-- One (test) card payment per order.
INSERT INTO payments (order_id, amount, card_last4, reference, status, paid_at, refunded_at)
SELECT o.id, o.total, v.last4, v.reference, v.status, o.created_at,
       CASE WHEN v.status = 'REFUNDED' THEN (o.created_at + INTERVAL '20 minutes') END
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
       (o.created_at + (v.minutes_after) * INTERVAL '1 minute')
FROM (VALUES
    (1, 'PAID',       'Order placed and paid', 0, 0),
    (1, 'PROCESSING', NULL,                     1, 90),
    (1, 'SHIPPED',    'Picked up by Kamal Perera', 1, 600),
    (1, 'DELIVERED',  'Handed to the customer', 1, 1000),
    (2, 'PAID',       'Order placed and paid', 0, 0),
    (2, 'PROCESSING', NULL,                     1, 60),
    (2, 'SHIPPED',    'Picked up by Ruwan Jayasinghe', 1, 300),
    (3, 'PAID',       'Order placed and paid', 0, 0),
    (4, 'PAID',       'Order placed and paid', 0, 0),
    (4, 'CANCELLED',  'Cancelled by the customer: Ordered by mistake', 0, 20),
    (5, 'PAID',       'Order placed and paid', 0, 0),
    (5, 'PROCESSING', NULL,                     1, 45)
) AS v (order_id, status, note, by_admin, minutes_after)
JOIN orders o ON o.id = v.order_id
CROSS JOIN (SELECT id FROM users WHERE email = 'admin@medisys.lk') a;
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
--   RX 7  Tharindu REJECTED             a holiday photo -> Tharindu is flagged (module 04 section)

INSERT INTO prescriptions
    (user_id, customer_note, file_key, original_file_name, content_type, file_size,
     status, pharmacist_note, reviewed_by, reviewed_at, uploaded_at, updated_at)
SELECT u.id, v.customer_note, v.file_key, v.original_file_name, v.content_type, v.file_size,
       v.status, v.pharmacist_note,
       CASE WHEN v.status = 'PENDING' THEN NULL ELSE ph.id END,
       CASE WHEN v.status = 'PENDING' THEN NULL ELSE (LOCALTIMESTAMP + (-v.hours_ago + 2) * INTERVAL '1 hour') END,
       (LOCALTIMESTAMP - v.hours_ago * INTERVAL '1 hour'),
       (LOCALTIMESTAMP + (-v.hours_ago + 2) * INTERVAL '1 hour')
FROM (VALUES
    (1, 'nimal@example.com',  'For my throat infection.',
     'samples/rx-demo-1.png', 'prescription-city-medical.png', 'image/png', 59269,
     'PENDING', NULL, 5),
    (2, 'kasuni@example.com', NULL,
     'samples/rx-demo-2.jpg', 'IMG_20260910_0932.jpg', 'image/jpeg', 29912,
     'CORRECTION_REQUESTED', 'The photo is too blurry to read the dosage. Please take a clear photo in good light.', 30),
    (3, 'kasuni@example.com', NULL,
     'samples/rx-demo-3.png', 'azithromycin-rx.png', 'image/png', 56581,
     'REJECTED', 'The prescription is not signed by the doctor.', 72),
    (4, 'nimal@example.com',  'Monthly refill.',
     'samples/rx-demo-4.pdf', 'diabetes-clinic-prescription.pdf', 'application/pdf', 1343,
     'APPROVED', 'Take Metformin with food to avoid an upset stomach.', 48),
    (5, 'kasuni@example.com', NULL,
     'samples/rx-demo-5.png', 'old-prescription.png', 'image/png', 56228,
     'PENDING', NULL, 40 * 24),
    (6, 'kasuni@example.com', NULL,
     'samples/rx-demo-6.png', 'blood-pressure-rx.png', 'image/png', 55096,
     'APPROVED', NULL, 96),
    (7, 'tharindu@example.com', 'urgent pls',
     'samples/rx-demo-7.png', 'IMG_beach_mirissa.png', 'image/png', 17333,
     'REJECTED', 'This is a holiday photo, not a prescription. Please upload the prescription from your doctor.', 22)
) AS v (sort_order, email, customer_note, file_key, original_file_name, content_type, file_size,
        status, pharmacist_note, hours_ago)
JOIN users u ON u.email = v.email
CROSS JOIN (SELECT id FROM users WHERE email = 'pharmacist@medisys.lk') ph
ORDER BY v.sort_order;

-- The medicines the pharmacist listed (price copied from the catalog).
INSERT INTO prescription_items (prescription_id, medicine_id, quantity, dosage_instructions, unit_price)
SELECT p.id, m.id, v.quantity, v.dosage, m.price
FROM (VALUES
    ('samples/rx-demo-4.pdf', 'Metformin', 5, '1 tablet twice daily with meals'),
    ('samples/rx-demo-4.pdf', 'Panadol',   2, '1-2 tablets when needed for pain, at most 8 in 24 hours'),
    ('samples/rx-demo-6.png', 'Losartan',  3, '1 tablet every morning')
) AS v (file_key, medicine_name, quantity, dosage)
JOIN prescriptions p ON p.file_key = v.file_key
JOIN medicines m ON m.name = v.medicine_name;

-- RX 6 was paid: order 2 (see the module 02 section) paid for it.
UPDATE prescriptions SET order_id = 2 WHERE file_key = 'samples/rx-demo-6.png';

-- The notifications those decisions sent.
INSERT INTO notifications (user_id, message, link, is_read, created_at)
SELECT u.id, v.message, v.link, v.is_read = 1, (LOCALTIMESTAMP - v.hours_ago * INTERVAL '1 hour')
FROM (VALUES
    ('nimal@example.com', 'Your prescription RX-000004 was approved: 2 medicines, total Rs. 100.00. See how to use them and pay.',
     '/prescriptions', 0, 46),
    ('kasuni@example.com', 'Please upload a new copy of prescription RX-000002: The photo is too blurry to read the dosage. Please take a clear photo in good light.',
     '/prescriptions', 0, 28),
    ('kasuni@example.com', 'Your prescription RX-000003 was rejected: The prescription is not signed by the doctor.',
     '/prescriptions', 1, 70),
    ('kasuni@example.com', 'Thank you! Order ORD-000002 (Rs. 384.00) was placed and paid. Payment reference PAY-20260913-K7Q2XD.',
     '/orders/view?id=2', 1, 90),
    ('kasuni@example.com', 'Your order ORD-000002 has left the pharmacy with Ruwan Jayasinghe.',
     '/deliveries/track?orderId=2', 1, 85),
    ('nimal@example.com', 'Order ORD-000001 was delivered. Thank you for shopping with MediSys!',
     '/orders/view?id=1', 1, 127)
) AS v (email, message, link, is_read, hours_ago)
JOIN users u ON u.email = v.email;
-- =================================================================
-- Module 06 - Delivery Tracking and Notification
-- =================================================================
-- One delivery per order (the app creates it together with the order):
--   ORD 1  DELIVERED          by Kamal
--   ORD 2  OUT_FOR_DELIVERY   by Ruwan - 2nd attempt after "nobody at home", running late
--   ORD 3  PENDING            no rider yet, not packed   -> admin assigns a rider
--   ORD 4  CANCELLED          the order was cancelled
--   ORD 5  PENDING            Ruwan, order packed         -> Ruwan can pick it up
-- A new order is expected 2 days after it was placed (DeliveryDAO.DELIVERY_DAYS).

INSERT INTO deliveries (order_id, staff_id, status, attempts, estimated_date, delivered_at, created_at, updated_at)
SELECT o.id, s.id, v.status, v.attempts, CAST((o.created_at + INTERVAL '2 days') AS DATE),
       CASE WHEN v.status = 'DELIVERED' THEN (o.created_at + (v.updated_after) * INTERVAL '1 minute') END,
       o.created_at, (o.created_at + (v.updated_after) * INTERVAL '1 minute')
FROM (VALUES
    (1, 'rider2@medisys.lk',   'DELIVERED',        1, 1000),
    (2, 'delivery@medisys.lk', 'OUT_FOR_DELIVERY', 2, 5340),
    (3, NULL,                   'PENDING',          0, 0),
    (4, NULL,                   'CANCELLED',        0, 20),
    (5, 'delivery@medisys.lk', 'PENDING',          0, 60)
) AS v (order_id, rider_email, status, attempts, updated_after)
JOIN orders o ON o.id = v.order_id
LEFT JOIN users s ON s.email = v.rider_email
ORDER BY v.order_id;          -- so delivery N belongs to order N (the links below rely on it)

-- The tracking history (minutes after the order was placed).
INSERT INTO delivery_updates (delivery_id, status, note, updated_by, created_at)
SELECT d.id, v.status, v.note, u.id, (o.created_at + (v.minutes_after) * INTERVAL '1 minute')
FROM (VALUES
    (1, 'PENDING',          'Order received. We are preparing your parcel.', NULL,                   0),
    (1, 'PENDING',          'Rider: Kamal Perera, 0771234599',               'admin@medisys.lk',    100),
    (1, 'DISPATCHED',       NULL,                                             'rider2@medisys.lk',   600),
    (1, 'OUT_FOR_DELIVERY', NULL,                                             'rider2@medisys.lk',   620),
    (1, 'DELIVERED',        'Handed to the customer',                        'rider2@medisys.lk',   1000),
    (2, 'PENDING',          'Order received. We are preparing your parcel.', NULL,                   0),
    (2, 'PENDING',          'Rider: Ruwan Jayasinghe, 0751112223',           'admin@medisys.lk',    120),
    (2, 'DISPATCHED',       NULL,                                             'delivery@medisys.lk', 300),
    (2, 'OUT_FOR_DELIVERY', NULL,                                             'delivery@medisys.lk', 320),
    (2, 'FAILED',           'Nobody at home and the phone was not answered', 'delivery@medisys.lk', 420),
    (2, 'OUT_FOR_DELIVERY', 'Trying again today',                            'delivery@medisys.lk', 5340),
    (3, 'PENDING',          'Order received. We are preparing your parcel.', NULL,                   0),
    (4, 'PENDING',          'Order received. We are preparing your parcel.', NULL,                   0),
    (4, 'CANCELLED',        'Cancelled by the customer: Ordered by mistake', NULL,                   20),
    (5, 'PENDING',          'Order received. We are preparing your parcel.', NULL,                   0),
    (5, 'PENDING',          'Rider: Ruwan Jayasinghe, 0751112223',           'admin@medisys.lk',    60)
) AS v (order_id, status, note, by_email, minutes_after)
JOIN deliveries d ON d.order_id = v.order_id
JOIN orders o ON o.id = v.order_id
LEFT JOIN users u ON u.email = v.by_email;

-- The notifications those steps sent.
INSERT INTO notifications (user_id, message, link, is_read, created_at)
SELECT u.id, v.message, v.link, v.is_read = 1, (LOCALTIMESTAMP - v.minutes_ago * INTERVAL '1 minute')
FROM (VALUES
    ('kasuni@example.com',  'We could not deliver order ORD-000002: Nobody at home and the phone was not answered. Our rider will try again soon.',
     '/deliveries/track?orderId=2', 1, 90 * 60 - 420),
    ('kasuni@example.com',  'Ruwan Jayasinghe is trying again to deliver your order ORD-000002 today. Please keep your phone nearby. Note: Trying again today',
     '/deliveries/track?orderId=2', 0, 60),
    ('delivery@medisys.lk', 'New delivery for you: ORD-000005 to 45 Lake Drive, Kandy.',
     '/staff/deliveries/view?id=5', 0, 26 * 60 - 60),
    ('delivery@medisys.lk', 'New delivery for you: ORD-000002 to 45 Lake Drive, Kandy.',
     '/staff/deliveries/view?id=2', 1, 90 * 60 - 120)
) AS v (email, message, link, is_read, minutes_ago)
JOIN users u ON u.email = v.email;
-- =================================================================
-- Module 04 - Reports and Analytics
-- =================================================================
-- The reports read the other modules' tables, so what they need is history.
-- Three more customers placed 30 shop orders over the last 75 days: 27 were
-- delivered (4 late, 4 after a failed first attempt) and 3 were cancelled.
-- They are new customers so the demo accounts above keep their own orders.
-- Order ids 6-35 belong to these rows (5 + n), and delivery N belongs to order N.

INSERT INTO users (full_name, email, phone, whatsapp, nic, date_of_birth, address, password_hash, role, created_at)
SELECT v.full_name, v.email, v.phone, v.phone, v.nic, CAST(v.dob AS DATE), v.address,
       'pbkdf2$120000$UAbhe9vXLT/vc3JkTryxAA==$eg07Xv88WmKfyx+tKFxuEoYQXOwL6Iow+YbKqHoympU=', 'CUSTOMER',
       (LOCALTIMESTAMP - INTERVAL '80 days')
FROM (VALUES
    (1, 'Amaya Rodrigo',     'amaya@example.com',  '0772345678', '199256712345', '1992-03-07', '17 Galle Road, Dehiwala'),
    (2, 'Dilan Kumara',      'dilan@example.com',  '0713456789', '880234567V',   '1988-01-23', '5 Hill Street, Kurunegala'),
    (3, 'Ishara Senanayake', 'ishara@example.com', '0764567890', '199581234567', '1995-11-04', '92 Lake Road, Nugegoda')
) AS v (sort_order, full_name, email, phone, nic, dob, address)
ORDER BY v.sort_order;

-- One row per historical order. late = delivered after the expected day,
-- failed = the first delivery attempt failed.
-- (a TEMP table only lives until this script ends)
CREATE TEMP TABLE hist (
    n INT, email VARCHAR(150), days_ago INT, hour_of_day INT, status VARCHAR(15),
    med1 VARCHAR(150), q1 INT, med2 VARCHAR(150) NULL, q2 INT NULL, late INT, failed INT,
    placed_at TIMESTAMP NULL, delivered_at TIMESTAMP NULL
);

INSERT INTO hist (n, email, days_ago, hour_of_day, status, med1, q1, med2, q2, late, failed) VALUES
    ( 1, 'amaya@example.com',  75, 10, 'DELIVERED', 'Panadol',              4, 'Vitamin C',            2,    0, 0),
    ( 2, 'dilan@example.com',  73, 15, 'DELIVERED', 'Benadryl Cough Syrup', 1, 'Piriton',              3,    0, 0),
    ( 3, 'ishara@example.com', 70, 11, 'DELIVERED', 'Dettol Antiseptic',    1, NULL,                    NULL, 0, 0),
    ( 4, 'amaya@example.com',  68, 18, 'CANCELLED', 'Brufen',               2, NULL,                    NULL, 0, 0),
    ( 5, 'dilan@example.com',  66,  9, 'DELIVERED', 'Hydrocortisone Cream', 1, 'Panadol',              2,    1, 0),
    ( 6, 'ishara@example.com', 63, 14, 'DELIVERED', 'Vitamin C',            5, NULL,                    NULL, 0, 0),
    ( 7, 'amaya@example.com',  60, 16, 'DELIVERED', 'Benadryl Cough Syrup', 2, 'Dettol Antiseptic',    1,    0, 1),
    ( 8, 'dilan@example.com',  57, 12, 'DELIVERED', 'Panadol',              6, 'Piriton',              2,    0, 0),
    ( 9, 'ishara@example.com', 54, 19, 'DELIVERED', 'Dettol Antiseptic',    2, 'Vitamin C',            3,    0, 0),
    (10, 'amaya@example.com',  51, 10, 'DELIVERED', 'Piriton',              4, NULL,                    NULL, 0, 0),
    (11, 'dilan@example.com',  48, 13, 'CANCELLED', 'Hydrocortisone Cream', 2, NULL,                    NULL, 0, 0),
    (12, 'ishara@example.com', 45, 17, 'DELIVERED', 'Benadryl Cough Syrup', 1, 'Panadol',              2,    1, 1),
    (13, 'amaya@example.com',  42, 11, 'DELIVERED', 'Vitamin C',            4, 'Brufen',               1,    0, 0),
    (14, 'dilan@example.com',  39, 15, 'DELIVERED', 'Dettol Antiseptic',    1, 'Hydrocortisone Cream', 1,    0, 0),
    (15, 'ishara@example.com', 36,  9, 'DELIVERED', 'Panadol',              3, NULL,                    NULL, 0, 0),
    (16, 'amaya@example.com',  33, 20, 'DELIVERED', 'Benadryl Cough Syrup', 3, NULL,                    NULL, 0, 0),
    (17, 'dilan@example.com',  30, 12, 'DELIVERED', 'Piriton',              5, 'Vitamin C',            2,    0, 0),
    (18, 'ishara@example.com', 27, 14, 'DELIVERED', 'Dettol Antiseptic',    4, NULL,                    NULL, 0, 0),
    (19, 'amaya@example.com',  25, 10, 'DELIVERED', 'Panadol',              2, 'Hydrocortisone Cream', 1,    1, 0),
    (20, 'dilan@example.com',  22, 16, 'CANCELLED', 'Benadryl Cough Syrup', 1, NULL,                    NULL, 0, 0),
    (21, 'ishara@example.com', 20, 11, 'DELIVERED', 'Vitamin C',            6, 'Piriton',              2,    0, 0),
    (22, 'amaya@example.com',  18, 18, 'DELIVERED', 'Dettol Antiseptic',    1, 'Panadol',              4,    0, 0),
    (23, 'dilan@example.com',  15, 13, 'DELIVERED', 'Benadryl Cough Syrup', 2, 'Hydrocortisone Cream', 1,    0, 1),
    (24, 'ishara@example.com', 13,  9, 'DELIVERED', 'Brufen',               2, 'Vitamin C',            1,    0, 0),
    (25, 'amaya@example.com',  11, 15, 'DELIVERED', 'Panadol',              5, NULL,                    NULL, 0, 0),
    (26, 'dilan@example.com',   9, 17, 'DELIVERED', 'Dettol Antiseptic',    2, 'Benadryl Cough Syrup', 1,    1, 0),
    (27, 'ishara@example.com',  7, 12, 'DELIVERED', 'Hydrocortisone Cream', 2, NULL,                    NULL, 0, 0),
    (28, 'amaya@example.com',   6, 10, 'DELIVERED', 'Vitamin C',            3, 'Piriton',              3,    0, 0),
    (29, 'dilan@example.com',   4, 14, 'DELIVERED', 'Panadol',              3, 'Dettol Antiseptic',    1,    0, 0),
    (30, 'ishara@example.com',  3, 19, 'DELIVERED', 'Benadryl Cough Syrup', 1, 'Vitamin C',            2,    0, 0);

-- Placed at hour_of_day on that day; delivered the next day, or 3 days later when late.
UPDATE hist SET placed_at = (CURRENT_DATE - days_ago) + hour_of_day * INTERVAL '1 hour';
UPDATE hist SET delivered_at = (placed_at + (CASE WHEN late = 1 THEN 76 ELSE 26 END) * INTERVAL '1 hour') WHERE status = 'DELIVERED';

-- The orders (subtotal from the catalog prices; delivery Rs. 300, free from Rs. 2,500).
INSERT INTO orders (user_id, source, status, subtotal, delivery_fee, total, delivery_name, delivery_address,
                    delivery_phone, cancel_reason, created_at, updated_at)
SELECT u.id, 'CART', h.status, t.subtotal, f.fee, t.subtotal + f.fee, u.full_name, u.address, u.phone,
       CASE WHEN h.status = 'CANCELLED' THEN 'Cancelled by the customer: Ordered by mistake' END,
       h.placed_at, COALESCE(h.delivered_at, (h.placed_at + INTERVAL '30 minutes'))
FROM hist h
JOIN users u ON u.email = h.email
JOIN medicines m1 ON m1.name = h.med1
LEFT JOIN medicines m2 ON m2.name = h.med2
CROSS JOIN LATERAL (SELECT m1.price * h.q1 + COALESCE(m2.price * h.q2, 0) AS subtotal) t
CROSS JOIN LATERAL (SELECT CASE WHEN t.subtotal >= 2500 THEN 0.00 ELSE 300.00 END AS fee) f
ORDER BY h.n;

INSERT INTO order_items (order_id, medicine_id, medicine_name, dosage_form, unit_price, quantity, dosage_instructions)
SELECT 5 + h.n, m.id, CONCAT(m.name, ' ', m.strength), m.dosage_form, m.price, l.qty, NULL
FROM hist h
CROSS JOIN LATERAL (VALUES (h.med1, h.q1), (h.med2, h.q2)) AS l (med, qty)
JOIN medicines m ON m.name = l.med;

INSERT INTO payments (order_id, amount, card_last4, reference, status, paid_at, refunded_at)
SELECT o.id, o.total, '4242', CONCAT('PAY-HIST-', RIGHT(CONCAT('000', h.n), 3)),
       CASE WHEN h.status = 'CANCELLED' THEN 'REFUNDED' ELSE 'PAID' END, h.placed_at,
       CASE WHEN h.status = 'CANCELLED' THEN (h.placed_at + INTERVAL '30 minutes') END
FROM hist h JOIN orders o ON o.id = 5 + h.n;

-- Order history (the module 02 timeline). by_admin = 1: the admin made the change.
INSERT INTO order_status_history (order_id, status, note, changed_by, changed_at)
SELECT 5 + h.n, s.status, s.note, CASE WHEN s.by_admin = 1 THEN a.id END, s.at
FROM hist h
CROSS JOIN (SELECT id FROM users WHERE email = 'admin@medisys.lk') a
CROSS JOIN LATERAL (VALUES
    ('PAID',       'Order placed and paid',                         0,      h.placed_at,                      1),
    ('PROCESSING', NULL,                                             1,      (h.placed_at + INTERVAL '60 minutes'),  CASE WHEN h.status = 'DELIVERED' THEN 1 ELSE 0 END),
    ('SHIPPED',    'Picked up by the rider',                        1,      (h.placed_at + INTERVAL '180 minutes'), CASE WHEN h.status = 'DELIVERED' THEN 1 ELSE 0 END),
    ('DELIVERED',  NULL,                                             1,      h.delivered_at,                    CASE WHEN h.status = 'DELIVERED' THEN 1 ELSE 0 END),
    ('CANCELLED',  'Cancelled by the customer: Ordered by mistake', 0,      (h.placed_at + INTERVAL '30 minutes'),  CASE WHEN h.status = 'CANCELLED' THEN 1 ELSE 0 END)
) AS s (status, note, by_admin, at, wanted)
WHERE s.wanted = 1;

-- Deliveries (module 06): odd orders went with Ruwan, even ones with Kamal.
INSERT INTO deliveries (order_id, staff_id, status, attempts, estimated_date, delivered_at, created_at, updated_at)
SELECT 5 + h.n,
       CASE WHEN h.status = 'CANCELLED' THEN NULL
            WHEN h.n % 2 = 1 THEN (SELECT id FROM users WHERE email = 'delivery@medisys.lk')
            ELSE (SELECT id FROM users WHERE email = 'rider2@medisys.lk') END,
       h.status, CASE WHEN h.status = 'CANCELLED' THEN 0 WHEN h.failed = 1 THEN 2 ELSE 1 END,
       CAST((h.placed_at + INTERVAL '2 days') AS DATE), h.delivered_at,
       h.placed_at, COALESCE(h.delivered_at, (h.placed_at + INTERVAL '30 minutes'))
FROM hist h
ORDER BY h.n;

INSERT INTO delivery_updates (delivery_id, status, note, updated_by, created_at)
SELECT d.id, s.status, s.note, CASE WHEN s.by_rider = 1 THEN d.staff_id END, s.at
FROM hist h
JOIN deliveries d ON d.order_id = 5 + h.n
CROSS JOIN LATERAL (VALUES
    ('PENDING',          'Order received. We are preparing your parcel.', 0, h.placed_at,                      1),
    ('DISPATCHED',       NULL,                                             1, (h.placed_at + INTERVAL '180 minutes'), CASE WHEN h.status = 'DELIVERED' THEN 1 ELSE 0 END),
    ('OUT_FOR_DELIVERY', NULL,                                             1, (h.placed_at + INTERVAL '200 minutes'), CASE WHEN h.status = 'DELIVERED' THEN 1 ELSE 0 END),
    ('FAILED',           'Nobody at home',                                1, (h.placed_at + INTERVAL '260 minutes'), CASE WHEN h.failed = 1 THEN 1 ELSE 0 END),
    ('OUT_FOR_DELIVERY', 'Trying again',                                  1, (h.delivered_at - INTERVAL '60 minutes'), CASE WHEN h.failed = 1 THEN 1 ELSE 0 END),
    ('DELIVERED',        'Handed to the customer',                        1, h.delivered_at,                    CASE WHEN h.status = 'DELIVERED' THEN 1 ELSE 0 END),
    ('CANCELLED',        'Cancelled by the customer: Ordered by mistake', 0, (h.placed_at + INTERVAL '30 minutes'), CASE WHEN h.status = 'CANCELLED' THEN 1 ELSE 0 END)
) AS s (status, note, by_rider, at, wanted)
WHERE s.wanted = 1;

DROP TABLE hist;
