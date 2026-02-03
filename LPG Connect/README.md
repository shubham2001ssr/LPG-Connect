# LPG Gas Connection System

LPG Gas Connection System is a desktop application built with Java Swing that digitises the lifecycle of household LPG requests. It provides dedicated experiences for applicants and administrators, applies consistent validation at the UI boundary, and supports both in-memory and MySQL-backed persistence via the DAO pattern.

## Features
- User registration, login, and role-based routing for admin and regular users.
- CRUD workflow for LPG connection applications, including approval and rejection controls.
- Centralised validation to guard against bad input (mobile number format, required fields, positive connection count).
- Pluggable persistence layer with automatic fallback to an in-memory store when MySQL is unavailable.
- Utility scripts and SQL to bootstrap the MySQL schema and seed sample data.

## Project Layout
- `src/` – all Java sources, including the Swing UI, DAO implementations, and helper utilities.
- `create_database.sql` / `setup_database.sql` – helpers for provisioning the `lpg_system` schema.
- `lib/mysql-connector-j-9.4.0.jar` – bundled JDBC driver dependency.
- `Documentation.md` – extended architecture notes and UI guidance.

## Prerequisites
- JDK 17 or newer (text blocks are used throughout the code).
- MySQL Server 8.x (optional; the app falls back to in-memory storage if the database is unreachable).
- macOS/Linux shell commands below use `:` for classpath separation; replace with `;` on Windows.

## Getting Started
1. **Clone the project**
   ```bash
   git clone <your-fork-url>
   cd LPG\ Connect
   ```
2. **Install dependencies**
   - Ensure the bundled driver `lib/mysql-connector-j-9.4.0.jar` remains alongside the sources.
   - Update MySQL connection credentials in `LPGGasSystemMain.java` or `create_database.java` if they differ from your local setup.
3. **Compile the project**
   ```bash
   mkdir -p out
   javac -d out -cp "lib/mysql-connector-j-9.4.0.jar:src" src/*.java
   ```
4. **Set up the database (optional but recommended)**
   - Using Java helper:
     ```bash
     java -cp "lib/mysql-connector-j-9.4.0.jar:out" create_database
     ```
   - Or run the SQL scripts directly:
     ```bash
     mysql -u root -p < create_database.sql
     mysql -u root -p lpg_system < setup_database.sql
     ```

## Running the Application
Compile (if you have not already done so) and launch the Swing UI (with MySQL if available, or in-memory fallback otherwise):
```bash
mkdir -p out
javac -d out -cp "lib/mysql-connector-j-9.4.0.jar:src" src/*.java
java  -cp "lib/mysql-connector-j-9.4.0.jar:out" LPGGasSystemMain
```

### Default Accounts
- Admin: `admin` / `admin123`
- User: `user1` / `user123`

Once logged in, regular users can submit new LPG connection requests and track their status. Administrators can view the queue, approve or reject requests, and manage user accounts.

## Verification and Troubleshooting
- Run backend smoke checks without the UI:
  ```bash
  mkdir -p out
  javac -d out -cp "lib/mysql-connector-j-9.4.0.jar:src" src/*.java
  java  -cp "lib/mysql-connector-j-9.4.0.jar:out" simple_backend_test
  ```
- If the MySQL connection fails, the DAO factory prints a warning and switches to the in-memory store. Verify your database credentials and that the `lpg_system` schema exists.
- Ensure the MySQL Connector/J driver remains on the classpath; otherwise the JDBC driver will not load.

## Next Steps
- Capture UI screenshots and place them under `snapshots/` as suggested in `Documentation.md`.
- Package the application with a build tool (e.g., Gradle or Maven) to simplify dependency management.
- Expand automated tests to cover Swing interactions or integrate with CI before publishing the repository.
