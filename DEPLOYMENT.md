# How the live system is hosted

```
 Browser ──> https://<name>.vercel.app ──(Vercel forwards)──> Render: Docker + Tomcat 11 + our WAR
                                                                 │                    │
                                                    JDBC (SSL)   ▼                    ▼  HTTPS API
                                                   Supabase PostgreSQL         Cloudinary (files + image CDN)
```

Vercel cannot run Java, so the Java app runs on **Render** and Vercel only forwards
requests to it. That gives the site a `vercel.app` address while staying a Java web app,
as the SE2030 spec requires. All four services have free plans.

## 1. Supabase (database)

**Already done:** project `medisys` (ref `duzpvyrcxqyneatmpnfc`, Mumbai), with the tables and
demo data loaded. Connection (Session pooler): host `aws-0-ap-south-1.pooler.supabase.com`,
port `5432`, database `postgres`, user `postgres.duzpvyrcxqyneatmpnfc`. The team lead keeps
the password.

To rebuild it from nothing: <https://supabase.com> → New project → **SQL Editor** → run
`database/schema.sql`, then `database/sample-data.sql` → **Connect** → *Session pooler*.

`schema.sql` turns on Row Level Security for every table, so Supabase's public REST API
can't read anything. Only our app (the table owner) can.

## 2. Cloudinary (uploaded files and product photos)

1. <https://cloudinary.com> → sign up → Dashboard → copy the **API environment variable**
   `cloudinary://<key>:<secret>@<cloud name>`.
2. Settings → Security → tick **Allow delivery of PDF and ZIP files** (prescriptions can be PDFs).

Nothing else to do: when the app starts it uploads the demo photos itself (folder
`medisys/`). Prescriptions and profile photos are uploaded as **authenticated** (private)
files; product photos are public and are served from Cloudinary's CDN.

## 3. Render (runs the Java app)

1. Open <https://render.com/deploy?repo=https://github.com/IT25100562/drug-ordering-system>
   (or Render → **New → Blueprint** → pick this repo) and sign in with GitHub.
   Render reads `render.yaml` and creates the `medisys` web service (Docker, free plan).
2. Fill in the secret settings it asks for:

   | Key | Value |
   |-----|-------|
   | `DB_URL` | `jdbc:postgresql://<pooler host>:5432/postgres?sslmode=require` |
   | `DB_USER` | `postgres.<project ref>` |
   | `DB_PASSWORD` | the Supabase database password |
   | `CLOUDINARY_URL` | `cloudinary://<key>:<secret>@<cloud name>` |

3. Wait for the first build (about 5 minutes). The log ends with
   `[MediSys] Database connected` and `File storage: Cloudinary (...)`.
   Note the address, e.g. `https://medisys.onrender.com`.

Every push to `main` redeploys automatically.

**Keep it awake:** the free plan sleeps after 15 idle minutes. In GitHub → Settings →
Secrets and variables → Actions → **Variables**, add `APP_URL` = the Render address. The
workflow `.github/workflows/keep-alive.yml` then opens the site every 10 minutes.

## 4. Vercel (the public address)

1. If the Render address is not `https://medisys.onrender.com`, change it in `vercel/vercel.json`
   and push.
2. <https://vercel.com> → sign in with GitHub → **Add New → Project** → this repo →
   **Root Directory: `vercel`** → Deploy.
3. Project → Settings → Domains: rename it to the address you want, e.g. `medisys-lk.vercel.app`.
   Put that address at the top of `README.md`.

## Checking the live site

- `/` shows the shop with product photos (from `res.cloudinary.com`).
- Log in as each demo user (see `README.md`) and run your module's demo.
- Render → Logs shows every error with its stack trace.

## Changing the database later

Edit `schema.sql` / `sample-data.sql`, then run both again in the Supabase SQL Editor.
**This deletes all data**, including orders placed on the live site.
