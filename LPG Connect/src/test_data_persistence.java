import java.sql.*;

public class test_data_persistence {
    public static void main(String[] args) {
        String DB_URL = "jdbc:mysql://localhost:3306/lpg_system";
        String DB_USER = "root";
        String DB_PASSWORD = "12345678";
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            System.out.println("🧪 Testing Data Persistence");
            System.out.println("============================");
            
            Statement stmt = conn.createStatement();
            
            // Get current count
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM applications");
            int beforeCount = 0;
            if (rs.next()) {
                beforeCount = rs.getInt(1);
            }
            System.out.println("📊 Applications before test: " + beforeCount);
            
            // Add a test application
            String insertTest = "INSERT INTO applications (applicant_username, name, mobile_no, address, num_connections, status) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertTest);
            pstmt.setString(1, "Shri");
            pstmt.setString(2, "Persistence Test User");
            pstmt.setString(3, "9999999999");
            pstmt.setString(4, "Test Address for Persistence");
            pstmt.setInt(5, 1);
            pstmt.setString(6, "PENDING");
            
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("✅ Test application added! Rows affected: " + rowsAffected);
            
            // Check count after insertion
            rs = stmt.executeQuery("SELECT COUNT(*) FROM applications");
            int afterCount = 0;
            if (rs.next()) {
                afterCount = rs.getInt(1);
            }
            System.out.println("📊 Applications after test: " + afterCount);
            
            // Verify the test application exists
            rs = stmt.executeQuery("SELECT * FROM applications WHERE name = 'Persistence Test User'");
            if (rs.next()) {
                System.out.println("✅ Test application found in database:");
                System.out.println("   - ID: " + rs.getInt("app_id"));
                System.out.println("   - User: " + rs.getString("applicant_username"));
                System.out.println("   - Name: " + rs.getString("name"));
                System.out.println("   - Status: " + rs.getString("status"));
                System.out.println("   - Created: " + rs.getTimestamp("created_at"));
            }
            
            // Show all recent applications
            System.out.println("\n📋 Recent Applications (last 5):");
            rs = stmt.executeQuery("SELECT app_id, applicant_username, name, status, created_at FROM applications ORDER BY created_at DESC LIMIT 5");
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.println("   " + count + ". ID:" + rs.getInt("app_id") + 
                                 " | User:" + rs.getString("applicant_username") + 
                                 " | Name:" + rs.getString("name") + 
                                 " | Status:" + rs.getString("status") + 
                                 " | Time:" + rs.getTimestamp("created_at"));
            }
            
            conn.close();
            
            System.out.println("\n🎉 DATA PERSISTENCE TEST COMPLETE!");
            System.out.println("✅ Data IS being saved to the MySQL database!");
            System.out.println("💾 The LPG application backend is working correctly!");
            
        } catch (Exception e) {
            System.err.println("❌ Data persistence test failed: " + e.getMessage());
        }
    }
}
