require('dotenv').config();
const express = require('express');
const cors = require('cors');
const sql = require('mssql');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const app = express();

app.use(cors());
app.use(express.json());

// Auto-create uploads directory if missing
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}
app.use('/uploads', express.static(uploadDir));

// MSSQL Database Configuration
const dbConfig = {
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    server: process.env.DB_SERVER,
    pool: { max: 10, min: 0, idleTimeoutMillis: 30000 },
    options: {
        encrypt: false,
        trustServerCertificate: true,
        connectTimeout: 15000,
        requestTimeout: 15000
    }
};

// Connection Helper with Auto-Reconnect
let pool = null;
async function getDbPool() {
    if (pool && pool.connected) return pool;
    try {
        pool = await new sql.ConnectionPool(dbConfig).connect();
        console.log('[DB] Connected to MSSQL database successfully.');
        return pool;
    } catch (err) {
        pool = null;
        console.error('[DB ERROR] Database connection failed:', err.message);
        throw err;
    }
}

// Storage Configuration
const storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, uploadDir),
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, uniqueSuffix + path.extname(file.originalname));
    }
});
const upload = multer({ storage });

// ==========================================
// --- USER & ROLE MANAGEMENT ENDPOINTS ---
// ==========================================
const JWT_SECRET = process.env.JWT_SECRET || 'fallback_super_secret_key_123';

// 1. REGISTER USER
app.post('/api/users/register', async (req, res) => {
    console.log('[POST] /api/users/register - Registering new user');
    try {
        const { full_name, email, password, role } = req.body;
        if (!full_name || !email || !password) return res.status(400).json({ error: 'Missing required fields' });

        const salt = await bcrypt.genSalt(10);
        const password_hash = await bcrypt.hash(password, salt);
        const userRole = role || 'customer';

        const db = await getDbPool();
        await db.request()
            .input('full_name', sql.VarChar, full_name)
            .input('email', sql.VarChar, email)
            .input('password_hash', sql.VarChar, password_hash)
            .input('role', sql.VarChar, userRole)
            .query(`INSERT INTO users (full_name, email, password_hash, role) 
                    VALUES (@full_name, @email, @password_hash, @role)`);

        res.status(201).json({ message: 'User registered successfully' });
    } catch (err) {
        console.error('[ERROR] Registration failed:', err.message);
        res.status(500).json({ error: 'Registration failed. Email might already exist.' });
    }
});

