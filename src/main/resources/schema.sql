-- Create database
CREATE DATABASE IF NOT EXISTS ecommerce;
USE ecommerce;

-- Admin table
CREATE TABLE IF NOT EXISTS admin_details (
    admin_id BINARY(16) PRIMARY KEY,
    admin_username VARCHAR(100) NOT NULL,
    admin_email VARCHAR(100) UNIQUE NOT NULL,
    admin_password VARCHAR(255) NOT NULL,
    address TEXT,
    phone VARCHAR(20),
    is_approved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Users table
CREATE TABLE IF NOT EXISTS users (
    user_id BINARY(16) PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    useremail VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    address TEXT,
    gender VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Items table
CREATE TABLE IF NOT EXISTS items (
    itemid BINARY(16) PRIMARY KEY,
    item_name VARCHAR(200) NOT NULL,
    description TEXT,
    item_cost DECIMAL(10, 2) NOT NULL,
    item_quantity INT NOT NULL,
    item_category VARCHAR(100) NOT NULL,
    added_by VARCHAR(100),
    imgname VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (item_category),
    INDEX idx_added_by (added_by)
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BINARY(16),
    item_name VARCHAR(200),
    total DECIMAL(10, 2) NOT NULL,
    payment_by VARCHAR(100) NOT NULL,
    payment_id VARCHAR(255),
    order_status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_payment_by (payment_by)
);

-- Reviews table
CREATE TABLE IF NOT EXISTS reviews (
    review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_text TEXT,
    itemid BINARY(16),
    added_by VARCHAR(100),
    rating INT CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_itemid (itemid)
);

-- Contact table
CREATE TABLE IF NOT EXISTS contact_details (
    contact_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);