-- Add delivery_otp to deliveries table
ALTER TABLE deliveries 
ADD delivery_otp VARCHAR(255) NULL;

-- Add is_active flag for soft deletion to deliveries table
ALTER TABLE deliveries 
ADD is_active BIT DEFAULT 1 NOT NULL;

-- Add is_active flag for soft deletion to notifications table
ALTER TABLE notifications 
ADD is_active BIT DEFAULT 1 NOT NULL;
