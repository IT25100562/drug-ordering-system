import java.sql.Connection;
import java.sql.DriverManager;

public class TestDB {
    public static void main(String[] args) {
        String url = "jdbc:sqlserver://localhost;instanceName=mssqllocaldb;databaseName=MediSysDB;encrypt=false;trustServerCertificate=true";
        String user = "medisys_app";
        String pass = "MediSys@2026";
        try {
            Connection conn = DriverManager.getConnection(url, user, pass);
            System.out.println("SUCCESS!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
