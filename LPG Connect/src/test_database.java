import java.sql.*;

public class test_database {
    public static void main(String[] args) {
        String DB_URL = "jdbc:mysql://localhost:3306/lpg_system";
        String DB_USER = "root";
        String DB_PASSWORD = "12345678";
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL Driver loaded successfully!");
            
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Database connection successful!");
            
            // Test query
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next()) {
                System.out.println("Number of users in database: " + rs.getInt(1));
            }
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM applications");
            if (rs.next()) {
                System.out.println("Number of applications in database: " + rs.getInt(1));
            }
            
            conn.close();
            System.out.println("Database test completed successfully!");
            
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            System.err.println("Please make sure:");
            System.err.println("1. MySQL server is running");
            System.err.println("2. Database 'lpg_system' exists");
            System.err.println("3. Username and password are correct");
        }
    }
}
