# PharmaCare - Drug Ordering System

A full-stack, role-based web application for ordering medicines, uploading medical prescriptions, and managing pharmacy inventory.

## 🚀 Features

* **Role-Based Access:** Secure JWT authentication for Customers, Pharmacists, and Admins.
* **Shopping & Cart:** Browse the medicine catalog, search for specific drugs, and manage a shopping cart.
* **Order Checkout:** Place orders with dynamic total calculations and delivery fees.
* **Prescription Management:** Customers can securely upload prescription files (images/PDFs) for Pharmacist review and approval.
* **Inventory Management:** Admins/Pharmacists can add, edit, and discontinue medicine stock.
* **Delivery Tracking:** Admins can push delivery status updates, triggering automated in-app notifications for customers.

## 🛠 Tech Stack

* **Frontend:** HTML5, Vanilla JavaScript, Bootstrap 5 CSS Framework
* **Backend:** Node.js, Express.js, Multer (File Uploads), JSON Web Tokens (JWT), bcryptjs
* **Database:** Microsoft SQL Server 2022 (Docker containerized)

## ⚙️ How to Run the Project

### 1. Database Setup
1. Ensure Docker is running.
2. Execute the `schema.sql` file located in the `database` folder against your MSSQL instance to generate all required tables.

### 2. Backend Setup
1. Navigate to the backend directory: `cd backend`
2. Install the required Node modules: `npm install`
3. Ensure you have a `.env` file configured with your DB credentials and `JWT_SECRET`.
4. Start the server: `node server.js`
5. The API will be available at `http://localhost:3000`.

### 3. Frontend Setup
1. The frontend requires no build steps or bundlers.
2. Simply open `frontend/index.html` in any modern web browser to access the application.
3. Register a new account to begin!
