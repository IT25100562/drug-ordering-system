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