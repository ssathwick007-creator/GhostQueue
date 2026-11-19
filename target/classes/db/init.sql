-- ==============================================================================
-- GHOSTQUEUE HEALTHCARE OPERATIONS INTELLIGENCE PLATFORM
-- DATABASE INITIALIZATION SCRIPT (MySQL 8+)
-- Database: ghostqueue_db
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS ghostqueue_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ghostqueue_db;

-- ------------------------------------------------------------------------------
-- 1. PATIENTS TABLE
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS patients (
    patient_id VARCHAR(50) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(20) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(100),
    address VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_patients PRIMARY KEY (patient_id),
    CONSTRAINT chk_patient_age CHECK (age >= 0 AND age <= 130)
) ENGINE=InnoDB;

CREATE INDEX idx_patients_name ON patients(full_name);
CREATE INDEX idx_patients_phone ON patients(phone);

-- ------------------------------------------------------------------------------
-- 2. DOCTORS TABLE
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS doctors (
    doctor_id VARCHAR(50) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    department VARCHAR(100) NOT NULL,
    specialization VARCHAR(150),
    phone VARCHAR(30),
    availability_status VARCHAR(50) DEFAULT 'AVAILABLE',
    CONSTRAINT pk_doctors PRIMARY KEY (doctor_id)
) ENGINE=InnoDB;

CREATE INDEX idx_doctors_department ON doctors(department);
CREATE INDEX idx_doctors_status ON doctors(availability_status);

