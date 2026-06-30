-- TeleConnect Module 2.7 - Telecom Analytics & Reporting
-- DDL for all tables. Run once against teleconnect_db.

-- Core module entity
CREATE TABLE IF NOT EXISTS telecom_reports (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scope ENUM('REGION','PLAN','SEGMENT','PERIOD') NOT NULL,
    scope_value VARCHAR(100) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    metrics TEXT NOT NULL,
    generated_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_by BIGINT,
    INDEX idx_report_scope_period (scope, period_start, period_end)
);

-- Source entities (shared with other modules in teleconnect_db)
CREATE TABLE IF NOT EXISTS subscriber_accounts (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscriber_id BIGINT NOT NULL,
    account_type ENUM('PREPAID','POSTPAID','ENTERPRISE') NOT NULL,
    registration_date DATE NOT NULL,
    status ENUM('ACTIVE','SUSPENDED','TERMINATED') NOT NULL,
    kyc_status VARCHAR(50),
    region_id BIGINT,
    INDEX idx_sa_status_reg (status, registration_date)
);

CREATE TABLE IF NOT EXISTS sim_lines (
    line_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    msisdn VARCHAR(20) NOT NULL,
    iccid VARCHAR(20),
    activation_date DATE NOT NULL,
    service_type VARCHAR(30),
    status ENUM('ACTIVE','DEACTIVATED','SUSPENDED','PORTED_OUT') NOT NULL,
    INDEX idx_sim_account_status (account_id, status, activation_date)
);

CREATE TABLE IF NOT EXISTS billing_cycles (
    cycle_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    cycle_start DATE NOT NULL,
    cycle_end DATE NOT NULL,
    status ENUM('OPEN','GENERATED','CLOSED') NOT NULL
);

CREATE TABLE IF NOT EXISTS invoices (
    invoice_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    cycle_id BIGINT NOT NULL,
    plan_charges DECIMAL(10,2) NOT NULL,
    excess_charges DECIMAL(10,2) NOT NULL,
    add_on_charges DECIMAL(10,2) NOT NULL,
    taxes DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    due_date DATE NOT NULL,
    status ENUM('GENERATED','SENT','PAID','OVERDUE','DISPUTED') NOT NULL,
    created_at DATETIME,
    INDEX idx_invoice_account_cycle_status (account_id, cycle_id, status)
);

CREATE TABLE IF NOT EXISTS billing_disputes (
    dispute_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    subscriber_id BIGINT NOT NULL,
    disputed_amount DECIMAL(10,2) NOT NULL,
    raised_date DATE NOT NULL,
    status ENUM('OPEN','UNDER_REVIEW','RESOLVED') NOT NULL
);

CREATE TABLE IF NOT EXISTS usage_summaries (
    summary_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    line_id BIGINT NOT NULL,
    billing_cycle_id BIGINT NOT NULL,
    data_used_mb BIGINT NOT NULL DEFAULT 0,
    voice_used_min BIGINT NOT NULL DEFAULT 0,
    sms_used BIGINT NOT NULL DEFAULT 0,
    data_remaining_mb BIGINT,
    voice_remaining_min BIGINT,
    last_updated DATETIME,
    INDEX idx_usage_line_cycle (line_id, billing_cycle_id)
);

CREATE TABLE IF NOT EXISTS fault_tickets (
    ticket_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    line_id BIGINT,
    fault_type VARCHAR(50),
    priority ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
    raised_date DATETIME NOT NULL,
    resolved_date DATETIME,
    status ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED','ESCALATED') NOT NULL,
    INDEX idx_fault_raised_priority_status (raised_date, priority, status)
);