// 2. LOGIN USER
app.post('/api/users/login', async (req, res) => {
    console.log('[POST] /api/users/login - Authenticating user');
    try {
        const { email, password } = req.body;

        const db = await getDbPool();
        const result = await db.request()
            .input('email', sql.VarChar, email)
            .query('SELECT * FROM users WHERE email = @email');

        const user = result.recordset[0];
        if (!user) return res.status(400).json({ error: 'Invalid email or password' });

        const validPassword = await bcrypt.compare(password, user.password_hash);
        if (!validPassword) return res.status(400).json({ error: 'Invalid email or password' });

        const token = jwt.sign({ id: user.id, role: user.role }, JWT_SECRET, { expiresIn: '2h' });

        res.json({
            message: 'Login successful',
            token,
            user: { id: user.id, full_name: user.full_name, role: user.role }
        });
    } catch (err) {
        console.error('[ERROR] Login failed:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// --- PRESCRIPTION ENDPOINTS ---
// ==========================================
app.post('/api/prescriptions/upload', upload.single('prescription'), async (req, res) => {
    console.log('[POST] /api/prescriptions/upload - Request received');
    try {
        if (!req.file) return res.status(400).json({ error: 'No file uploaded' });

        const db = await getDbPool();
        const userId = req.body.user_id || 1;

        await db.request()
            .input('user_id', sql.Int, userId)
            .input('file_path', sql.VarChar, req.file.filename)
            .query('INSERT INTO prescriptions (user_id, file_path) VALUES (@user_id, @file_path)');

        console.log('[INFO] File saved to database:', req.file.filename);
        res.status(201).json({ message: 'Prescription uploaded successfully', filePath: req.file.filename });
    } catch (err) {
        console.error('[ERROR] Prescription upload failed:', err.message);
        res.status(500).json({ error: err.message });
    }
});

app.get('/api/prescriptions', async (req, res) => {
    console.log('[GET] /api/prescriptions - Fetching records');
    try {
        const db = await getDbPool();
        const result = await db.request().query('SELECT * FROM prescriptions ORDER BY uploaded_at DESC');
        console.log(`[INFO] Retrieved ${result.recordset.length} record(s)`);
        res.json(result.recordset);
    } catch (err) {
        console.error('[ERROR] Failed to fetch prescriptions:', err.message);
        res.status(500).json({ error: err.message });
    }
});

app.patch('/api/prescriptions/:id/status', async (req, res) => {
    console.log(`[PATCH] /api/prescriptions/${req.params.id}/status - Updating status`);
    try {
        const { id } = req.params;
        const { status } = req.body;

        if (!['approved', 'rejected', 'request_fix', 'pending'].includes(status)) {
            return res.status(400).json({ error: 'Invalid status update. Allowed: approved, rejected, request_fix, pending' });
        }

        const db = await getDbPool();
        await db.request()
            .input('id', sql.Int, id)
            .input('status', sql.VarChar, status)
            .query('UPDATE prescriptions SET status = @status WHERE id = @id');
        res.json({ message: `Prescription status updated to ${status}` });
    } catch (err) {
        console.error('[ERROR] Status update failed:', err.message);
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/prescriptions/expired', async (req, res) => {
    console.log('[DELETE] /api/prescriptions/expired - Cleaning up old records');
    try {
        const db = await getDbPool();
        const result = await db.request()
            .query("DELETE FROM prescriptions WHERE status = 'expired' OR uploaded_at < DATEADD(day, -30, GETDATE())");
        res.json({ message: 'Expired prescriptions deleted', rowsAffected: result.rowsAffected[0] });
    } catch (err) {
        console.error('[ERROR] Cleanup failed:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// --- MEDICINE INVENTORY ENDPOINTS ---
// ==========================================
app.post('/api/medicines', async (req, res) => {
    console.log('[POST] /api/medicines - Adding item');
    try {
        const { name, category, price, stock_quantity, description } = req.body;
        if (!name || !price) return res.status(400).json({ error: 'Name and price are required' });

        const db = await getDbPool();
        await db.request()
            .input('name', sql.VarChar, name)
            .input('category', sql.VarChar, category || 'General')
            .input('price', sql.Decimal(10, 2), price)
            .input('stock_quantity', sql.Int, stock_quantity || 0)
            .input('description', sql.VarChar, description || '')
            .query(`INSERT INTO medicines (name, category, price, stock_quantity, description) 
                    VALUES (@name, @category, @price, @stock_quantity, @description)`);

        res.status(201).json({ message: 'Medicine added successfully' });
    } catch (err) {
        console.error('[ERROR] Add medicine failed:', err.message);
        res.status(500).json({ error: err.message });
    }
});

app.get('/api/medicines', async (req, res) => {
    console.log('[GET] /api/medicines - Fetching inventory');
    try {
        const db = await getDbPool();
        const result = await db.request()
            .query('SELECT * FROM medicines WHERE is_discontinued = 0 ORDER BY name ASC');
        console.log(`[INFO] Retrieved ${result.recordset.length} medicine(s)`);
        res.json(result.recordset);
    } catch (err) {
        console.error('[ERROR] Fetch inventory failed:', err.message);
        res.status(500).json({ error: err.message });
    }
});

app.put('/api/medicines/:id', async (req, res) => {
    console.log(`[PUT] /api/medicines/${req.params.id} - Updating item`);
    try {
        const { id } = req.params;
        const { name, category, price, stock_quantity, description } = req.body;

        const db = await getDbPool();
        await db.request()
            .input('id', sql.Int, id)
            .input('name', sql.VarChar, name)
            .input('category', sql.VarChar, category)
            .input('price', sql.Decimal(10, 2), price)
            .input('stock_quantity', sql.Int, stock_quantity)
            .input('description', sql.VarChar, description)
            .query(`UPDATE medicines 
                    SET name = @name, category = @category, price = @price, 
                        stock_quantity = @stock_quantity, description = @description 
                    WHERE id = @id`);

        res.json({ message: 'Medicine updated successfully' });
    } catch (err) {
        console.error('[ERROR] Update medicine failed:', err.message);
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/medicines/:id', async (req, res) => {
    console.log(`[DELETE] /api/medicines/${req.params.id} - Discontinuing item`);
    try {
        const { id } = req.params;
        const db = await getDbPool();
        await db.request()
            .input('id', sql.Int, id)
            .query('UPDATE medicines SET is_discontinued = 1 WHERE id = @id');

        res.json({ message: 'Medicine marked as discontinued' });
    } catch (err) {
        console.error('[ERROR] Discontinue medicine failed:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// START SERVER
const PORT = process.env.PORT || 3000;
app.listen(PORT, async () => {
    console.log(`[SERVER] Running on http://localhost:${PORT}`);
    try { await getDbPool(); } catch (e) {}
});