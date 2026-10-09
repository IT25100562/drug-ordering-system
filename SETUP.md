# Running MediSys on your laptop

You need: **JDK 17 or newer**, **IntelliJ IDEA** (it includes Maven), **Apache Tomcat 11**,
and a database. Pick one database option below.

## 1. Get the code

```
git clone https://github.com/IT25100562/drug-ordering-system.git
```

Open the folder in IntelliJ and let it load the Maven project.

## 2. Database

**Option A: use the team's Supabase database (easiest).** Ask the team lead for the
database password, then create `src/main/resources/db.properties`:

```
db.url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require
db.user=postgres.duzpvyrcxqyneatmpnfc
db.password=<the password>
```

> Careful: this is the **live** database. Your test orders appear on the live site.

**Option B: your own PostgreSQL.** Install PostgreSQL 16 (password for `postgres`:
`postgres`), then in a terminal from the project folder:

```
psql -U postgres -c "CREATE DATABASE medisys"
psql -U postgres -d medisys -f database/schema.sql -f database/sample-data.sql
```

Copy `src/main/resources/db.properties.example` to `db.properties` (it already points to
`localhost:5432/medisys`). Run the two scripts again whenever you want fresh demo data:
`schema.sql` **drops every table first**.

## 3. File storage (optional)

Without settings, uploaded files go to `<your home folder>/medisys-uploads`. To use
Cloudinary like the live site, copy `app.properties.example` to `app.properties` and set
`cloudinary.url` (Cloudinary dashboard, "API environment variable").

## 4. Run it

In IntelliJ: *Run → Edit Configurations → + → Tomcat Server → Local* (Tomcat 11). On the
**Deployment** tab add the artifact `medisys:war exploded` and set the context path to `/`.
Run it and open <http://localhost:8080/>.

The console shows `[MediSys] Database connected` and `[MediSys] File storage: ...` when the
settings are right.

Without IntelliJ:

```
mvn clean package
copy target\medisys.war <tomcat>\webapps\ROOT.war
<tomcat>\bin\catalina.bat run
```

Or with Docker (the same image as the live site):

```
docker build -t medisys .
docker run -p 8080:8080 -e DB_URL=... -e DB_USER=... -e DB_PASSWORD=... medisys
```

## 5. Try every module

Log in with the demo accounts in `README.md` and follow the **Demo** line of your module in
`VIVA-GUIDE.md`.

## Problems?

| You see | Fix |
|---------|-----|
| `DATABASE NOT CONNECTED` in the console | Check `db.properties`; for Supabase use the **Session pooler** host and the user `postgres.<ref>` |
| Every page says something went wrong | The tables are missing: run `schema.sql` and `sample-data.sql` |
| Product photos are missing | Normal on a new database until the app has started once (it copies the demo photos) |
| Port 8080 is busy | Stop the other Tomcat, or change the port in Tomcat's `conf/server.xml` |
