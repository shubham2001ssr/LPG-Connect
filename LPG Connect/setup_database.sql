-- LPG Gas Connection System Database Setup
-- Run this script in MySQL to create the database and tables

CREATE DATABASE IF NOT EXISTS lpg_system;
USE lpg_system;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL
);

-- Create applications table
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
);

-- Insert default users
INSERT IGNORE INTO users (username, password, role) VALUES 
('admin', 'admin123', 'ADMIN'),
('user1', 'user123', 'USER');

-- Insert sample application
INSERT IGNORE INTO applications (applicant_username, name, mobile_no, address, num_connections, status) 
VALUES ('user1', 'Priya Sharma', '9876543210', '123, Main St.', 2, 'PENDING');

-- Show tables
SHOW TABLES;
SELECT * FROM users;
SELECT * FROM applications;
