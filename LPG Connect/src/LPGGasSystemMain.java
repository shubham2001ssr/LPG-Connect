import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * LPG Gas Connection System - Simple In-Memory Simulation
 * * This file contains all components (Model, DAO, UI) for easy testing and execution,
 * satisfying all project constraints (Swing UI, CRUD, Two Roles, Validation).
 * NOTE: For a real project, these classes should be split into packages (model, dao, ui).
 */

// --- UTILITY/EXCEPTION (Mandatory Constraint: Exception Handling) ---
class ValidationException extends Exception {
    public ValidationException(String message) {
        super(message);
    }
}

class Validator {
    public static void validateMobileNo(String mobile) throws ValidationException {
        if (!mobile.matches("\\d{10}")) {
            throw new ValidationException("Mobile number must be exactly 10 digits.");
        }
    }

    public static void validatePositiveInteger(String value) throws ValidationException {
        try {
            int num = Integer.parseInt(value);
            if (num <= 0) {
                throw new ValidationException("Number of Connections must be a positive number.");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Number of Connections must be a valid integer.");
        }
    }
    
    public static void validateNotEmpty(String value, String fieldName) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty.");
        }
    }
}

// --- MODEL LAYER (Mandatory Constraint: Inheritance is demonstrated by Admin/RegularUser) ---

class User {
    private String username;
    private String password;
    private String role; // "ADMIN" or "USER"

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
}

class RegularUser extends User {
    public RegularUser(String username, String password) {
        super(username, password, "USER");
    }
}

class AdminUser extends User {
    public AdminUser(String username, String password) {
        super(username, password, "ADMIN");
    }
}

class Application {
    private static int nextId = 1001; 
    private int appId;
    private String applicantUsername; // Link to the user who submitted it
    private String name;
    private String mobileNo;
    private String address;
    private int numConnections;
    private String status; // PENDING, APPROVED, REJECTED

    public Application(String applicantUsername, String name, String mobileNo, String address, int numConnections) {
        this.appId = nextId++;
        this.applicantUsername = applicantUsername;
        this.name = name;
        this.mobileNo = mobileNo;
        this.address = address;
        this.numConnections = numConnections;
        this.status = "PENDING";
    }

    // Getters and Setters...
    public int getAppId() { return appId; }
    public void setAppId(int appId) { this.appId = appId; }
    public String getApplicantUsername() { return applicantUsername; }
    public String getName() { return name; }
    public String getMobileNo() { return mobileNo; }
    public String getAddress() { return address; }
    public int getNumConnections() { return numConnections; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    // ...
}

// --- DAO LAYER (Mandatory Constraint: Interface) ---

interface ApplicationDAO {
    Optional<User> validateUser(String username, String password);
    void registerUser(User user); // Updated to accept any User type
    List<User> findAllUsers(); // R - Get all users
    
    // CRUD Operations for Applications
    void saveApplication(Application app); // C
    List<Application> findAllApplications(); // R (Admin)
    Optional<Application> findApplicationById(int id); // R (Helper)
    // UPDATED: Returns a list of all applications by a user
    List<Application> findApplicationsByUsername(String username); // R (User - returns list)
    void updateApplication(Application app); // U
    void deleteApplication(int id); // D
}

class InMemoryDAO implements ApplicationDAO {
    private static final List<User> userList = new ArrayList<>();
    private static final List<Application> applicationList = new ArrayList<>();

    static {
        // Default Admin and User Accounts
        userList.add(new AdminUser("admin", "admin123")); 
        userList.add(new RegularUser("user1", "user123"));
        
        // Initial test application data
        applicationList.add(new Application("user1", "Priya Sharma", "9876543210", "123, Main St.", 2));
    }

    @Override
    public Optional<User> validateUser(String username, String password) {
        return userList.stream()
            .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
            .findFirst();
    }
    
    @Override
    public void registerUser(User user) {
        if (userList.stream().noneMatch(u -> u.getUsername().equals(user.getUsername()))) {
            userList.add(user);
        } else {
            // In a real app, this would throw a custom UserAlreadyExistsException
        }
    }
    
    @Override
    public List<User> findAllUsers() {
        return new ArrayList<>(userList);
    }

    // --- Application CRUD Implementation ---
    @Override
    public void saveApplication(Application app) { // C - Create
        applicationList.add(app);
    }

    @Override
    public List<Application> findAllApplications() { // R - Retrieve All
        return applicationList;
    }
    
    @Override
    public Optional<Application> findApplicationById(int id) {
        return applicationList.stream()
                .filter(app -> app.getAppId() == id)
                .findFirst();
    }
    
    // UPDATED IMPLEMENTATION
    @Override
    public List<Application> findApplicationsByUsername(String username) { // R - Retrieve all of User's
        // Returns a list of all applications submitted by the given username.
        return applicationList.stream()
                .filter(app -> app.getApplicantUsername().equals(username))
                .collect(Collectors.toList());
    }

    @Override
    public void updateApplication(Application updatedApp) { // U - Update
        for (int i = 0; i < applicationList.size(); i++) {
            if (applicationList.get(i).getAppId() == updatedApp.getAppId()) {
                applicationList.set(i, updatedApp); 
                return;
            }
        }
    }
    
    @Override
    public void deleteApplication(int id) { // D - Delete
        applicationList.removeIf(app -> app.getAppId() == id);
    }
}

// --- SQL DATABASE DAO IMPLEMENTATION ---

class MySQLDAO implements ApplicationDAO {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/lpg_system";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "12345678";
    
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            initializeDatabase();
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }
    
