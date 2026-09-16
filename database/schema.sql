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

CREATE TABLE users (
                       id INT IDENTITY(1,1) PRIMARY KEY,
                       full_name VARCHAR(100) NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(20) DEFAULT 'customer',
                       created_at DATETIME DEFAULT GETDATE()
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

CREATE TABLE cart_items (
                            id INT IDENTITY(1,1) PRIMARY KEY,
                            user_id INT NOT NULL,
                            medicine_id INT NOT NULL,
                            quantity INT DEFAULT 1,
                            added_at DATETIME DEFAULT GETDATE(),
                            FOREIGN KEY (user_id) REFERENCES users(id),
                            FOREIGN KEY (medicine_id) REFERENCES medicines(id)
);

CREATE TABLE orders (
                        id INT IDENTITY(1,1) PRIMARY KEY,
                        user_id INT NOT NULL,
                        total_amount DECIMAL(10,2) NOT NULL,
                        delivery_fee DECIMAL(10,2) DEFAULT 5.00,
                        status VARCHAR(50) DEFAULT 'Pending', -- Pending, Paid, Dispatched, Delivered
                        created_at DATETIME DEFAULT GETDATE(),
                        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
                             id INT IDENTITY(1,1) PRIMARY KEY,
                             order_id INT NOT NULL,
                             medicine_id INT NOT NULL,
                             quantity INT NOT NULL,
                             price_at_purchase DECIMAL(10,2) NOT NULL,
                             FOREIGN KEY (order_id) REFERENCES orders(id),
                             FOREIGN KEY (medicine_id) REFERENCES medicines(id)
);