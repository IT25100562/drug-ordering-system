# MediSys: setup guide (Windows)

This guide takes you from a new computer to MediSys running in your browser.
Plan about **45-60 minutes** the first time; most of that is downloads.

What you will install:

| # | Software | Why | Required? |
|---|----------|-----|-----------|
| 1 | Git or GitHub Desktop | get the code | yes |
| 2 | IntelliJ IDEA **Ultimate** | open, build and run the project (includes Java and Maven) | yes (recommended) |
| 3 | SQL Server 2022 **Developer** | the database | yes |
| 4 | sqlcmd | run the database scripts | yes (or use SSMS / IntelliJ) |
| 5 | Apache Tomcat **11** | the web server that runs the app | yes |
| 6 | SQL Server Management Studio (SSMS) | look at the tables | optional |

> **Use exactly these major versions.** The code uses `jakarta.servlet` (Tomcat 10/11) and Java 17+.
> Tomcat 9 will **not** work.

---

## Part A - Install the software (once per computer)

### A1. Git / GitHub Desktop
1. Download GitHub Desktop from <https://desktop.github.com> and install it.
2. Sign in with your GitHub account (the one that was added to the repository).

### A2. IntelliJ IDEA Ultimate (free for students)
Only the **Ultimate** edition can run Tomcat from inside IntelliJ.

1. Get the free student licence: <https://www.jetbrains.com/community/education/#students>.
   Apply with your **university e-mail**. The approval e-mail arrives in a few minutes.
2. Download **IntelliJ IDEA Ultimate** from <https://www.jetbrains.com/idea/download/> and install it
   (tick "Add launchers dir to PATH" and ".java" association if offered).
3. Start it and log in with the JetBrains account that has the student licence.

IntelliJ ships with a Java runtime (JBR, Java 21+) and Maven, so you do **not** need to install Java
or Maven separately for IntelliJ. (For the command-line way in Part D you need a JDK; see D0.)

> Only have the free **Community** edition? Skip the IntelliJ run steps and use Part D (command line).

### A3. SQL Server 2022 Developer (free)
1. Download from <https://www.microsoft.com/sql-server/sql-server-downloads> →
   **Developer** → *Download now*.
2. Run it and choose **Custom** (not Basic), so you can pick the authentication mode.
3. In the setup wizard:
   - *Installation* → **New SQL Server standalone installation**.
   - Edition: **Developer**.
   - Features: tick **Database Engine Services** (nothing else is needed).
   - Instance Configuration: **Default instance** (the name will be `MSSQLSERVER`).
   - Database Engine Configuration → Server Configuration:
     - Authentication mode: **Mixed Mode (SQL Server authentication and Windows authentication)**.
       Type any strong password for `sa` and keep it somewhere.
     - Click **Add Current User**.
4. Finish the installation.

**Turn on TCP/IP (port 1433)** - the app connects over the network port:

1. Open **SQL Server 2022 Configuration Manager** (Start menu → type "SQL Server 2022 Configuration Manager").
2. *SQL Server Network Configuration* → *Protocols for MSSQLSERVER* → right-click **TCP/IP** → **Enable**.
3. Right-click TCP/IP → *Properties* → *IP Addresses* tab → scroll to **IPAll**:
   - TCP Dynamic Ports: *(empty)*
   - TCP Port: **1433**
4. *SQL Server Services* → right-click **SQL Server (MSSQLSERVER)** → **Restart**.

> **Already installed SQL Server Express?** It works too, but:
> - the server name is `localhost\SQLEXPRESS` in the sqlcmd commands below;
> - do step 3 above under *Protocols for SQLEXPRESS* (Express uses a random port otherwise);
> - make sure Mixed Mode is on (see Troubleshooting → "Login failed for user 'medisys_app'").

### A4. sqlcmd
Open **PowerShell** and check:

```powershell
sqlcmd -?
```

If it says "not recognized", install it:

```powershell
winget install sqlcmd
```

Close and reopen PowerShell afterwards.

### A5. Apache Tomcat 11
1. Go to <https://tomcat.apache.org/download-11.cgi>.
2. Under *Binary Distributions → Core*, download the **64-bit Windows zip**.
3. Extract it to a folder **without spaces**, e.g. `C:\tomcat11`
   (so that `C:\tomcat11\bin\catalina.bat` exists).

Nothing else to do - IntelliJ starts Tomcat for you.

---

## Part B - Get the project and the database (once)

### B1. Clone the repository
1. GitHub Desktop → *File → Clone repository* → pick `drug-ordering-system`.
   Default folder: `Documents\GitHub\drug-ordering-system`.
2. Top bar → *Current branch* → choose **`Dev`**.

> GitHub Desktop tip: when you switch branches with unsaved changes it offers to
> **stash** them. If files seem to disappear, look in *Branch → Stashed changes*.

### B2. Create the database
Open **PowerShell** in the project folder (in GitHub Desktop: *Repository → Open in Command Prompt*,
or `cd` to the folder) and run the three scripts **in this order**:

