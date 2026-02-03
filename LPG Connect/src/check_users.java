import java.sql.*;

public class check_users {
    public static void main(String[] args) {
        String DB_URL = "jdbc:mysql://localhost:3306/lpg_system";
        String DB_USER = "root";
        String DB_PASSWORD = "12345678";
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            System.out.println("🔍 Checking Users Table");
            System.out.println("======================");
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT username, LENGTH(username) as len, ASCII(SUBSTRING(username,1,1)) as first_char FROM users");
            
            while (rs.next()) {
                String username = rs.getString("username");
                int len = rs.getInt("len");
                int firstChar = rs.getInt("first_char");
                System.out.println("Username: '" + username + "' | Length: " + len + " | First char ASCII: " + firstChar);
            }
            
            conn.close();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
