CREATE TABLE prescriptions (
                               id INT IDENTITY(1,1) PRIMARY KEY,
                               user_id INT NOT NULL,
                               file_path VARCHAR(255) NOT NULL,
                               status VARCHAR(20) DEFAULT 'pending',
                               uploaded_at DATETIME DEFAULT GETDATE()
);

CREATE TABLE medicines (
                           id INT IDENTITY(1,1) PRIMARY KEY,
                           name VARCHAR(150) NOT NULL,
                           category VARCHAR(100) NOT NULL,
                           price DECIMAL(10,2) NOT NULL,
                           stock_quantity INT DEFAULT 0,
                           description VARCHAR(MAX),
    is_discontinued BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE()
);

CREATE TABLE users (
                       id INT IDENTITY(1,1) PRIMARY KEY,
                       full_name VARCHAR(100) NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(20) DEFAULT 'customer', -- customer, pharmacist, admin
                       created_at DATETIME DEFAULT GETDATE()
);