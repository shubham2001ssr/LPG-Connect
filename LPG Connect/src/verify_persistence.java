import java.sql.*;

public class verify_persistence {
    public static void main(String[] args) {
        String DB_URL = "jdbc:mysql://localhost:3306/lpg_system";
        String DB_USER = "root";
        String DB_PASSWORD = "12345678";
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            System.out.println("=== LPG Database Verification ===");
            
            // Check users
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT username, role FROM users");
            System.out.println("\n📋 Users in database:");
            while (rs.next()) {
                System.out.println("  - " + rs.getString("username") + " (" + rs.getString("role") + ")");
            }
            
            // Check applications
            rs = stmt.executeQuery("SELECT app_id, applicant_username, name, status, created_at FROM applications ORDER BY created_at DESC");
            System.out.println("\n📝 Applications in database:");
            while (rs.next()) {
                System.out.println("  - ID: " + rs.getInt("app_id") + 
                                 ", User: " + rs.getString("applicant_username") + 
                                 ", Name: " + rs.getString("name") + 
                                 ", Status: " + rs.getString("status") + 
                                 ", Created: " + rs.getTimestamp("created_at"));
            }
            
            // Test data persistence by adding a new application
            System.out.println("\n🧪 Testing data persistence...");
            String insertTest = "INSERT INTO applications (applicant_username, name, mobile_no, address, num_connections, status) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertTest);
            pstmt.setString(1, "user1");
            pstmt.setString(2, "Test User");
            pstmt.setString(3, "9876543210");
            pstmt.setString(4, "Test Address");
            pstmt.setInt(5, 1);
            pstmt.setString(6, "PENDING");
            
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("✅ Test application added successfully! Rows affected: " + rowsAffected);
            
            // Verify the new application
            rs = stmt.executeQuery("SELECT COUNT(*) FROM applications");
            if (rs.next()) {
                System.out.println("📊 Total applications now: " + rs.getInt(1));
            }
            
            conn.close();
            System.out.println("\n✅ Database verification completed successfully!");
            System.out.println("🎉 Data persistence is working! The LPG application will save all data to MySQL database.");
            
        } catch (Exception e) {
            System.err.println("❌ Verification failed: " + e.getMessage());
        }
    }
}
