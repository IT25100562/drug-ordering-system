CREATE TABLE prescriptions (
                               id INT IDENTITY(1,1) PRIMARY KEY,
                               user_id INT NOT NULL,
                               file_path VARCHAR(255) NOT NULL,
                               status VARCHAR(20) DEFAULT 'pending',
                               uploaded_at DATETIME DEFAULT GETDATE()
);