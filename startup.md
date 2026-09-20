How to run MediSys, step by step
All commands are for PowerShell (not Git Bash), run from the project folder.

1. Go to the project folder

cd C:\Users\navee\Documents\GitHub\drug-ordering-system
2. Create the database (first time only)
You only need this on a new machine. I skipped it on yours because the database already has data, and schema.sql deletes every table.


sqlcmd -S localhost -E -C -i database\create-database.sql
sqlcmd -S localhost -E -C -d MediSysDB -i database\schema.sql
sqlcmd -S localhost -E -C -d MediSysDB -i database\sample-data.sql
To reset the demo data later, run just the last two commands again.

3. Create the settings file (first time only)
You already have this file.


Copy-Item src\main\resources\db.properties.example src\main\resources\db.properties
4. Build the app
Maven isn't on your PATH, so this uses the copy that comes with IntelliJ:


$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr"
& "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" package -DskipTests
This creates target\medisys.war.

5. Put the new build into Tomcat

Remove-Item C:\Users\navee\tomcat11\webapps\medisys -Recurse -Force
Copy-Item target\medisys.war C:\Users\navee\tomcat11\webapps\ -Force
6. Start Tomcat

C:\Users\navee\tomcat11\bin\catalina.bat run
Keep this window open. The app is ready when you see [MediSys] Database connected and Server startup in ... milliseconds. Press Ctrl+C to stop it.

7. Open the app
Go to http://localhost:8080/medisys/ (the /medisys/ part is needed).

Role	Email	Password
Admin	admin@medisys.lk	Admin@123
Customer	nimal@example.com	Customer@123
Pharmacist	pharmacist@medisys.lk	Pharma@123
Rider	delivery@medisys.lk	Delivery@123
To pay, use the test card 4242 4242 4242 4242 with any future MM/YY and any 3-digit CVV.

After changing code: stop Tomcat with Ctrl+C, then repeat steps 4, 5 and 6.

Tomcat is running in the background from this session right now. If you want to start it yourself in step 6, tell me and I'll stop mine first, because both use port 8080.