# Vercel front door

Vercel cannot run Java, so the app itself runs on Render (see `../render.yaml`).
This folder is a tiny Vercel project that forwards every request to Render, so the
site is reached at **https://&lt;your-project&gt;.vercel.app**.

- In Vercel: *Add New → Project → this repo*, set **Root Directory** to `vercel`, deploy.
- If your Render address is not `https://medisys.onrender.com`, change it in `vercel.json`.

Nothing else lives here: all code is in `src/`.