-- ------------------------------------------------------------------------------
-- 3. APPOINTMENTS TABLE
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointments (
    appointment_id VARCHAR(50) NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    doctor_id VARCHAR(50) NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    appointment_status VARCHAR(30) NOT NULL,
    booking_date DATE NOT NULL,
    cancellation_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_appointments PRIMARY KEY (appointment_id),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) 
        REFERENCES patients(patient_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) 
        REFERENCES doctors(doctor_id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_appointments_lookup ON appointments(appointment_date, doctor_id, appointment_status);
CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_status ON appointments(appointment_status);

-- ------------------------------------------------------------------------------
-- 4. APPOINTMENT_HISTORY TABLE (Audit Log)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointment_history (
    history_id INT AUTO_INCREMENT NOT NULL,
    appointment_id VARCHAR(50) NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remarks VARCHAR(255),
    CONSTRAINT pk_appointment_history PRIMARY KEY (history_id),
    CONSTRAINT fk_history_appointment FOREIGN KEY (appointment_id) 
        REFERENCES appointments(appointment_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_history_appt ON appointment_history(appointment_id);

-- ------------------------------------------------------------------------------
-- 5. WAITING_QUEUE TABLE
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS waiting_queue (
    queue_id VARCHAR(50) NOT NULL,
    patient_id VARCHAR(50) NOT NULL,
    doctor_id VARCHAR(50),
    requested_date DATE NOT NULL,
    preferred_time VARCHAR(50),
    priority_level VARCHAR(20) NOT NULL,
    queue_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_waiting_queue PRIMARY KEY (queue_id),
    CONSTRAINT fk_queue_patient FOREIGN KEY (patient_id) 
        REFERENCES patients(patient_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_queue_doctor FOREIGN KEY (doctor_id) 
        REFERENCES doctors(doctor_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_queue_priority ON waiting_queue(priority_level, queue_status);
CREATE INDEX idx_queue_date ON waiting_queue(requested_date);

-- ==============================================================================
-- FICTIONAL SAMPLE DATA POPULATION (Safe via INSERT IGNORE)
-- ==============================================================================

-- 6 Doctors across major hospital departments
INSERT IGNORE INTO doctors (doctor_id, full_name, department, specialization, phone, availability_status) VALUES
('DOC-101', 'Dr. Robert Vance', 'Cardiology', 'Interventional Cardiology & Stents', '+1 (555) 019-1001', 'AVAILABLE'),
('DOC-102', 'Dr. David Kim', 'Neurology', 'Vestibular & Cognitive Neurology', '+1 (555) 019-1002', 'AVAILABLE'),
('DOC-103', 'Dr. James Wilson', 'Orthopedics', 'Total Joint Reconstruction', '+1 (555) 019-1003', 'AVAILABLE'),
('DOC-104', 'Dr. Evelyn Reed', 'Oncology', 'Medical Oncology Surveillance', '+1 (555) 019-1004', 'AVAILABLE'),
('DOC-105', 'Dr. Sarah Lin', 'Pediatrics', 'Pediatric Pulmonology & Allergy', '+1 (555) 019-1005', 'AVAILABLE'),
('DOC-106', 'Dr. Thomas Wright', 'General Medicine', 'Preventative Internal Medicine', '+1 (555) 019-1006', 'AVAILABLE');

-- 20 Fictional Patients
INSERT IGNORE INTO patients (patient_id, full_name, age, gender, phone, email, address) VALUES
('PT-1001', 'Eleanor Vance', 68, 'Female', '+1 (555) 234-8910', 'e.vance@fictional-hospital.org', '424 Elm St, Medical District'),
('PT-1002', 'Arthur Pendelton', 54, 'Male', '+1 (555) 876-1234', 'arthur.p@fictional-hospital.org', '108 Oakwood Ave, Greenfield'),
('PT-1003', 'Maria Gomez', 42, 'Female', '+1 (555) 345-6789', 'maria.gomez@fictional-hospital.org', '712 Pine Crest Dr, Riverside'),
('PT-1004', 'David Sterling', 29, 'Male', '+1 (555) 987-6543', 'd.sterling@fictional-hospital.org', '89 Maple Ridge Rd, Lakeside'),
('PT-1005', 'Amina Al-Mansoor', 37, 'Female', '+1 (555) 456-7890', 'amina.m@fictional-hospital.org', '330 Cedar Lane, North Heights'),
('PT-1006', 'Lucas Chen', 9, 'Male', '+1 (555) 678-9012', 'chen.family@fictional-hospital.org', '554 Birchwood Ter, Fairview'),
('PT-1007', 'Chloe Bennett', 33, 'Female', '+1 (555) 789-0123', 'c.bennett@fictional-hospital.org', '614 Willow Pass, Clearfield'),
('PT-1008', 'Samuel O Connor', 71, 'Male', '+1 (555) 890-1234', 'sam.oconnor@fictional-hospital.org', '230 Sycamore Blvd, Brookside'),
('PT-1009', 'Fatima Zahra', 46, 'Female', '+1 (555) 901-2345', 'f.zahra@fictional-hospital.org', '478 Chestnut St, Highlands'),
('PT-1010', 'Brandon Lee', 58, 'Male', '+1 (555) 012-3456', 'brandon.lee@fictional-hospital.org', '124 Magnolia Ct, Glenwood'),
('PT-1011', 'Priya Sharma', 28, 'Female', '+1 (555) 123-4567', 'priya.s@fictional-hospital.org', '905 Ashwood Way, Sunnyvale'),
('PT-1012', 'Jonathan Hayes', 63, 'Male', '+1 (555) 234-5678', 'j.hayes@fictional-hospital.org', '332 Hickory Hollow, Oakridge'),
('PT-1013', 'Victoria Ross', 50, 'Female', '+1 (555) 345-6780', 'v.ross@fictional-hospital.org', '781 Beech Ave, Silver Spring'),
('PT-1014', 'Daniel Kovacs', 39, 'Male', '+1 (555) 456-7891', 'd.kovacs@fictional-hospital.org', '118 Poplar Grove, Woodbury'),
('PT-1015', 'Hannah Schmidt', 62, 'Female', '+1 (555) 567-8902', 'h.schmidt@fictional-hospital.org', '453 Walnut Cir, Mill Creek'),
('PT-1016', 'George Washington Carver', 74, 'Male', '+1 (555) 678-9013', 'g.carver@fictional-hospital.org', '892 Cypress Point, Lakeview'),
('PT-1017', 'Isabella Morales', 31, 'Female', '+1 (555) 789-0124', 'i.morales@fictional-hospital.org', '219 Alder Rd, Southgate'),
('PT-1018', 'Ethan Walker', 48, 'Male', '+1 (555) 890-1235', 'e.walker@fictional-hospital.org', '641 Spruce St, Eastbrook'),
('PT-1019', 'Zoe Patel', 24, 'Female', '+1 (555) 901-2346', 'zoe.patel@fictional-hospital.org', '375 Laurel Dr, West Valley'),
('PT-1020', 'Marcus Aurelius Sterling', 59, 'Male', '+1 (555) 012-3457', 'm.sterling@fictional-hospital.org', '512 Redwood Blvd, Summit');

-- 30 Fictional Appointments with realistic dates, statuses, and doctors
INSERT IGNORE INTO appointments (appointment_id, patient_id, doctor_id, appointment_date, appointment_time, appointment_status, booking_date, cancellation_reason) VALUES
('APT-1001', 'PT-1001', 'DOC-101', CURRENT_DATE, '08:30:00', 'COMPLETED', DATE_SUB(CURRENT_DATE, INTERVAL 14 DAY), NULL),
('APT-1002', 'PT-1004', 'DOC-103', CURRENT_DATE, '09:15:00', 'CANCELLED', DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY), 'Patient called: transportation breakdown'),
('APT-1003', 'PT-1003', 'DOC-102', CURRENT_DATE, '10:00:00', 'CONFIRMED', DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), NULL),
('APT-1004', 'PT-1007', 'DOC-106', CURRENT_DATE, '11:00:00', 'CONFIRMED', DATE_SUB(CURRENT_DATE, INTERVAL 25 DAY), NULL),
('APT-1005', 'PT-1008', 'DOC-101', CURRENT_DATE, '11:45:00', 'CONFIRMED', DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), NULL),
('APT-1006', 'PT-1010', 'DOC-104', CURRENT_DATE, '13:30:00', 'BOOKED', DATE_SUB(CURRENT_DATE, INTERVAL 35 DAY), NULL),
('APT-1007', 'PT-1006', 'DOC-105', CURRENT_DATE, '14:15:00', 'CONFIRMED', DATE_SUB(CURRENT_DATE, INTERVAL 4 DAY), NULL),
('APT-1008', 'PT-1013', 'DOC-102', CURRENT_DATE, '15:00:00', 'BOOKED', DATE_SUB(CURRENT_DATE, INTERVAL 28 DAY), NULL),
('APT-1009', 'PT-1014', 'DOC-106', CURRENT_DATE, '15:45:00', 'CONFIRMED', DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), NULL),
('APT-1010', 'PT-1011', 'DOC-106', CURRENT_DATE, '16:30:00', 'NO_SHOW', DATE_SUB(CURRENT_DATE, INTERVAL 18 DAY), 'Unannounced absence at appointment time'),

-- Tomorrow (Day +1)
('APT-1011', 'PT-1002', 'DOC-101', DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '09:00:00', 'CONFIRMED', DATE_SUB(CURRENT_DATE, INTERVAL 12 DAY), NULL),
('APT-1012', 'PT-1005', 'DOC-104', DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '10:30:00', 'CONFIRMED', DATE_SUB(CURRENT_DATE, INTERVAL 8 DAY), NULL),
('APT-1013', 'PT-1012', 'DOC-103', DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '11:15:00', 'CONFIRMED', DATE_SUB(CURRENT_DATE, INTERVAL 9 DAY), NULL),
('APT-1014', 'PT-1015', 'DOC-104', DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '13:00:00', 'BOOKED', DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), NULL),
('APT-1015', 'PT-1009', 'DOC-102', DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '14:00:00', 'CONFIRMED', DATE_SUB(CURRENT_DATE, INTERVAL 11 DAY), NULL),
('APT-1016', 'PT-1016', 'DOC-101', DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '15:30:00', 'BOOKED', DATE_SUB(CURRENT_DATE, INTERVAL 14 DAY), NULL),

-- Day +2
('APT-1017', 'PT-1017', 'DOC-105', DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), '09:30:00', 'CONFIRMED', DATE_SUB(CURRENT_DATE, INTERVAL 6 DAY), NULL),
('APT-1018', 'PT-1018', 'DOC-106', DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), '10:15:00', 'CONFIRMED', DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), NULL),
('APT-1019', 'PT-1019', 'DOC-103', DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), '11:00:00', 'BOOKED', DATE_SUB(CURRENT_DATE, INTERVAL 21 DAY), NULL),
('APT-1020', 'PT-1020', 'DOC-104', DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), '14:30:00', 'CONFIRMED', DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), NULL),

