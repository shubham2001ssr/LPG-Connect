import java.sql.*;

public class create_database {
    public static void main(String[] args) {
        String DB_URL = "jdbc:mysql://localhost:3306/";
        String DB_USER = "root";
        String DB_PASSWORD = "12345678";
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL Driver loaded successfully!");
            
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to MySQL server!");
            
            Statement stmt = conn.createStatement();
            
            // Create database
            stmt.execute("CREATE DATABASE IF NOT EXISTS lpg_system");
            System.out.println("Database 'lpg_system' created or already exists!");
            
            // Use the database
            stmt.execute("USE lpg_system");
            
            // Create users table
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(50) PRIMARY KEY,
                    password VARCHAR(100) NOT NULL,
                    role VARCHAR(20) NOT NULL
                )
            """;
            stmt.execute(createUsersTable);
            System.out.println("Users table created!");
            
            // Create applications table
            String createApplicationsTable = """
                CREATE TABLE IF NOT EXISTS applications (
                    app_id INT AUTO_INCREMENT PRIMARY KEY,
                    applicant_username VARCHAR(50) NOT NULL,
                    name VARCHAR(100) NOT NULL,
                    mobile_no VARCHAR(15) NOT NULL,
                    address TEXT NOT NULL,
                    num_connections INT NOT NULL,
                    status VARCHAR(20) DEFAULT 'PENDING',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (applicant_username) REFERENCES users(username)
                )
            """;
            stmt.execute(createApplicationsTable);
            System.out.println("Applications table created!");
            
            // Insert default users
            String insertDefaultUsers = """
                INSERT IGNORE INTO users (username, password, role) VALUES 
                ('admin', 'admin123', 'ADMIN'),
                ('user1', 'user123', 'USER')
            """;
            stmt.execute(insertDefaultUsers);
            System.out.println("Default users inserted!");
            
            // Insert sample application
            String insertSampleApp = """
                INSERT IGNORE INTO applications (applicant_username, name, mobile_no, address, num_connections, status) 
                VALUES ('user1', 'Priya Sharma', '9876543210', '123, Main St.', 2, 'PENDING')
            """;
            stmt.execute(insertSampleApp);
            System.out.println("Sample application inserted!");
            
            // Verify data
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next()) {
                System.out.println("Number of users: " + rs.getInt(1));
            }
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM applications");
            if (rs.next()) {
                System.out.println("Number of applications: " + rs.getInt(1));
            }
            
            conn.close();
            System.out.println("Database setup completed successfully!");
            System.out.println("You can now run the LPG application with persistent database storage!");
            
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database setup failed: " + e.getMessage());
        }
    }
}
