/*
    MediSys - step 1 of 3: create the database and the app's login.
    Shared file - agree with the team before changing it.

    Run this ONCE on your machine, as a Windows / sa login, e.g.:
        sqlcmd -S localhost -E -C -i database\create-database.sql

    Then run schema.sql and sample-data.sql (see README.md).

    The app connects with the SQL login below. The same login and password are
    in src/main/resources/db.properties.example, so every team member can use it
    as is. It is only for local development - never use it on a real server.
*/

IF DB_ID('MediSysDB') IS NULL
    CREATE DATABASE MediSysDB;
GO

IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'medisys_app')
    CREATE LOGIN medisys_app WITH PASSWORD = 'MediSys@2026', CHECK_POLICY = OFF;
GO

USE MediSysDB;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'medisys_app')
BEGIN
    CREATE USER medisys_app FOR LOGIN medisys_app;
    ALTER ROLE db_owner ADD MEMBER medisys_app;
END
GO