-- Day +3
('APT-1021', 'PT-1001', 'DOC-106', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), '09:00:00', 'CONFIRMED', DATE_SUB(CURRENT_DATE, INTERVAL 8 DAY), NULL),
('APT-1022', 'PT-1003', 'DOC-102', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), '10:45:00', 'BOOKED', DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), NULL),
('APT-1023', 'PT-1007', 'DOC-106', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), '11:30:00', 'CONFIRMED', DATE_SUB(CURRENT_DATE, INTERVAL 16 DAY), NULL),
('APT-1024', 'PT-1010', 'DOC-104', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), '13:15:00', 'CANCELLED', DATE_SUB(CURRENT_DATE, INTERVAL 20 DAY), 'Patient requested rescheduling to next week'),

-- Historical Completed & Past Appointments
('APT-1025', 'PT-1001', 'DOC-101', DATE_SUB(CURRENT_DATE, INTERVAL 28 DAY), '09:00:00', 'COMPLETED', DATE_SUB(CURRENT_DATE, INTERVAL 45 DAY), NULL),
('APT-1026', 'PT-1002', 'DOC-101', DATE_SUB(CURRENT_DATE, INTERVAL 21 DAY), '10:00:00', 'NO_SHOW', DATE_SUB(CURRENT_DATE, INTERVAL 35 DAY), 'Unreported absence'),
('APT-1027', 'PT-1008', 'DOC-101', DATE_SUB(CURRENT_DATE, INTERVAL 14 DAY), '11:00:00', 'COMPLETED', DATE_SUB(CURRENT_DATE, INTERVAL 24 DAY), NULL),
('APT-1028', 'PT-1012', 'DOC-103', DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), '14:00:00', 'COMPLETED', DATE_SUB(CURRENT_DATE, INTERVAL 20 DAY), NULL),
('APT-1029', 'PT-1004', 'DOC-103', DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY), '15:00:00', 'CANCELLED', DATE_SUB(CURRENT_DATE, INTERVAL 14 DAY), 'Work conflict'),
('APT-1030', 'PT-1014', 'DOC-106', DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY), '09:30:00', 'COMPLETED', DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), NULL);

