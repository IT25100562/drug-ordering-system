# Run MediSys locally on Ubuntu

This guide runs the website with Apache Tomcat on your Ubuntu computer and uses
the SQL Server container you start yourself. It does **not** start or stop Docker,
create or reset the database, or delete your database data.

## One-time setup

- Docker installed and permission to run `docker`.
- The SQL Server container is already created and initialized with the MediSys
  database scripts. Start it manually before running the website.
- SQL Server publishes its container port to host port `1433` (for example,
  `127.0.0.1:1433->1433/tcp`).
- Java 17 or newer, Maven, and `curl`. Install system-wide if needed:

  ```bash
  sudo apt update
  sudo apt install -y openjdk-17-jdk maven curl
  java -version
  mvn -version
  ```

- Apache Tomcat 11. Download the Linux `.tar.gz` from
  <https://tomcat.apache.org/download-11.cgi> and extract it, for example, to
  `$HOME/apache-tomcat-11`. Tomcat must run with the same Java installation.
- `src/main/resources/db.properties` exists and has the SQL Server host, port,
  database name, and `medisys_app` credentials. This file is local and ignored
  by Git. The default example works with the login created by
  `database/create-database.sql`; change its values if your local SQL login differs.

### Tools installed for this computer

For this computer, Java 17, Maven, and Tomcat 11 were downloaded under
`$HOME/.local/opt/medisys-tools` without installing system packages. In a terminal
opened at the project directory, set up their paths once per terminal:

```bash
export TOOL_HOME="$HOME/.local/opt/medisys-tools"
export JAVA_HOME="$TOOL_HOME/openjdk/usr/lib/jvm/java-17-openjdk-amd64"
export PATH="$JAVA_HOME/bin:$TOOL_HOME/apache-maven-3.9.16/bin:$PATH"
export MAVEN_OPTS="-Djavax.net.ssl.trustStore=$JAVA_HOME/lib/security/cacerts -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=PKCS12"
export TOMCAT_HOME="$TOOL_HOME/apache-tomcat-11.0.26"
```

These environment settings are needed in any new terminal before starting the
site. If you install Java/Maven/Tomcat elsewhere, use those executable paths
instead. `MAVEN_OPTS` points Java at the certificate store needed to download
Maven dependencies.

The database is initialized separately from starting the website. If it has not
yet been initialized, follow the one-time SQL setup in [SETUP.md](SETUP.md#ubuntu-docker-setup-sql-server).
Do not rerun `database/schema.sql` just to start the website: it drops existing
tables and deletes their data.

## Start the website

Start the SQL Server Docker container manually, then open a terminal in the
project directory. For the tools installed under
`$HOME/.local/opt/medisys-tools`, copy and run this block in that terminal:

```bash
export TOOL_HOME="$HOME/.local/opt/medisys-tools"
export JAVA_HOME="$TOOL_HOME/openjdk/usr/lib/jvm/java-17-openjdk-amd64"
export PATH="$JAVA_HOME/bin:$TOOL_HOME/apache-maven-3.9.16/bin:$PATH"
export MAVEN_OPTS="-Djavax.net.ssl.trustStore=$JAVA_HOME/lib/security/cacerts -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=PKCS12"
export TOMCAT_HOME="$TOOL_HOME/apache-tomcat-11.0.26"
./scripts/start-local.sh
```

The start script checks Java, Maven, Tomcat, the local database configuration,
and SQL Server's host port; builds the WAR; copies it into Tomcat; and starts
Tomcat in the background. It waits for the web app to respond and prints the URL:

<http://localhost:8080/medisys/>

After startup, check the Tomcat log if the app reports a database connection
problem:

```bash
tail -f "$TOMCAT_HOME/logs/catalina.out"
```

Look for `[MediSys] Database connected`. A missing or incorrect `db.properties`,
an uninitialized database, a stopped SQL Server container, or an unpublished
host port can prevent database access.

## Stop the website

In the same terminal where you started the site, run:

```bash
./scripts/stop-local.sh
```

If you opened a new terminal, set the Tomcat path again before stopping:

```bash
export TOMCAT_HOME="$HOME/.local/opt/medisys-tools/apache-tomcat-11.0.26"
./scripts/stop-local.sh
```

This stops the Tomcat instance started for this project. It deliberately leaves
the SQL Server Docker container running; stop that container yourself when you
want to.

The scripts accept `TOMCAT_HOME` (or `CATALINA_HOME`) if Tomcat is installed in
a different folder. The scripts do not manage IntelliJ's own Tomcat run
configuration; use IntelliJ's Stop button for a server started from the IDE.
These `.sh` scripts are intended to be run from a terminal after setting the
environment above; double-clicking them in the file manager is not a supported
start/stop method.

## Browse the database in IntelliJ and DBeaver

The database connection uses SQL Server on `localhost:1433`, database
`MediSysDB`, and the `medisys_app` SQL login from your ignored
`src/main/resources/db.properties`. Keep the Docker SQL Server container running.
Enter the password from that local properties file into each database client
when prompted; don't add it to tracked project files.

### IntelliJ IDEA

The project includes a local, password-free `MediSysDB` datasource definition
under `.idea`. In IntelliJ, open **View → Tool Windows → Database** (or the
**Database** tool window), then select the `MediSysDB` datasource and click
**Connect**. On first connect, enter the `medisys_app` password from
`src/main/resources/db.properties` and choose to save it in the IDE's local
password storage if desired. If the SQL Server driver download prompt appears,
click **Download**. Use **Refresh** on the datasource to update its table list.

If the datasource does not appear after opening the project, use **File → Sync
Project with Filesystem**, or add it via **Database → + → Data Source → Microsoft
SQL Server** with the settings below.

### DBeaver

1. Select **Database → New Database Connection** and choose **SQL Server**.
2. Enter **Host** `localhost`, **Port** `1433`, **Database** `MediSysDB`,
   **Authentication** SQL Server, and **Username** `medisys_app`.
3. Enter the password from `src/main/resources/db.properties`. Under SSL, disable
   encryption for this local development connection (or enable trust-server-certificate
   if your DBeaver driver requires it).
4. Click **Test Connection**, download the Microsoft SQL Server driver if prompted,
   then **Finish**. Expand `MediSysDB` → `Tables` to inspect the project data.

Both clients connect to the same running database; changes made by one are visible
to the other after refreshing. A connection refused error usually means the Docker
container is stopped or has not published host port 1433.

## Run from IntelliJ instead

Open the project as a Maven project, configure a JDK 17 or newer, and use a local
Tomcat 11 run configuration with the `medisys:war exploded` artifact and context
path `/medisys`. Keep the SQL Server container running and the local
`db.properties` configured. Start and stop the app using IntelliJ's Run and Stop
buttons; do not run the scripts and IntelliJ's Tomcat configuration at the same
time because both need port 8080.
