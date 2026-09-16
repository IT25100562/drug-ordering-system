/*
    MediSys - step 3 of 3: demo data for testing and the presentation.
    Shared file - agree with the team before changing it.

        sqlcmd -S localhost -E -C -d MediSysDB -i database\sample-data.sql

    Run it right after schema.sql (the tables must be empty).
    Each module adds its own demo rows in its own section.
*/

USE MediSysDB;
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