```powershell
cd $HOME\Documents\GitHub\drug-ordering-system

sqlcmd -S localhost -E -C -i database\create-database.sql
sqlcmd -S localhost -E -C -d MediSysDB -i database\schema.sql
sqlcmd -S localhost -E -C -d MediSysDB -i database\sample-data.sql
```

| Script | What it does | When to run it |
|--------|--------------|----------------|
| `create-database.sql` | creates the `MediSysDB` database and the app login `medisys_app` / `MediSys@2026` | once |
| `schema.sql` | **deletes and recreates** all tables | first time, and whenever someone changes the tables |
| `sample-data.sql` | demo users, medicines, orders, prescriptions, deliveries | always right after `schema.sql` |

`-E` = log in with your Windows account, `-C` = trust the local certificate.
Each command should finish without red error lines. (Messages like "(5 rows affected)" are fine.)

> Use **PowerShell** or Command Prompt, not Git Bash - sqlcmd fails in Git Bash.
> No sqlcmd? Open each script in SSMS (or IntelliJ's *Database* tool window) and run it there,
> in the same order, with `MediSysDB` selected for the last two.

### B3. Create the two local settings files
In `src\main\resources`:

1. Copy **`db.properties.example`** → rename the copy to **`db.properties`**.
   Leave its contents as they are (it already has the `medisys_app` login).
2. *(Optional)* copy `app.properties.example` → `app.properties` only if you want uploaded files
   somewhere else than `C:\Users\<you>\medisys-uploads`.

Both files are in `.gitignore`, so they are never pushed.

---

## Part C - Open and run in IntelliJ (recommended)

### C1. Open the project
1. IntelliJ → *File → Open* → select the **`drug-ordering-system`** folder → *Trust Project*.
2. If a popup says "Maven build scripts found", click **Load Maven Project**.
   Wait until the progress bar at the bottom finishes (it downloads the libraries the first time).
3. *File → Project Structure → Project* → **SDK**: pick any JDK **17 or newer**.
   None listed? Choose *Add SDK → Download JDK…* → Version 21, vendor *Eclipse Temurin* → Download.

### C2. Create the Tomcat run configuration
1. Top right → *Current File* dropdown → **Edit Configurations…**
2. Click **+** → **Tomcat Server → Local**. (Not there? *Settings → Plugins* → enable "Tomcat and TomEE".)
3. **Server** tab:
   - Application server → *Configure…* → **+** → Tomcat Home: `C:\tomcat11` → OK.
   - Name: `MediSys`.
   - HTTP port: `8080` (change it if 8080 is used by something else).
4. **Deployment** tab:
   - **+** → *Artifact…* → **`medisys:war exploded`**.
   - Application context: **`/medisys`**
5. Back on the **Server** tab: *On 'Update' action* → **Update classes and resources**,
   *On frame deactivation* → **Update classes and resources** (lets you see JSP/CSS changes without a restart).
6. **OK**.

### C3. Run
1. Press the green **Run ▶** (or **Debug 🐞**) next to `MediSys`.
2. The *Services/Run* window shows Tomcat starting. It is ready when you see
   `Artifact medisys:war exploded: Artifact is deployed successfully`, plus
   `[MediSys] Database connected: ...` and `[MediSys] File storage: ...`.
   If you see **`[MediSys] DATABASE NOT CONNECTED`**, read the message after it and check Troubleshooting.
3. The browser opens by itself; otherwise go to **<http://localhost:8080/medisys/>**.

To stop: the red **Stop ■** button. After changing Java code press **Update** (Ctrl+F10) or restart.

---

## Part D - Run from the command line (no IntelliJ Ultimate)

### D0. Tools
1. **JDK 17 or newer:** install *Eclipse Temurin 21* from <https://adoptium.net>
   (in the installer, set **"Set JAVA_HOME variable"** to *Will be installed*).
2. **Maven:** download the *Binary zip archive* from <https://maven.apache.org/download.cgi>,
   extract it to `C:\maven`, then add `C:\maven\bin` to your PATH
   (Start → "Edit the system environment variables" → *Environment Variables* → *Path* → *New*).

Close and reopen PowerShell, then check `java -version` and `mvn -version` (both must print a version).

> Have IntelliJ (any edition)? You can skip the Maven install and use its bundled one:
> `& "C:\Program Files\JetBrains\<IntelliJ folder>\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" package`

### D1. Build and start

```powershell
cd $HOME\Documents\GitHub\drug-ordering-system
echo $env:JAVA_HOME                                   # must show the JDK folder

mvn package                                           # creates target\medisys.war
Copy-Item target\medisys.war C:\tomcat11\webapps\ -Force
C:\tomcat11\bin\catalina.bat run                      # keep this window open
```

Open **<http://localhost:8080/medisys/>**. Stop with **Ctrl+C**.
After code changes: stop, run `mvn package` and copy the war again, then start.

---

## Part E - Try it

### Demo accounts

| Role | Email | Password | Starts at |
|------|-------|----------|-----------|
| Customer | nimal@example.com | Customer@123 | Medicines |
| Customer | kasuni@example.com | Customer@123 | Medicines |
| Customer (red-flagged) | tharindu@example.com | Customer@123 | Medicines |
| Customers with order history (for the reports) | amaya@ / dilan@ / ishara@example.com | Customer@123 | Medicines |
| Admin | admin@medisys.lk | Admin@123 | Orders |
| Senior Pharmacist | pharmacist@medisys.lk | Pharma@123 | Verification Dashboard |
| Delivery rider | delivery@medisys.lk | Delivery@123 | My Deliveries |
| Delivery rider | rider2@medisys.lk | Delivery@123 | My Deliveries |

Test card for payments: **4242 4242 4242 4242**, any future expiry (MM/YY), any 3-digit CVV.

### A 10-minute tour of all six modules

| # | Module | Try this |
|---|--------|----------|
| 03 | Medicine Catalog & Inventory | Browse **Medicines** as a guest. Log in as **admin → Inventory**: add, edit, restock, discontinue a medicine; **Categories**. |
| 01 | Cart & Wishlist | As **nimal**: add medicines to the cart, change quantities, save one for later (heart). |
| 02 | Order Placement & Checkout | Nimal: **Cart → Proceed to checkout**, pay with the test card, then **Orders**. |
| 05 | Prescription Upload & Verification | Log out, click **Upload a prescription** → you must log in or **register**. Upload a JPG/PNG/PDF. As **pharmacist**: open it, list medicines, approve (or reject / ask for a correction). As the customer: **Prescriptions → Pay**. |
| 06 | Delivery Tracking & Notification | As **delivery@**: **My Deliveries → New deliveries → Got the package**, then **On the way → Delivered**. As the customer: **Orders → Track**, and the 🔔 bell. |
| 04 | Reports & Analytics | **Admin → Reports**: switch between 7 / 30 / 90 days, hover the bars, **Download CSV**, **Save this report**, then **Saved reports** (edit the notes, delete). |
| minor | Accounts, login, profile | **Register** a new customer, open the **profile** (photo, history). Pharmacist: **Flag** a customer on the review page. **Admin → Users**: staff accounts, flags. **Forgot password** on the login page. |

To get the demo data back at any time, run `schema.sql` and `sample-data.sql` again (Part B2).

---

## Part F - Working on the project as a team

1. Always start from the latest `Dev`: GitHub Desktop → branch `Dev` → **Fetch origin** → **Pull**.
2. Create your own branch from `Dev` (*Branch → New branch*, e.g. `feature/<your-module>-fix`).
3. Commit small steps with clear messages, **Push**, and open a Pull Request into `Dev`.
4. If you change a table, update `database/schema.sql` **and** `database/sample-data.sql`,
   and tell the team to re-run both scripts.
5. Shared files (`header.jspf`, `style.css`, `AuthFilter`, `TextUtil`, the SQL scripts …) - agree
   with the team before changing them.

Where your code lives: see *Files per module* in [README.md](README.md) and
[handover-docs/file-ownership.txt](handover-docs/file-ownership.txt).

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `sqlcmd: A network-related or instance-specific error` | SQL Server is not running (Services → *SQL Server (MSSQLSERVER)* → Start), or you use Express: add `\SQLEXPRESS` to `-S localhost`. |
| `sqlcmd: -E and -U/-P are mutually exclusive` | You are in Git Bash. Use PowerShell. |
| `JAVA_HOME environment variable is not defined` (catalina.bat / mvn) | Set it for this window: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21..."` (use the real folder name), or tick "Set JAVA_HOME" when installing the JDK. |
| Every page shows **"Something went wrong" / 500**, log says *The TCP/IP connection to the host localhost, port 1433 has failed* | TCP/IP is off or not on port 1433 - redo the TCP/IP steps in A3 and restart SQL Server. |
| Log says **Login failed for user 'medisys_app'** | Mixed Mode is off. SSMS → right-click the server → *Properties → Security* → **SQL Server and Windows Authentication mode** → OK → restart SQL Server. (Or run `create-database.sql` again if the login is missing.) |
| Log says **src/main/resources/db.properties is missing** | You skipped B3 - copy `db.properties.example` to `db.properties`. |
| Log says **Invalid object name 'users'** (or another table) | Run `schema.sql` and `sample-data.sql` (B2). |
| Page error about a **missing column** after pulling new code | The tables changed - run `schema.sql` and `sample-data.sql` again. |
| **Port 8080 is already in use** | Another Tomcat/program uses it. Stop it, or change the HTTP port in the run configuration (C2). |
| IntelliJ has no **Tomcat Server** in the + list | You are on Community edition (use Part D), or enable the "Tomcat and TomEE" plugin. |
| **`medisys:war exploded`** is missing in Deployment | The Maven project was not loaded: right-click `pom.xml` → *Add as Maven Project*, then reload. |
| Red code everywhere, `jakarta` cannot be resolved | Maven tool window (right side) → **Reload All Maven Projects**; check the Project SDK (C1 step 3). |
| 404 at `http://localhost:8080/` | Add the context: **`/medisys/`**. |
| Demo prescription images don't show | They are copied to `C:\Users\<you>\medisys-uploads\samples` when the app starts - restart Tomcat. |
| Login says the password is wrong for a demo account | Someone changed it - run `schema.sql` + `sample-data.sql` to reset all demo data. |
