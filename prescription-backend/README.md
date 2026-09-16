# Prescription Upload & Verification Module
**Developer:** Divisekara A W D M D M B (IT25100562)

## Description
Standalone microservice for managing user prescription uploads, administrative verifications, and status tracking.

## API Endpoints
* **`POST /api/prescriptions/upload`** - Upload prescription image/PDF (`formData`: `prescription`, `user_id`)
* **`GET /api/prescriptions`** - Retrieve list of submitted prescriptions
* **`PATCH /api/prescriptions/:id/status`** - Update status (`approved`, `rejected`, `request_fix`, `pending`)
* **`DELETE /api/prescriptions/expired`** - Purge records older than 30 days