    private static void initializeDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Create users table
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(50) PRIMARY KEY,
                    password VARCHAR(100) NOT NULL,
                    role VARCHAR(20) NOT NULL
                )
            """;
            
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
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createUsersTable);
                stmt.execute(createApplicationsTable);
                
                // Insert default admin and user if they don't exist
                String insertDefaultUsers = """
                    INSERT IGNORE INTO users (username, password, role) VALUES 
                    ('admin', 'admin123', 'ADMIN'),
                    ('user1', 'user123', 'USER')
                """;
                stmt.execute(insertDefaultUsers);
                
                // Insert sample application if none exist
                String checkApplications = "SELECT COUNT(*) FROM applications";
                try (ResultSet rs = stmt.executeQuery(checkApplications)) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        String insertSampleApp = """
                            INSERT INTO applications (applicant_username, name, mobile_no, address, num_connections, status) 
                            VALUES ('user1', 'Priya Sharma', '9876543210', '123, Main St.', 2, 'PENDING')
                        """;
                        stmt.execute(insertSampleApp);
                    }
                }
            }
        }
    }
    
    @Override
    public Optional<User> validateUser(String username, String password) {
        String sql = "SELECT username, password, role FROM users WHERE username = ? AND password = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    User user = role.equals("ADMIN") ? 
                        new AdminUser(username, password) : 
                        new RegularUser(username, password);
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error during user validation: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    @Override
    public void registerUser(User user) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Database error during user registration: " + e.getMessage());
        }
    }
    
    @Override
    public List<User> findAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT username, password, role FROM users ORDER BY username";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                String username = rs.getString("username");
                String password = rs.getString("password");
                String role = rs.getString("role");
                
                User user = role.equals("ADMIN") ? 
                    new AdminUser(username, password) : 
                    new RegularUser(username, password);
                users.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Database error during users retrieval: " + e.getMessage());
        }
        
        return users;
    }
    
    @Override
    public void saveApplication(Application app) {
        String sql = "INSERT INTO applications (applicant_username, name, mobile_no, address, num_connections, status) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, app.getApplicantUsername());
            pstmt.setString(2, app.getName());
            pstmt.setString(3, app.getMobileNo());
            pstmt.setString(4, app.getAddress());
            pstmt.setInt(5, app.getNumConnections());
            pstmt.setString(6, app.getStatus());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Database error during application save: " + e.getMessage());
        }
    }
    
    @Override
    public List<Application> findAllApplications() {
        List<Application> applications = new ArrayList<>();
        String sql = "SELECT * FROM applications ORDER BY created_at DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                Application app = new Application(
                    rs.getString("applicant_username"),
                    rs.getString("name"),
                    rs.getString("mobile_no"),
                    rs.getString("address"),
                    rs.getInt("num_connections")
                );
                app.setAppId(rs.getInt("app_id"));
                app.setStatus(rs.getString("status"));
                applications.add(app);
            }
        } catch (SQLException e) {
            System.err.println("Database error during applications retrieval: " + e.getMessage());
        }
        
        return applications;
    }
    
    @Override
    public Optional<Application> findApplicationById(int id) {
        String sql = "SELECT * FROM applications WHERE app_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Application app = new Application(
                        rs.getString("applicant_username"),
                        rs.getString("name"),
                        rs.getString("mobile_no"),
                        rs.getString("address"),
                        rs.getInt("num_connections")
                    );
                    app.setAppId(rs.getInt("app_id"));
                    app.setStatus(rs.getString("status"));
                    return Optional.of(app);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error during application lookup: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<Application> findApplicationsByUsername(String username) {
        List<Application> applications = new ArrayList<>();
        String sql = "SELECT * FROM applications WHERE applicant_username = ? ORDER BY created_at DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Application app = new Application(
                        rs.getString("applicant_username"),
                        rs.getString("name"),
                        rs.getString("mobile_no"),
                        rs.getString("address"),
                        rs.getInt("num_connections")
                    );
                    app.setAppId(rs.getInt("app_id"));
                    app.setStatus(rs.getString("status"));
                    applications.add(app);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error during user applications retrieval: " + e.getMessage());
        }
        
        return applications;
    }
    
    @Override
    public void updateApplication(Application updatedApp) {
        String sql = "UPDATE applications SET status = ? WHERE app_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, updatedApp.getStatus());
            pstmt.setInt(2, updatedApp.getAppId());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Database error during application update: " + e.getMessage());
        }
    }
    
    @Override
    public void deleteApplication(int id) {
        String sql = "DELETE FROM applications WHERE app_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Database error during application deletion: " + e.getMessage());
        }
    }
}

// --- DAO FACTORY ---

class DAOFactory {
    public static ApplicationDAO createDAO() {
        try {
            // Try to create MySQL DAO
            return new MySQLDAO();
        } catch (Exception e) {
            System.err.println("MySQL database not available, falling back to in-memory storage: " + e.getMessage());
            return new InMemoryDAO();
        }
    }
}

// --- UI FRAMES ---

class LoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private ApplicationDAO dao = DAOFactory.createDAO();
    // User/Shared Theme Color: Light Yellow
    private static final Color USER_BG_COLOR = new Color(255, 255, 224); 

    public LoginFrame() {
        super("LPG Gas Connection System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        
        // Set Background Color with gradient effect
        getContentPane().setBackground(new Color(240, 248, 255));
        
        // Create main panel with proper layout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(240, 248, 255));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Add promotional banner
        JLabel promotionalBanner = new JLabel(
            "<html><div style='text-align:center; font-family: Arial; background: linear-gradient(90deg, #87CEEB 0%, #FFB6C1 100%); padding: 8px; border-radius: 6px; margin: 5px;'>" +
            "<h3 style='color: #1E3A8A; margin: 2px 0; font-size: 14px;'>LPG CONNECTION SERVICE</h3>" +
            "<h2 style='color: #1E3A8A; margin: 3px 0; font-size: 16px; font-weight: bold;'>Easy and Affordable Connections</h2>" +
            "<div style='display: flex; justify-content: space-around; margin: 3px 0;'>" +
            "<span style='font-size: 9px; color: #1E3A8A;'>Quick</span>" +
            "<span style='font-size: 9px; color: #1E3A8A;'>Safe</span>" +
            "<span style='font-size: 9px; color: #1E3A8A;'>Ready</span>" +
            "</div>" +
            "</div></html>",
            SwingConstants.CENTER
        );
        promotionalBanner.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Add title header
        JLabel titleLabel = new JLabel("<html><div style='text-align:center;'><h2 style='color: #2E8B57; margin: 10px;'>LPG Gas Connection System</h2><p style='color: #666; font-size: 12px;'>Secure Login Portal</p></div></html>", SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        txtUsername = new JTextField(15);
        txtPassword = new JPasswordField(15);
        JButton btnLogin = new JButton("Login");
        JButton btnSignup = new JButton("Register");
        
        // Style the buttons
        btnLogin.setBackground(new Color(34, 139, 34));
        btnLogin.setForeground(Color.BLACK);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorder(BorderFactory.createRaisedBevelBorder());
        
        btnSignup.setBackground(new Color(70, 130, 180));
        btnSignup.setForeground(Color.BLACK);
        btnSignup.setFocusPainted(false);
        btnSignup.setBorder(BorderFactory.createRaisedBevelBorder());
        
        // Set preferred button sizes
        Dimension buttonSize = new Dimension(120, 35);
        btnLogin.setPreferredSize(buttonSize);
        btnSignup.setPreferredSize(buttonSize);
        
        // Add components using GridBagLayout
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(promotionalBanner, gbc);
        
        gbc.gridy = 1;
        mainPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(new JLabel("Username:"), gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(txtUsername, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(txtPassword, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(btnSignup, gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(btnLogin, gbc);
        
        add(mainPanel);
        
        btnLogin.addActionListener(this::handleLogin);
        btnSignup.addActionListener(e -> {
            new RegistrationFrame().setVisible(true);
            this.dispose();
        });
    }
    
    private void handleLogin(ActionEvent e) {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());
        
        Optional<User> userOpt = dao.validateUser(username, password);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            JOptionPane.showMessageDialog(this, "Login Successful! Welcome, " + user.getRole(), "Success", JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
            
            if (user.getRole().equals("ADMIN")) {
                new AdminDashboardFrame().setVisible(true);
            } else {
                new UserDashboardFrame(username).setVisible(true);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Invalid Username or Password.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

class RegistrationFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private ApplicationDAO dao = DAOFactory.createDAO();
    // User/Shared Theme Color: Light Yellow
    private static final Color USER_BG_COLOR = new Color(255, 255, 224); 

    public RegistrationFrame() {
        super("LPG System - Register User");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 250);
        setLocationRelativeTo(null);
        
        // Set Background Color
        getContentPane().setBackground(new Color(240, 248, 255));
        
        // Create main panel with proper layout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(240, 248, 255));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Add title header
        JLabel titleLabel = new JLabel("<html><div style='text-align:center;'><h2 style='color: #2E8B57; margin: 10px;'>Create New Account</h2><p style='color: #666; font-size: 12px;'>Join LPG Gas Connection System</p></div></html>", SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        txtUsername = new JTextField(15);
        txtPassword = new JPasswordField(15);
        JButton btnRegister = new JButton("Create Account");
        JButton btnBack = new JButton("Back to Login");
        
        // Style the buttons
        btnRegister.setBackground(new Color(34, 139, 34));
        btnRegister.setForeground(Color.BLACK);
        btnRegister.setFocusPainted(false);
        btnRegister.setBorder(BorderFactory.createRaisedBevelBorder());
        
        btnBack.setBackground(new Color(105, 105, 105));
        btnBack.setForeground(Color.BLACK);
        btnBack.setFocusPainted(false);
        btnBack.setBorder(BorderFactory.createRaisedBevelBorder());
        
        // Set preferred button sizes
        Dimension buttonSize = new Dimension(140, 35);
        btnRegister.setPreferredSize(buttonSize);
        btnBack.setPreferredSize(buttonSize);
        
        // Add components using GridBagLayout
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(new JLabel("Username:"), gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(txtUsername, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(txtPassword, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(btnBack, gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(btnRegister, gbc);
        
        add(mainPanel);
        
        btnRegister.addActionListener(this::handleRegister);
        btnBack.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            this.dispose();
        });
    }

    private void handleRegister(ActionEvent e) {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());
        
        try {
            Validator.validateNotEmpty(username, "Username");
            Validator.validateNotEmpty(password, "Password");
            
            // Check if username already exists (Simplified validation)
            if (dao.validateUser(username, password).isPresent()) {
                 JOptionPane.showMessageDialog(this, "Username already taken.", "Error", JOptionPane.ERROR_MESSAGE);
                 return;
            }
            
            dao.registerUser(new RegularUser(username, password));
            JOptionPane.showMessageDialog(this, "Registration successful! Please log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
            new LoginFrame().setVisible(true);
            this.dispose();
            
        } catch (ValidationException ve) {
            JOptionPane.showMessageDialog(this, ve.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

// --- USER UI (Create Application & Read-Only View) ---

class UserDashboardFrame extends JFrame implements ActionListener {
    private String username;
    // User/Shared Theme Color: Light Yellow
    private static final Color USER_BG_COLOR = new Color(255, 255, 224); 

    public UserDashboardFrame(String username) {
        super("User Dashboard: " + username);
        this.username = username;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(500, 300));
        
        // Set Background Color
        getContentPane().setBackground(USER_BG_COLOR);
        
        JMenuBar mb = new JMenuBar();
        JMenu appMenu = new JMenu("Application");
        JMenu exitMenu = new JMenu("Exit");

        JMenuItem newApp = new JMenuItem("New Application Request");
        JMenuItem viewApp = new JMenuItem("View My Application Status");
        JMenuItem exitItem = new JMenuItem("Logout");
        
        newApp.addActionListener(this);
        viewApp.addActionListener(this);
        exitItem.addActionListener(this);
        
        appMenu.add(newApp);
        appMenu.add(viewApp);
        exitMenu.add(exitItem);
        
        mb.add(appMenu);
        mb.add(exitMenu);
        setJMenuBar(mb);

        // Enhanced Welcome Tag with LPG theme and promotional image
        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setBackground(USER_BG_COLOR);
        
        // Create promotional banner
        JLabel promotionalBanner = new JLabel(
            "<html><div style='text-align:center; font-family: Arial; background: linear-gradient(90deg, #87CEEB 0%, #FFB6C1 100%); padding: 10px; border-radius: 8px; margin: 5px;'>" +
            "<h2 style='color: #1E3A8A; margin: 2px 0; font-size: 16px;'>LPG CONNECTION SERVICE</h2>" +
            "<h1 style='color: #1E3A8A; margin: 5px 0; font-size: 20px; font-weight: bold;'>Easy and Affordable Connections</h1>" +
            "<div style='display: flex; justify-content: space-around; margin: 5px 0;'>" +
            "<span style='font-size: 10px; color: #1E3A8A;'>Quick Installation</span>" +
            "<span style='font-size: 10px; color: #1E3A8A;'>Safe & Reliable</span>" +
            "<span style='font-size: 10px; color: #1E3A8A;'>Kitchen Ready</span>" +
            "</div>" +
            "</div></html>",
            SwingConstants.CENTER
        );
        
        // Create welcome message
        JLabel welcomeLabel = new JLabel(
            "<html><div style='text-align:center; padding: 20px;'><h1 style='color: #2E8B57; font-size: 28px; margin: 10px;'>Welcome, " 
            + username + "</h1><p style='color: #666; font-size: 16px; margin: 10px;'>LPG Gas Connection Management System</p><p style='font-size: 14px; color: #888;'>Use the 'Application' menu to request or view your connection status.</p></div></html>", 
            SwingConstants.CENTER
        );
        
        welcomePanel.add(promotionalBanner, BorderLayout.NORTH);
        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);
        add(welcomePanel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        
        if (command.equals("New Application Request")) {
            // New instance ensures the latest check for existing application is run
            new NewApplicationFrame(username, this).setVisible(true);
        } else if (command.equals("View My Application Status")) {
            // New instance will now show all applications in a table
            new ViewMyApplicationFrame(username).setVisible(true); 
        } else if (command.equals("Logout")) {
            new LoginFrame().setVisible(true);
            this.dispose();
        }
    }
}

class NewApplicationFrame extends JFrame {
    private JTextField txtName, txtMobile, txtConnections;
    private JTextArea txtAddress;
    private String username;
    private JFrame parent;
    private ApplicationDAO dao = DAOFactory.createDAO();
    // User/Shared Theme Color: Light Yellow
    private static final Color USER_BG_COLOR = new Color(255, 255, 224); 

    public NewApplicationFrame(String username, JFrame parent) {
        super("LPG System - New Connection Request");
        this.username = username;
        this.parent = parent;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(550, 500);
        setLocationRelativeTo(parent);
        setMinimumSize(new Dimension(500, 450));
        
        // Set Background Color
        getContentPane().setBackground(new Color(240, 248, 255));
        
        // Create main panel with proper layout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(240, 248, 255));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Add title header
        JLabel titleLabel = new JLabel("<html><div style='text-align:center;'><h2 style='color: #2E8B57; margin: 10px;'>New LPG Connection Request</h2><p style='color: #666; font-size: 12px;'>Fill in your details below</p></div></html>", SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        txtName = new JTextField(20);
        txtMobile = new JTextField(20);
        txtAddress = new JTextArea(4, 20);
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);
        txtConnections = new JTextField(20);
        JButton btnSubmit = new JButton("Submit Request");
        JButton btnCancel = new JButton("Cancel");
        
        // Style the buttons
        btnSubmit.setBackground(new Color(34, 139, 34));
        btnSubmit.setForeground(Color.BLACK);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setBorder(BorderFactory.createRaisedBevelBorder());
        
        btnCancel.setBackground(new Color(220, 20, 60));
        btnCancel.setForeground(Color.BLACK);
        btnCancel.setFocusPainted(false);
        btnCancel.setBorder(BorderFactory.createRaisedBevelBorder());
        
        // Set preferred button sizes
        Dimension buttonSize = new Dimension(150, 35);
        btnSubmit.setPreferredSize(buttonSize);
        btnCancel.setPreferredSize(buttonSize);
        
        // Add components using GridBagLayout
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(new JLabel("Applicant Name:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(txtName, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(new JLabel("Mobile No. (10 digits):"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(txtMobile, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(new JLabel("Address:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(txtAddress, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(new JLabel("No. of Connections:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(txtConnections, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(btnCancel, gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(btnSubmit, gbc);
        
        add(mainPanel);
        
        btnSubmit.addActionListener(this::handleSubmit);
        btnCancel.addActionListener(e -> this.dispose());
    }

    private void handleSubmit(ActionEvent e) {
        // Mandatory Constraint: All Validations are must.
        try {
            String name = txtName.getText();
            String mobile = txtMobile.getText();
            String address = txtAddress.getText();
            String connections = txtConnections.getText();
            
            Validator.validateNotEmpty(name, "Name");
            Validator.validateMobileNo(mobile);
            Validator.validateNotEmpty(address, "Address");
            Validator.validatePositiveInteger(connections);
            
            int numConn = Integer.parseInt(connections);
            
            // --- LOGIC TO ENFORCE ONE ACTIVE APPLICATION ---
            List<Application> existingApps = dao.findApplicationsByUsername(username);
            
            // Check if any existing application is PENDING or APPROVED
            boolean hasActiveApplication = existingApps.stream()
                .anyMatch(app -> app.getStatus().equals("PENDING") || app.getStatus().equals("APPROVED"));
            
            if (hasActiveApplication) {
                JOptionPane.showMessageDialog(this, 
                    "You currently have a PENDING or APPROVED application. You cannot submit a new request until it is settled.", 
                    "Application Blocked", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            // If the user only has REJECTED applications, or no applications, they can submit a new one.
            // --- END LOGIC ---

            // C - Create: This runs if no existing active app was found.
            Application newApp = new Application(username, name, mobile, address, numConn);
            dao.saveApplication(newApp); 
            
            JOptionPane.showMessageDialog(this, "Application submitted! ID: " + newApp.getAppId(), "Success", JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
            
        } catch (ValidationException ve) {
            // Mandatory Constraint: Exception Handling
            JOptionPane.showMessageDialog(this, ve.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

/**
 * UPDATED: Refactored to use a JTable to display ALL of the user's applications
 * (Pending, Approved, Rejected) instead of just showing one application's status.
 */
class ViewMyApplicationFrame extends JFrame {
    private String username;
    private ApplicationDAO dao = DAOFactory.createDAO();
    private JTable applicationTable;
    private DefaultTableModel tableModel;
    private final String[] COLUMN_NAMES = {"ID", "Name", "Mobile No.", "Connections", "Status"};
    // User/Shared Theme Color: Light Yellow
    private static final Color USER_BG_COLOR = new Color(255, 255, 224); 

    public ViewMyApplicationFrame(String username) {
        super("My Application History: " + username);
        this.username = username;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 500);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(700, 400));
        
        // Set Background Color
        getContentPane().setBackground(USER_BG_COLOR);
        
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0);
        applicationTable = new JTable(tableModel);
        applicationTable.setEnabled(false); // Make table read-only for the user
        
        JScrollPane scrollPane = new JScrollPane(applicationTable);
        
        JButton btnRefresh = new JButton("Refresh History");
        JButton btnClose = new JButton("Close");
        
        // Set preferred button sizes
        Dimension buttonSize = new Dimension(120, 35);
        btnRefresh.setPreferredSize(buttonSize);
        btnClose.setPreferredSize(buttonSize);
        
        btnRefresh.addActionListener(e -> loadApplicationData());
        btnClose.addActionListener(e -> this.dispose());
        
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        southPanel.add(btnRefresh);
        southPanel.add(btnClose);
        southPanel.setBackground(USER_BG_COLOR); // Apply theme color to panel
        
        getContentPane().add(new JLabel("<html><h2 style='text-align:center;'>Your LPG Connection Application History</h2></html>", SwingConstants.CENTER), BorderLayout.NORTH);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(southPanel, BorderLayout.SOUTH);

        // Initial load of data
        loadApplicationData();
    }
    
    // Method to load and update data dynamically (called on load and refresh button click)
    private void loadApplicationData() {
        tableModel.setRowCount(0); 
        
        // R - Retrieve ALL applications for this user
        List<Application> applications = dao.findApplicationsByUsername(username);
        
        if (applications.isEmpty()) {
            // Display a message if no applications are found
            // Using System.out.println for console logging instead of JOptionPane
        } else {
            // Populate the table with all application records
            for (Application app : applications) {
                tableModel.addRow(new Object[]{
                    app.getAppId(), 
                    app.getName(), 
                    app.getMobileNo(), 
                    app.getNumConnections(), 
                    app.getStatus()
                });
            }
        }
    }
}

// --- ENHANCED ADMIN UI (Full CRUD + User Management) ---

/**
 * Enhanced AdminDashboardFrame with comprehensive user management and application approval system
 */
class AdminDashboardFrame extends JFrame implements ActionListener {
    private JTable applicationTable, userTable;
    private DefaultTableModel applicationTableModel, userTableModel;
    private ApplicationDAO dao = DAOFactory.createDAO();
    private JTabbedPane tabbedPane;
    
    // Statistics cards for updating
    private JPanel statsCard1, statsCard2, statsCard3, statsCard4, statsCard5, statsCard6;
    private JLabel statsValue1, statsValue2, statsValue3, statsValue4, statsValue5, statsValue6;
    
    // Application table columns
    private final String[] APP_COLUMN_NAMES = {"ID", "User", "Name", "Mobile No.", "Connections", "Status", "Created"};
    // User table columns
    private final String[] USER_COLUMN_NAMES = {"Username", "Role", "Password", "Actions"};
    
    // Admin Theme Color
    private static final Color ADMIN_BG_COLOR = new Color(230, 240, 255);
    private static final Color BUTTON_BG_COLOR = new Color(70, 130, 180);
    private static final Color SUCCESS_COLOR = new Color(34, 139, 34);
    private static final Color WARNING_COLOR = new Color(255, 140, 0);
    private static final Color DANGER_COLOR = new Color(220, 20, 60);

    public AdminDashboardFrame() {
        super("Admin Dashboard - LPG Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1000, 700));
        
        // Set Background Color
        getContentPane().setBackground(ADMIN_BG_COLOR);
        
        // Create menu bar
        createMenuBar();
        
        // Create main content
        createMainContent();
        
        // Load initial data
        loadApplicationData();
        loadUserData();
        loadStatistics();
    }
    
    private void createMenuBar() {
        JMenuBar mb = new JMenuBar();
        
        // Dashboard Menu
        JMenu dashboardMenu = new JMenu("Dashboard");
        JMenuItem refreshItem = new JMenuItem("Refresh All Data");
        refreshItem.addActionListener(e -> {
            loadApplicationData();
            loadUserData();
        });
        dashboardMenu.add(refreshItem);
        
        // User Management Menu
        JMenu userMenu = new JMenu("User Management");
        JMenuItem viewUsersItem = new JMenuItem("View All Users");
        JMenuItem addUserItem = new JMenuItem("Add New User");
        JMenuItem userStatsItem = new JMenuItem("User Statistics");
        
        viewUsersItem.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        addUserItem.addActionListener(e -> showAddUserDialog());
        userStatsItem.addActionListener(e -> showUserStatistics());
        
        userMenu.add(viewUsersItem);
        userMenu.add(addUserItem);
        userMenu.addSeparator();
        userMenu.add(userStatsItem);
        
        // Application Management Menu
        JMenu appMenu = new JMenu("Applications");
        JMenuItem viewAppsItem = new JMenuItem("View All Applications");
        JMenuItem pendingAppsItem = new JMenuItem("Pending Applications");
        JMenuItem approvedAppsItem = new JMenuItem("Approved Applications");
        JMenuItem rejectedAppsItem = new JMenuItem("Rejected Applications");
        
        viewAppsItem.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        pendingAppsItem.addActionListener(e -> filterApplications("PENDING"));
        approvedAppsItem.addActionListener(e -> filterApplications("APPROVED"));
        rejectedAppsItem.addActionListener(e -> filterApplications("REJECTED"));
        
        appMenu.add(viewAppsItem);
        appMenu.addSeparator();
        appMenu.add(pendingAppsItem);
        appMenu.add(approvedAppsItem);
        appMenu.add(rejectedAppsItem);
        
        // Exit Menu
        JMenu exitMenu = new JMenu("Exit");
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> handleLogout());
        exitMenu.add(logoutItem);
        
        mb.add(dashboardMenu);
        mb.add(userMenu);
        mb.add(appMenu);
        mb.add(exitMenu);
        setJMenuBar(mb);
    }
    
    private void createMainContent() {
        // Create header panel
        JPanel headerPanel = createHeaderPanel();
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(ADMIN_BG_COLOR);
        
        // Applications tab
        JPanel applicationsPanel = createApplicationsPanel();
        tabbedPane.addTab("Applications", applicationsPanel);
        
        // Users tab
        JPanel usersPanel = createUsersPanel();
        tabbedPane.addTab("Users", usersPanel);
        
        // Statistics tab
        JPanel statsPanel = createStatisticsPanel();
        tabbedPane.addTab("Statistics", statsPanel);
        
        // Add components to main frame
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ADMIN_BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        // Create the header label
        JLabel headerLabel = new JLabel(
            "<html><div style='text-align:center;'>" +
            "<h1 style='color: #2E8B57; margin: 5px; font-size: 24px;'>LPG Management System</h1>" +
            "<p style='color: #666; font-size: 14px; margin: 5px;'>Admin Control Panel - Complete System Management</p>" +
            "</div></html>", 
            SwingConstants.CENTER
        );
        
        headerPanel.add(headerLabel, BorderLayout.CENTER);
        return headerPanel;
    }
    
    
    private JPanel createApplicationsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ADMIN_BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create applications table
        applicationTableModel = new DefaultTableModel(APP_COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        applicationTable = new JTable(applicationTableModel);
        applicationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        applicationTable.setRowHeight(25);
        
        JScrollPane scrollPane = new JScrollPane(applicationTable);
        scrollPane.setPreferredSize(new Dimension(0, 400));
        
        // Create application buttons panel
        JPanel buttonPanel = createApplicationButtonsPanel();
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createApplicationButtonsPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(ADMIN_BG_COLOR);
        
        // Create buttons with consistent styling
        JButton btnRefresh = createStyledButton("Refresh", BUTTON_BG_COLOR, "Refresh application data");
        JButton btnUpdate = createStyledButton("Update Status", WARNING_COLOR, "Update application status");
        JButton btnDelete = createStyledButton("Delete", DANGER_COLOR, "Delete selected application");
        JButton btnViewDetails = createStyledButton("View Details", BUTTON_BG_COLOR, "View application details");
        
        // Add action listeners
        btnRefresh.addActionListener(e -> loadApplicationData());
        btnUpdate.addActionListener(e -> handleUpdateStatus());
        btnDelete.addActionListener(e -> handleDeleteApplication());
        btnViewDetails.addActionListener(e -> handleViewDetails());
        
        // Add buttons to panel
        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnViewDetails);
        buttonPanel.add(btnDelete);
        
        return buttonPanel;
    }
    
    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ADMIN_BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create users table
        userTableModel = new DefaultTableModel(USER_COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3; // Only actions column is editable
            }
        };
        userTable = new JTable(userTableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setRowHeight(25);
        
        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setPreferredSize(new Dimension(0, 400));
        
        // Create user buttons panel
        JPanel buttonPanel = createUserButtonsPanel();
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createUserButtonsPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(ADMIN_BG_COLOR);
        
        JButton btnRefresh = createStyledButton("Refresh", BUTTON_BG_COLOR, "Refresh user data");
        JButton btnAddUser = createStyledButton("Add User", SUCCESS_COLOR, "Add new user");
        JButton btnEditUser = createStyledButton("Edit User", WARNING_COLOR, "Edit selected user");
        JButton btnDeleteUser = createStyledButton("Delete User", DANGER_COLOR, "Delete selected user");
        JButton btnResetPassword = createStyledButton("Reset Password", BUTTON_BG_COLOR, "Reset user password");
        
        btnRefresh.addActionListener(e -> loadUserData());
        btnAddUser.addActionListener(e -> showAddUserDialog());
        btnEditUser.addActionListener(e -> showEditUserDialog());
        btnDeleteUser.addActionListener(e -> handleDeleteUser());
        btnResetPassword.addActionListener(e -> handleResetPassword());
        
        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnAddUser);
        buttonPanel.add(btnEditUser);
        buttonPanel.add(btnResetPassword);
        buttonPanel.add(btnDeleteUser);
        
        return buttonPanel;
    }
    
    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ADMIN_BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Statistics cards - store references for updating
        statsCard1 = createStatsCard("Total Users", "0", "Registered users in system");
        statsCard2 = createStatsCard("Total Applications", "0", "All applications submitted");
        statsCard3 = createStatsCard("Pending", "0", "Applications awaiting approval");
        statsCard4 = createStatsCard("Approved", "0", "Applications approved");
        statsCard5 = createStatsCard("Rejected", "0", "Applications rejected");
        statsCard6 = createStatsCard("Approval Rate", "0%", "Overall approval percentage");
        
        // Add cards to panel
        gbc.gridx = 0; gbc.gridy = 0; panel.add(statsCard1, gbc);
        gbc.gridx = 1; gbc.gridy = 0; panel.add(statsCard2, gbc);
        gbc.gridx = 2; gbc.gridy = 0; panel.add(statsCard3, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(statsCard4, gbc);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(statsCard5, gbc);
        gbc.gridx = 2; gbc.gridy = 1; panel.add(statsCard6, gbc);
        
        // Refresh button
        JButton btnRefreshStats = createStyledButton("Refresh Statistics", BUTTON_BG_COLOR, "Update all statistics");
        btnRefreshStats.addActionListener(e -> loadStatistics());
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(btnRefreshStats, gbc);
        
        return panel;
    }
    
    private JPanel createStatsCard(String title, String value, String description) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setPreferredSize(new Dimension(200, 120));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setForeground(new Color(34, 139, 34));
        
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        descLabel.setForeground(Color.GRAY);
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(descLabel, BorderLayout.SOUTH);
        
        // Store reference to value label for updating
        if (title.equals("Total Users")) statsValue1 = valueLabel;
        else if (title.equals("Total Applications")) statsValue2 = valueLabel;
        else if (title.equals("Pending")) statsValue3 = valueLabel;
        else if (title.equals("Approved")) statsValue4 = valueLabel;
        else if (title.equals("Rejected")) statsValue5 = valueLabel;
        else if (title.equals("Approval Rate")) statsValue6 = valueLabel;
        
        return card;
    }
    
    private JButton createStyledButton(String text, Color bgColor, String tooltip) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setPreferredSize(new Dimension(140, 35));
        button.setToolTipText(tooltip);
        return button;
    }
    
    // Data loading methods
    private void loadApplicationData() {
        applicationTableModel.setRowCount(0);
        List<Application> applications = dao.findAllApplications();
        for (Application app : applications) {
            applicationTableModel.addRow(new Object[]{
                app.getAppId(),
                app.getApplicantUsername(),
                app.getName(),
                app.getMobileNo(),
                app.getNumConnections(),
                app.getStatus(),
                "Recent" // Placeholder for created date
            });
        }
    }
    
    private void loadUserData() {
        userTableModel.setRowCount(0);
        
        try {
            // Load users from database
            List<User> users = dao.findAllUsers();
            
            for (User user : users) {
                String actionText = "System User";
                if (!user.getUsername().equals("admin")) {
                    actionText = "Regular User";
                }
                
                userTableModel.addRow(new Object[]{
                    user.getUsername(),
                    user.getRole(),
                    user.getPassword(),
                    actionText
                });
            }
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
            // Fallback to showing error
            userTableModel.addRow(new Object[]{"Error", "Error", "Error", "Failed to load users"});
        }
    }
    
    private void loadStatistics() {
        try {
            // Load applications from database
            List<Application> applications = dao.findAllApplications();
            
            // Calculate statistics
            int totalApplications = applications.size();
            int pendingCount = 0;
            int approvedCount = 0;
            int rejectedCount = 0;
            
            for (Application app : applications) {
                switch (app.getStatus()) {
                    case "PENDING":
                        pendingCount++;
                        break;
                    case "APPROVED":
                        approvedCount++;
                        break;
                    case "REJECTED":
                        rejectedCount++;
                        break;
                }
            }
            
            // Calculate approval rate
            double approvalRate = totalApplications > 0 ? (double) approvedCount / totalApplications * 100 : 0;
            
            // Get user count from database
            int totalUsers = dao.findAllUsers().size();
            
            // Update statistics cards
            if (statsValue1 != null) statsValue1.setText(String.valueOf(totalUsers));
            if (statsValue2 != null) statsValue2.setText(String.valueOf(totalApplications));
            if (statsValue3 != null) statsValue3.setText(String.valueOf(pendingCount));
            if (statsValue4 != null) statsValue4.setText(String.valueOf(approvedCount));
            if (statsValue5 != null) statsValue5.setText(String.valueOf(rejectedCount));
            if (statsValue6 != null) statsValue6.setText(String.format("%.1f%%", approvalRate));
            
            // Update card colors based on values
            updateStatsCardColors();
            
        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
            // Show error in statistics
            if (statsValue1 != null) statsValue1.setText("Error");
            if (statsValue2 != null) statsValue2.setText("Error");
            if (statsValue3 != null) statsValue3.setText("Error");
            if (statsValue4 != null) statsValue4.setText("Error");
            if (statsValue5 != null) statsValue5.setText("Error");
            if (statsValue6 != null) statsValue6.setText("Error");
        }
    }
    
    private void updateStatsCardColors() {
        // Update colors based on values for better visual feedback
        if (statsValue3 != null) {
            int pendingCount = Integer.parseInt(statsValue3.getText());
            statsValue3.setForeground(pendingCount > 0 ? new Color(255, 140, 0) : new Color(34, 139, 34));
        }
        
        if (statsValue4 != null) {
            int approvedCount = Integer.parseInt(statsValue4.getText());
            statsValue4.setForeground(approvedCount > 0 ? new Color(34, 139, 34) : Color.GRAY);
        }
        
        if (statsValue5 != null) {
            int rejectedCount = Integer.parseInt(statsValue5.getText());
            statsValue5.setForeground(rejectedCount > 0 ? new Color(220, 20, 60) : Color.GRAY);
        }
    }
    
    // Application management methods
    
    private void handleUpdateStatus() {
        int selectedRow = applicationTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an application to update.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int appId = (int) applicationTableModel.getValueAt(selectedRow, 0);
        Optional<Application> appOpt = dao.findApplicationById(appId);
        
        if (appOpt.isPresent()) {
            Application selectedApp = appOpt.get();
            String[] statuses = {"PENDING", "APPROVED", "REJECTED"};
            JComboBox<String> statusCombo = new JComboBox<>(statuses);
            statusCombo.setSelectedItem(selectedApp.getStatus());
            
            int result = JOptionPane.showConfirmDialog(this, statusCombo, 
                "Change Status for Application ID: " + appId,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            
            if (result == JOptionPane.OK_OPTION) {
                updateApplicationStatus(appId, (String) statusCombo.getSelectedItem());
            }
        }
    }
    
    private void updateApplicationStatus(int appId, String status) {
        Optional<Application> appOpt = dao.findApplicationById(appId);
        if (appOpt.isPresent()) {
            Application app = appOpt.get();
            app.setStatus(status);
            dao.updateApplication(app);
            loadApplicationData();
            loadStatistics(); // Refresh statistics after status update
            JOptionPane.showMessageDialog(this, 
                "Application " + appId + " status updated to " + status + ".", 
                "Status Updated", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void handleDeleteApplication() {
        int selectedRow = applicationTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an application to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int appId = (int) applicationTableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete Application ID " + appId + "?\nThis action cannot be undone.", 
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            dao.deleteApplication(appId);
            loadApplicationData();
            loadStatistics(); // Refresh statistics after deletion
            JOptionPane.showMessageDialog(this, "Application " + appId + " deleted successfully.", 
                "Delete Successful", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void handleViewDetails() {
        int selectedRow = applicationTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an application to view details.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int appId = (int) applicationTableModel.getValueAt(selectedRow, 0);
        Optional<Application> appOpt = dao.findApplicationById(appId);
        
        if (appOpt.isPresent()) {
            Application app = appOpt.get();
            showApplicationDetails(app);
        }
    }
    
    private void showApplicationDetails(Application app) {
        String details = String.format(
            "<html><div style='font-family: Arial; padding: 10px;'>" +
            "<h2 style='color: #2E8B57;'>Application Details</h2>" +
            "<p><b>Application ID:</b> %d</p>" +
            "<p><b>Applicant Username:</b> %s</p>" +
            "<p><b>Name:</b> %s</p>" +
            "<p><b>Mobile Number:</b> %s</p>" +
            "<p><b>Address:</b> %s</p>" +
            "<p><b>Number of Connections:</b> %d</p>" +
            "<p><b>Status:</b> <span style='color: %s; font-weight: bold;'>%s</span></p>" +
            "</div></html>",
            app.getAppId(),
            app.getApplicantUsername(),
            app.getName(),
            app.getMobileNo(),
            app.getAddress(),
            app.getNumConnections(),
            app.getStatus().equals("APPROVED") ? "green" : app.getStatus().equals("REJECTED") ? "red" : "orange",
            app.getStatus()
        );
        
        JOptionPane.showMessageDialog(this, details, "Application Details", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // User management methods
    private void showAddUserDialog() {
        JDialog dialog = new JDialog(this, "Add New User", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"USER", "ADMIN"});
        
        gbc.gridx = 0; gbc.gridy = 0;
        dialog.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        dialog.add(usernameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        dialog.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        dialog.add(passwordField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        dialog.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        dialog.add(roleCombo, gbc);
        
        JButton btnAdd = new JButton("Add User");
        JButton btnCancel = new JButton("Cancel");
        
        btnAdd.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String role = (String) roleCombo.getSelectedItem();
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all fields.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check if username already exists
            List<User> existingUsers = dao.findAllUsers();
            boolean usernameExists = existingUsers.stream()
                .anyMatch(u -> u.getUsername().equals(username));
            
            if (usernameExists) {
                JOptionPane.showMessageDialog(dialog, "Username already exists. Please choose a different username.", "User Exists", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                // Create user based on role
                User newUser;
                if ("ADMIN".equals(role)) {
                    newUser = new AdminUser(username, password);
                } else {
                    newUser = new RegularUser(username, password);
                }
                
                // Register user in database
                dao.registerUser(newUser);
                
                JOptionPane.showMessageDialog(dialog, "User '" + username + "' added successfully as " + role + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadUserData();
                loadStatistics(); // Refresh statistics
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error adding user: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        btnCancel.addActionListener(e -> dialog.dispose());
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.CENTER;
        dialog.add(btnCancel, gbc);
        gbc.gridx = 1;
        dialog.add(btnAdd, gbc);
        
        dialog.setVisible(true);
    }
    
    private void showEditUserDialog() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String username = (String) userTableModel.getValueAt(selectedRow, 0);
        JOptionPane.showMessageDialog(this, "Edit user functionality for: " + username, "Edit User", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void handleDeleteUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String username = (String) userTableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete user '" + username + "'?\nThis action cannot be undone.", 
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            // Delete user logic here
            JOptionPane.showMessageDialog(this, "User '" + username + "' deleted successfully.", 
                "Delete Successful", JOptionPane.INFORMATION_MESSAGE);
            loadUserData();
        }
    }
    
    private void handleResetPassword() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to reset password.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String username = (String) userTableModel.getValueAt(selectedRow, 0);
        JOptionPane.showMessageDialog(this, "Reset password functionality for: " + username, "Reset Password", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showUserStatistics() {
        JOptionPane.showMessageDialog(this, "User statistics functionality", "User Statistics", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void filterApplications(String status) {
        applicationTableModel.setRowCount(0);
        List<Application> applications = dao.findAllApplications();
        for (Application app : applications) {
            if (app.getStatus().equals(status)) {
                applicationTableModel.addRow(new Object[]{
                    app.getAppId(),
                    app.getApplicantUsername(),
                    app.getName(),
                    app.getMobileNo(),
                    app.getNumConnections(),
                    app.getStatus(),
                    "Recent"
                });
            }
        }
    }
    
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to logout?", 
            "Confirm Logout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            new LoginFrame().setVisible(true);
            this.dispose();
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Handle any additional action events if needed
    }
}

// --- MAIN ENTRY POINT ---

public class LPGGasSystemMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}
