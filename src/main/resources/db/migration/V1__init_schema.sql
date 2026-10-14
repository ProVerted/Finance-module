CREATE TABLE department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    code VARCHAR(10) NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(15) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    designation VARCHAR(50),
    department_id BIGINT NOT NULL,
    basic_salary DECIMAL(12,2) NOT NULL,
    joined_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_employee_department
        FOREIGN KEY (department_id)
        REFERENCES department(id)
);

CREATE TABLE working_hour_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    day_type VARCHAR(20) NOT NULL UNIQUE,
    expected_in TIME NOT NULL,
    expected_out TIME NOT NULL,
    ot_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    ot_multiplier DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE holiday_calendar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    description VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE attendance_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    check_in DATETIME,
    check_out DATETIME,
    day_type VARCHAR(20) NOT NULL,

    CONSTRAINT fk_attendance_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(id),

    CONSTRAINT uk_attendance_employee_date
        UNIQUE (employee_id, attendance_date)
);

CREATE TABLE overtime_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    total_ot_minutes BIGINT NOT NULL DEFAULT 0,
    hourly_rate DECIMAL(12,2) NOT NULL,
    ot_amount DECIMAL(12,2) NOT NULL,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by VARCHAR(50),
    approved_at DATETIME,

    CONSTRAINT fk_overtime_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(id),

    CONSTRAINT uk_overtime_employee_month
        UNIQUE (employee_id, year, month)
);

CREATE TABLE payslip (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    basic_salary DECIMAL(12,2) NOT NULL,
    ot_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    gross_pay DECIMAL(12,2) NOT NULL,
    employee_epf DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    employer_epf DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    employer_etf DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    deductions DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_deductions DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    net_pay DECIMAL(12,2) NOT NULL,
    generated_at DATETIME NOT NULL,
    finalized BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_payslip_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee(id),

    CONSTRAINT uk_payslip_employee_month
        UNIQUE (employee_id, year, month)
);

CREATE TABLE user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE
);