-- Appointment History for audit trail
INSERT IGNORE INTO appointment_history (appointment_id, previous_status, new_status, changed_at, remarks) VALUES
('APT-1001', 'BOOKED', 'CONFIRMED', DATE_SUB(NOW(), INTERVAL 2 DAY), 'Automated SMS confirmation received'),
('APT-1001', 'CONFIRMED', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 2 HOUR), 'Consultation completed by Dr. Vance'),
('APT-1002', 'BOOKED', 'CANCELLED', DATE_SUB(NOW(), INTERVAL 3 HOUR), 'Patient called to report transport delay'),
('APT-1010', 'CONFIRMED', 'NO_SHOW', DATE_SUB(NOW(), INTERVAL 1 HOUR), 'Flagged no-show by reception');

-- 10 Waiting Queue records
INSERT IGNORE INTO waiting_queue (queue_id, patient_id, doctor_id, requested_date, preferred_time, priority_level, queue_status) VALUES
('WQ-101', 'PT-1008', 'DOC-101', CURRENT_DATE, 'Morning (08:00 - 12:00)', 'URGENT', 'WAITING'),
('WQ-102', 'PT-1012', 'DOC-103', CURRENT_DATE, 'Morning (09:00 - 12:00)', 'HIGH', 'WAITING'),
('WQ-103', 'PT-1003', 'DOC-102', CURRENT_DATE, 'Afternoon (13:00 - 17:00)', 'STANDARD', 'WAITING'),
('WQ-104', 'PT-1005', 'DOC-104', CURRENT_DATE, 'Anytime (Immediate Standby)', 'HIGH', 'STANDBY'),
('WQ-105', 'PT-1006', 'DOC-105', DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), 'Afternoon', 'STANDARD', 'WAITING'),
('WQ-106', 'PT-1011', 'DOC-106', CURRENT_DATE, 'Anytime', 'URGENT', 'OFFERED'),
('WQ-107', 'PT-1014', 'DOC-106', DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), 'Morning', 'STANDARD', 'WAITING'),
('WQ-108', 'PT-1002', 'DOC-101', CURRENT_DATE, 'Morning', 'HIGH', 'WAITING'),
('WQ-109', 'PT-1017', 'DOC-105', DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), 'Morning', 'STANDARD', 'WAITING'),
('WQ-110', 'PT-1019', 'DOC-103', DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), 'Afternoon', 'HIGH', 'WAITING');

-- -------------------------------------------------------------
-- TABLE: APPOINTMENT_PREDICTIONS (PHASE 3 INTELLIGENT PREDICTION ENGINE)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointment_predictions (
    prediction_id VARCHAR(64) PRIMARY KEY,
    appointment_id VARCHAR(32) NOT NULL,
    model_version VARCHAR(32) NOT NULL,
    no_show_probability DECIMAL(5, 4) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    prediction_method VARCHAR(32) NOT NULL,
    contributing_factors TEXT NOT NULL,
    suggested_action VARCHAR(255) NOT NULL,
    predicted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actual_outcome VARCHAR(16) DEFAULT NULL,
    evaluation_status VARCHAR(32) DEFAULT 'PENDING',
    CONSTRAINT fk_pred_appointment FOREIGN KEY (appointment_id) 
        REFERENCES appointments (appointment_id) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_pred_appointment (appointment_id),
    INDEX idx_pred_risk_level (risk_level),
    INDEX idx_pred_predicted_at (predicted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

