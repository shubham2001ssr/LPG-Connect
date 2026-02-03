# LPG Gas Connection System Documentation

## Abstract
The LPG Gas Connection System is a Java Swing application that digitalises household LPG connection requests. It supports differentiated user roles, robust input validation, and dual persistence options (in-memory and MySQL). Administrators can review and process applications while end users submit and track their requests through a desktop interface.

## Introduction
The project demonstrates a full-stack desktop workflow for LPG connection lifecycle management. Users can register, authenticate, submit new applications, and monitor their status. Administrators manage the queue, update decisions, and maintain user accounts. The solution emphasises:
- A shared domain model that encapsulates users and LPG applications.
- Pluggable persistence using the DAO pattern with MySQL bootstrap scripts.
- Consistent validation at the UI boundary to guarantee clean input.
- Clear separation between presentation (Swing), business logic, and data access layers.

## Modules Explanation
### 1. Validation and Utilities
- `ValidationException` and `Validator` enforce mobile number formatting, non-empty fields, and positive connection counts before any persistence logic runs.
- Centralised validation prevents duplicate checks across frames and creates meaningful error messaging.

### 2. Domain Model
- `User`, `RegularUser`, and `AdminUser` establish role-based inheritance used throughout authentication and UI routing.
- `Application` encapsulates a gas connection request, tracks status transitions, and provides a unique identifier for CRUD operations.

### 3. Persistence Layer
- `ApplicationDAO` defines the CRUD contract for authentication, user registration, and application management.
- `InMemoryDAO` offers a lightweight store for demos and automated tests.
- `MySQLDAO` connects to a `lpg_system` schema, auto-creates schema objects, seeds default accounts, and persists data via JDBC.
- `DAOFactory` selects the MySQL implementation when available and gracefully falls back to the in-memory variant, enabling the UI to operate offline.

### 4. Presentation Layer (Swing UI)
- `LoginFrame` and `RegistrationFrame` provide onboarding workflows with themed layouts and actionable feedback.
- `UserDashboardFrame`, `NewApplicationFrame`, and `ViewMyApplicationFrame` give end users menu-driven access to submit and review applications, including guards that prevent duplicate active requests.
- `AdminDashboardFrame` exposes tabbed management for applications and accounts, including approval, rejection, status editing, and password resets.

### 5. Database Setup and Tooling
- `create_database.sql` and `setup_database.sql` initialise the `lpg_system` schema and tables.
- Utility runners such as `create_database.java`, `check_backend_data.java`, and `test_database.java` verify connectivity, seeding, and data consistency.
- Library dependencies are provided under `lib/` to support JDBC access.

### 6. Testing and Verification Scripts
- `simple_backend_test.java`, `verify_persistence.java`, and `test_data_persistence.java` exercise persistence paths to confirm read/write operations against MySQL.
- These scripts are intentionally decoupled from the UI so backend checks can run in headless environments.

## Code Snippets
```java
// Input validation guard (LPGGasSystemMain.java)
Validator.validateNotEmpty(name, "Name");
Validator.validateMobileNo(mobile);
Validator.validateNotEmpty(address, "Address");
Validator.validatePositiveInteger(connections);
```

```java
// DAO factory with automatic fallback (LPGGasSystemMain.java)
class DAOFactory {
    public static ApplicationDAO createDAO() {
        try {
            return new MySQLDAO();
        } catch (Exception e) {
            System.err.println("MySQL database not available, falling back to in-memory storage: " + e.getMessage());
            return new InMemoryDAO();
        }
    }
}
```

```java
// Role-based login routing (LPGGasSystemMain.java)
Optional<User> userOpt = dao.validateUser(username, password);
if (userOpt.isPresent()) {
    User user = userOpt.get();
    if (user.getRole().equals("ADMIN")) {
        new AdminDashboardFrame().setVisible(true);
    } else {
        new UserDashboardFrame(username).setVisible(true);
    }
} else {
    JOptionPane.showMessageDialog(this, "Invalid Username or Password.", "Error", JOptionPane.ERROR_MESSAGE);
}
```

## Snapshots
Capture and store UI screenshots under a `snapshots/` directory to accompany this documentation.

| Screen | Description | Suggested Path |
| --- | --- | --- |
| Login | Branded login with credential form and gradient banner. | `snapshots/login_screen.png` |
| User Dashboard | Landing page showing quick actions for new requests and status checks. | `snapshots/user_dashboard.png` |
| New Application | Form with validation for applicant details and connection count. | `snapshots/new_application_form.png` |
| Admin Dashboard | Tabbed interface for application queue and user administration. | `snapshots/admin_dashboard.png` |

## Conclusion
The LPG Gas Connection System showcases a role-aware Swing application that can operate with or without a relational backend. By decoupling validation, data access, and presentation, the project stays maintainable and extensible. Future enhancements can focus on richer reporting, audit logging, and automated packaging without reworking the core architecture described above.

