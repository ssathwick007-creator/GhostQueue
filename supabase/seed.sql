-- =============================================================================
-- GHOSTQUEUE HEALTHCARE INTELLIGENCE PLATFORM
-- Seed Data: System Settings, Departments, Doctors, Patients, Appointments, ML
-- =============================================================================

-- 1. System Settings
INSERT INTO public.application_settings (setting_key, setting_value, description) VALUES
('hospital.name', 'Metropolitan Academic Medical Center', 'Primary hospital display name'),
('hospital.campus', 'Main Medical Pavilion (North Tower)', 'Facility campus or pavilion location'),
('hospital.hours', '08:00 AM - 17:30 PM (Mon - Sat)', 'Clinical operating hours'),
('hospital.timezone', 'Eastern Time (US & Canada, UTC-05:00)', 'Hospital operating timezone'),
('threshold.high_risk', '0.65', 'Threshold probability for classifying an appointment as High Risk (>= 65%)'),
('threshold.medium_risk', '0.30', 'Threshold probability for classifying an appointment as Medium Risk (>= 30%)'),
('reallocation.strict_confirmation', 'true', 'Require explicit administrative confirmation before reallocating slots'),
('reallocation.urgent_priority_boost', '20', 'Compatibility score weight boost for Urgent priority queue entries'),
('reminders.auto_high_risk', 'true', 'Automatically create reminder tasks for high risk appointments'),
('reminders.default_lead_hours', '48', 'Hours in advance to dispatch appointment reminders')
ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value;

-- 2. Departments
INSERT INTO public.departments (department_id, name, description) VALUES
('CARDIOLOGY', 'Cardiology', 'Cardiovascular health, diagnostics, stenting & cardiac intervention'),
('NEUROLOGY', 'Neurology', 'Vestibular, cognitive neurology, epilepsy & neurodegenerative disorders'),
('ORTHOPEDICS', 'Orthopedics', 'Total joint reconstruction, sports medicine & spine care'),
('ONCOLOGY', 'Oncology', 'Medical oncology, cancer chemotherapy surveillance & immunotherapy'),
('PEDIATRICS', 'Pediatrics', 'Comprehensive child care, pediatric pulmonology & neonatology'),
('GENERAL_MEDICINE', 'General Medicine', 'Preventative internal medicine, annual wellness & primary care'),
('DERMATOLOGY', 'Dermatology', 'Clinical dermatopathology, skin cancer screening & cosmetic laser')
ON CONFLICT (department_id) DO UPDATE SET name = EXCLUDED.name;

-- 3. Demo Auth Users & Profiles (Admin, Doctors, Patients)
-- Using fixed UUIDs for deterministic seeding:
-- Admin:    a0000000-0000-0000-0000-000000000001
-- Doctor 1: d0000000-0000-0000-0000-000000000001 (Dr. Robert Vance)
-- Doctor 2: d0000000-0000-0000-0000-000000000002 (Dr. James Wilson)
-- Doctor 3: d0000000-0000-0000-0000-000000000003 (Dr. Alexander Hayes - Pending)
-- Patient 1: p0000000-0000-0000-0000-000000000001 (Eleanor Vance)
-- Patient 2: p0000000-0000-0000-0000-000000000002 (Arthur Pendelton)

-- Admin User
INSERT INTO auth.users (
    instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data, is_sso_user, is_anonymous, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000000',
    'a0000000-0000-0000-0000-000000000001',
    'authenticated', 'authenticated',
    'admin@ghostqueue.com',
    crypt('Admin@123', gen_salt('bf')),
    NOW(),
    '{"provider":"email","providers":["email"]}'::jsonb,
    '{"full_name":"Dr. Sarah Chen","role":"ADMIN"}'::jsonb,
    false, false, NOW(), NOW()
) ON CONFLICT (id) DO UPDATE SET encrypted_password = crypt('Admin@123', gen_salt('bf'));

INSERT INTO auth.identities (id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    jsonb_build_object('sub', 'a0000000-0000-0000-0000-000000000001', 'email', 'admin@ghostqueue.com'),
    'email', 'admin@ghostqueue.com', NOW(), NOW(), NOW()
) ON CONFLICT (provider, provider_id) DO NOTHING;

INSERT INTO public.profiles (id, full_name, email, phone, role, account_status)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Dr. Sarah Chen',
    'admin@ghostqueue.com',
    '+1 (555) 010-0001',
    'ADMIN',
    'ACTIVE'
) ON CONFLICT (id) DO UPDATE SET role = 'ADMIN', account_status = 'ACTIVE';

-- Doctor 1 (Dr. Robert Vance - Cardiology)
INSERT INTO auth.users (
    instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data, is_sso_user, is_anonymous, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000000',
    'd0000000-0000-0000-0000-000000000001',
    'authenticated', 'authenticated',
    'doctor.vance@ghostqueue.com',
    crypt('Doctor@123', gen_salt('bf')),
    NOW(),
    '{"provider":"email","providers":["email"]}'::jsonb,
    '{"full_name":"Dr. Robert Vance","role":"DOCTOR"}'::jsonb,
    false, false, NOW(), NOW()
) ON CONFLICT (id) DO UPDATE SET encrypted_password = crypt('Doctor@123', gen_salt('bf'));

INSERT INTO auth.identities (id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at)
VALUES (
    'd0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    jsonb_build_object('sub', 'd0000000-0000-0000-0000-000000000001', 'email', 'doctor.vance@ghostqueue.com'),
    'email', 'doctor.vance@ghostqueue.com', NOW(), NOW(), NOW()
) ON CONFLICT (provider, provider_id) DO NOTHING;

INSERT INTO public.profiles (id, full_name, email, phone, role, account_status)
VALUES (
    'd0000000-0000-0000-0000-000000000001',
    'Dr. Robert Vance',
    'doctor.vance@ghostqueue.com',
    '+1 (555) 019-1001',
    'DOCTOR',
    'ACTIVE'
) ON CONFLICT (id) DO UPDATE SET role = 'DOCTOR', account_status = 'ACTIVE';

-- Doctor 2 (Dr. James Wilson - Orthopedics)
INSERT INTO auth.users (
    instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data, is_sso_user, is_anonymous, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000000',
    'd0000000-0000-0000-0000-000000000002',
    'authenticated', 'authenticated',
    'doctor.wilson@ghostqueue.com',
    crypt('Doctor@123', gen_salt('bf')),
    NOW(),
    '{"provider":"email","providers":["email"]}'::jsonb,
    '{"full_name":"Dr. James Wilson","role":"DOCTOR"}'::jsonb,
    false, false, NOW(), NOW()
) ON CONFLICT (id) DO UPDATE SET encrypted_password = crypt('Doctor@123', gen_salt('bf'));

INSERT INTO auth.identities (id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at)
VALUES (
    'd0000000-0000-0000-0000-000000000002',
    'd0000000-0000-0000-0000-000000000002',
    jsonb_build_object('sub', 'd0000000-0000-0000-0000-000000000002', 'email', 'doctor.wilson@ghostqueue.com'),
    'email', 'doctor.wilson@ghostqueue.com', NOW(), NOW(), NOW()
) ON CONFLICT (provider, provider_id) DO NOTHING;

INSERT INTO public.profiles (id, full_name, email, phone, role, account_status)
VALUES (
    'd0000000-0000-0000-0000-000000000002',
    'Dr. James Wilson',
    'doctor.wilson@ghostqueue.com',
    '+1 (555) 019-1003',
    'DOCTOR',
    'ACTIVE'
) ON CONFLICT (id) DO UPDATE SET role = 'DOCTOR', account_status = 'ACTIVE';

-- Doctor 3 (Dr. Alexander Hayes - Pending Approval demo)
INSERT INTO auth.users (
    instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data, is_sso_user, is_anonymous, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000000',
    'd0000000-0000-0000-0000-000000000003',
    'authenticated', 'authenticated',
    'doctor.pending@ghostqueue.com',
    crypt('Doctor@123', gen_salt('bf')),
    NOW(),
    '{"provider":"email","providers":["email"]}'::jsonb,
    '{"full_name":"Dr. Alexander Hayes","role":"DOCTOR"}'::jsonb,
    false, false, NOW(), NOW()
) ON CONFLICT (id) DO UPDATE SET encrypted_password = crypt('Doctor@123', gen_salt('bf'));

INSERT INTO auth.identities (id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at)
VALUES (
    'd0000000-0000-0000-0000-000000000003',
    'd0000000-0000-0000-0000-000000000003',
    jsonb_build_object('sub', 'd0000000-0000-0000-0000-000000000003', 'email', 'doctor.pending@ghostqueue.com'),
    'email', 'doctor.pending@ghostqueue.com', NOW(), NOW(), NOW()
) ON CONFLICT (provider, provider_id) DO NOTHING;

INSERT INTO public.profiles (id, full_name, email, phone, role, account_status)
VALUES (
    'd0000000-0000-0000-0000-000000000003',
    'Dr. Alexander Hayes',
    'doctor.pending@ghostqueue.com',
    '+1 (555) 019-1008',
    'DOCTOR',
    'PENDING_APPROVAL'
) ON CONFLICT (id) DO UPDATE SET role = 'DOCTOR', account_status = 'PENDING_APPROVAL';

-- Patient 1 (Eleanor Vance)
INSERT INTO auth.users (
    instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data, is_sso_user, is_anonymous, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000000',
    'p0000000-0000-0000-0000-000000000001',
    'authenticated', 'authenticated',
    'patient.eleanor@ghostqueue.com',
    crypt('Patient@123', gen_salt('bf')),
    NOW(),
    '{"provider":"email","providers":["email"]}'::jsonb,
    '{"full_name":"Eleanor Vance","role":"PATIENT"}'::jsonb,
    false, false, NOW(), NOW()
) ON CONFLICT (id) DO UPDATE SET encrypted_password = crypt('Patient@123', gen_salt('bf'));

INSERT INTO auth.identities (id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at)
VALUES (
    'p0000000-0000-0000-0000-000000000001',
    'p0000000-0000-0000-0000-000000000001',
    jsonb_build_object('sub', 'p0000000-0000-0000-0000-000000000001', 'email', 'patient.eleanor@ghostqueue.com'),
    'email', 'patient.eleanor@ghostqueue.com', NOW(), NOW(), NOW()
) ON CONFLICT (provider, provider_id) DO NOTHING;

INSERT INTO public.profiles (id, full_name, email, phone, role, account_status)
VALUES (
    'p0000000-0000-0000-0000-000000000001',
    'Eleanor Vance',
    'patient.eleanor@ghostqueue.com',
    '+1 (555) 234-8910',
    'PATIENT',
    'ACTIVE'
) ON CONFLICT (id) DO UPDATE SET role = 'PATIENT', account_status = 'ACTIVE';

-- Patient 2 (Arthur Pendelton)
INSERT INTO auth.users (
    instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data, is_sso_user, is_anonymous, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000000',
    'p0000000-0000-0000-0000-000000000002',
    'authenticated', 'authenticated',
    'patient.arthur@ghostqueue.com',
    crypt('Patient@123', gen_salt('bf')),
    NOW(),
    '{"provider":"email","providers":["email"]}'::jsonb,
    '{"full_name":"Arthur Pendelton","role":"PATIENT"}'::jsonb,
    false, false, NOW(), NOW()
) ON CONFLICT (id) DO UPDATE SET encrypted_password = crypt('Patient@123', gen_salt('bf'));

INSERT INTO auth.identities (id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at)
VALUES (
    'p0000000-0000-0000-0000-000000000002',
    'p0000000-0000-0000-0000-000000000002',
    jsonb_build_object('sub', 'p0000000-0000-0000-0000-000000000002', 'email', 'patient.arthur@ghostqueue.com'),
    'email', 'patient.arthur@ghostqueue.com', NOW(), NOW(), NOW()
) ON CONFLICT (provider, provider_id) DO NOTHING;

INSERT INTO public.profiles (id, full_name, email, phone, role, account_status)
VALUES (
    'p0000000-0000-0000-0000-000000000002',
    'Arthur Pendelton',
    'patient.arthur@ghostqueue.com',
    '+1 (555) 876-1234',
    'PATIENT',
    'ACTIVE'
) ON CONFLICT (id) DO UPDATE SET role = 'PATIENT', account_status = 'ACTIVE';

-- 4. Doctors
INSERT INTO public.doctors (doctor_id, profile_id, doctor_identifier, full_name, department, specialization, phone, email, availability_status, account_status) VALUES
('DOC-101', 'd0000000-0000-0000-0000-000000000001', 'DOC-CARD-01', 'Dr. Robert Vance', 'Cardiology', 'Interventional Cardiology & Stents', '+1 (555) 019-1001', 'doctor.vance@ghostqueue.com', 'AVAILABLE', 'ACTIVE'),
('DOC-102', NULL, 'DOC-NEUR-01', 'Dr. David Kim', 'Neurology', 'Vestibular & Cognitive Neurology', '+1 (555) 019-1002', 'd.kim@fictional-hospital.org', 'AVAILABLE', 'ACTIVE'),
('DOC-103', 'd0000000-0000-0000-0000-000000000002', 'DOC-ORTH-01', 'Dr. James Wilson', 'Orthopedics', 'Total Joint Reconstruction', '+1 (555) 019-1003', 'doctor.wilson@ghostqueue.com', 'AVAILABLE', 'ACTIVE'),
('DOC-104', NULL, 'DOC-ONCO-01', 'Dr. Evelyn Reed', 'Oncology', 'Medical Oncology Surveillance', '+1 (555) 019-1004', 'e.reed@fictional-hospital.org', 'AVAILABLE', 'ACTIVE'),
('DOC-105', NULL, 'DOC-PEDI-01', 'Dr. Sarah Lin', 'Pediatrics', 'Pediatric Pulmonology & Allergy', '+1 (555) 019-1005', 's.lin@fictional-hospital.org', 'AVAILABLE', 'ACTIVE'),
('DOC-106', NULL, 'DOC-GENM-01', 'Dr. Thomas Wright', 'General Medicine', 'Preventative Internal Medicine', '+1 (555) 019-1006', 't.wright@fictional-hospital.org', 'AVAILABLE', 'ACTIVE'),
('DOC-107', 'd0000000-0000-0000-0000-000000000003', 'DOC-NEUR-02', 'Dr. Alexander Hayes', 'Neurology', 'Cerebrovascular & Stroke Care', '+1 (555) 019-1008', 'doctor.pending@ghostqueue.com', 'AVAILABLE', 'PENDING_APPROVAL')
ON CONFLICT (doctor_id) DO UPDATE SET full_name = EXCLUDED.full_name, profile_id = EXCLUDED.profile_id;

-- 5. Doctor Availability Schedules (Mon-Fri 08:30 to 17:00, 30 min slots)
INSERT INTO public.doctor_availability (doctor_id, day_of_week, start_time, end_time, slot_duration_minutes, is_active) VALUES
('DOC-101', 1, '08:30:00', '16:30:00', 30, true),
('DOC-101', 2, '08:30:00', '16:30:00', 30, true),
('DOC-101', 3, '08:30:00', '16:30:00', 30, true),
('DOC-101', 4, '08:30:00', '16:30:00', 30, true),
('DOC-101', 5, '08:30:00', '15:00:00', 30, true),
('DOC-102', 1, '09:00:00', '17:00:00', 30, true),
('DOC-102', 2, '09:00:00', '17:00:00', 30, true),
('DOC-102', 3, '09:00:00', '17:00:00', 30, true),
('DOC-103', 1, '08:00:00', '16:00:00', 45, true),
('DOC-103', 2, '08:00:00', '16:00:00', 45, true),
('DOC-103', 4, '08:00:00', '16:00:00', 45, true),
('DOC-103', 5, '08:00:00', '14:00:00', 45, true);

-- 6. Patients
INSERT INTO public.patients (patient_id, profile_id, patient_identifier, full_name, date_of_birth, gender, phone, email, address) VALUES
('PT-1001', 'p0000000-0000-0000-0000-000000000001', 'MED-PT-1001', 'Eleanor Vance', '1958-03-12', 'Female', '+1 (555) 234-8910', 'patient.eleanor@ghostqueue.com', '424 Elm St, Medical District'),
('PT-1002', 'p0000000-0000-0000-0000-000000000002', 'MED-PT-1002', 'Arthur Pendelton', '1972-07-25', 'Male', '+1 (555) 876-1234', 'patient.arthur@ghostqueue.com', '108 Oakwood Ave, Greenfield'),
('PT-1003', NULL, 'MED-PT-1003', 'Maria Gomez', '1984-11-09', 'Female', '+1 (555) 345-6789', 'maria.gomez@fictional-hospital.org', '712 Pine Crest Dr, Riverside'),
('PT-1004', NULL, 'MED-PT-1004', 'David Sterling', '1997-04-18', 'Male', '+1 (555) 987-6543', 'd.sterling@fictional-hospital.org', '89 Maple Ridge Rd, Lakeside'),
('PT-1005', NULL, 'MED-PT-1005', 'Amina Al-Mansoor', '1989-08-30', 'Female', '+1 (555) 456-7890', 'amina.m@fictional-hospital.org', '330 Cedar Lane, North Heights'),
('PT-1006', NULL, 'MED-PT-1006', 'Lucas Chen', '2017-02-14', 'Male', '+1 (555) 678-9012', 'chen.family@fictional-hospital.org', '554 Birchwood Ter, Fairview'),
('PT-1007', NULL, 'MED-PT-1007', 'Chloe Bennett', '1993-09-05', 'Female', '+1 (555) 789-0123', 'c.bennett@fictional-hospital.org', '614 Willow Pass, Clearfield'),
('PT-1008', NULL, 'MED-PT-1008', 'Samuel O Connor', '1955-01-20', 'Male', '+1 (555) 890-1234', 'sam.oconnor@fictional-hospital.org', '230 Sycamore Blvd, Brookside'),
('PT-1009', NULL, 'MED-PT-1009', 'Fatima Zahra', '1980-06-11', 'Female', '+1 (555) 901-2345', 'f.zahra@fictional-hospital.org', '478 Chestnut St, Highlands'),
('PT-1010', NULL, 'MED-PT-1010', 'Brandon Lee', '1968-12-03', 'Male', '+1 (555) 012-3456', 'brandon.lee@fictional-hospital.org', '124 Magnolia Ct, Glenwood'),
('PT-1011', NULL, 'MED-PT-1011', 'Priya Sharma', '1998-05-19', 'Female', '+1 (555) 123-4567', 'priya.s@fictional-hospital.org', '905 Ashwood Way, Sunnyvale'),
('PT-1012', NULL, 'MED-PT-1012', 'Jonathan Hayes', '1963-10-28', 'Male', '+1 (555) 234-5678', 'j.hayes@fictional-hospital.org', '332 Hickory Hollow, Oakridge'),
('PT-1013', NULL, 'MED-PT-1013', 'Victoria Ross', '1976-02-08', 'Female', '+1 (555) 345-6780', 'v.ross@fictional-hospital.org', '781 Beech Ave, Silver Spring'),
('PT-1014', NULL, 'MED-PT-1014', 'Daniel Kovacs', '1987-07-14', 'Male', '+1 (555) 456-7891', 'd.kovacs@fictional-hospital.org', '118 Poplar Grove, Woodbury'),
('PT-1015', NULL, 'MED-PT-1015', 'Hannah Schmidt', '1964-04-02', 'Female', '+1 (555) 567-8902', 'h.schmidt@fictional-hospital.org', '453 Walnut Cir, Mill Creek')
ON CONFLICT (patient_id) DO UPDATE SET full_name = EXCLUDED.full_name, profile_id = EXCLUDED.profile_id;

-- 7. Appointments
INSERT INTO public.appointments (
    appointment_id, patient_id, doctor_id, department, appointment_date, appointment_time,
    appointment_status, booking_date, cancellation_reason, clinical_notes
) VALUES
-- Today's Appointments
('APT-1001', 'PT-1001', 'DOC-101', 'Cardiology', CURRENT_DATE, '08:30:00', 'COMPLETED', CURRENT_DATE - INTERVAL '14 days', NULL, 'BP stable 128/82. Prescribed ACE inhibitor renewal.'),
('APT-1002', 'PT-1004', 'DOC-103', 'Orthopedics', CURRENT_DATE, '09:15:00', 'CANCELLED', CURRENT_DATE - INTERVAL '30 days', 'Patient called: transportation breakdown', NULL),
('APT-1003', 'PT-1003', 'DOC-102', 'Neurology', CURRENT_DATE, '10:00:00', 'CONFIRMED', CURRENT_DATE - INTERVAL '7 days', NULL, NULL),
('APT-1004', 'PT-1007', 'DOC-106', 'General Medicine', CURRENT_DATE, '11:00:00', 'CONFIRMED', CURRENT_DATE - INTERVAL '25 days', NULL, NULL),
('APT-1005', 'PT-1008', 'DOC-101', 'Cardiology', CURRENT_DATE, '11:45:00', 'CONFIRMED', CURRENT_DATE - INTERVAL '10 days', NULL, NULL),
('APT-1006', 'PT-1010', 'DOC-104', 'Oncology', CURRENT_DATE, '13:30:00', 'BOOKED', CURRENT_DATE - INTERVAL '35 days', NULL, NULL),
('APT-1007', 'PT-1006', 'DOC-105', 'Pediatrics', CURRENT_DATE, '14:15:00', 'CONFIRMED', CURRENT_DATE - INTERVAL '4 days', NULL, NULL),
('APT-1008', 'PT-1013', 'DOC-102', 'Neurology', CURRENT_DATE, '15:00:00', 'BOOKED', CURRENT_DATE - INTERVAL '28 days', NULL, NULL),
('APT-1009', 'PT-1014', 'DOC-106', 'General Medicine', CURRENT_DATE, '15:45:00', 'CONFIRMED', CURRENT_DATE - INTERVAL '5 days', NULL, NULL),
('APT-1010', 'PT-1011', 'DOC-106', 'General Medicine', CURRENT_DATE, '16:30:00', 'NO_SHOW', CURRENT_DATE - INTERVAL '18 days', 'Unannounced absence at appointment time', NULL),

-- Tomorrow
('APT-1011', 'PT-1002', 'DOC-101', CURRENT_DATE + INTERVAL '1 day', '09:00:00', 'CONFIRMED', CURRENT_DATE - INTERVAL '12 days', NULL, NULL),
('APT-1012', 'PT-1005', 'DOC-104', CURRENT_DATE + INTERVAL '1 day', '10:30:00', 'CONFIRMED', CURRENT_DATE - INTERVAL '8 days', NULL, NULL),
('APT-1013', 'PT-1012', 'DOC-103', CURRENT_DATE + INTERVAL '1 day', '11:15:00', 'CONFIRMED', CURRENT_DATE - INTERVAL '9 days', NULL, NULL),
('APT-1014', 'PT-1015', 'DOC-104', CURRENT_DATE + INTERVAL '1 day', '13:00:00', 'BOOKED', CURRENT_DATE - INTERVAL '15 days', NULL, NULL),
('APT-1015', 'PT-1009', 'DOC-102', CURRENT_DATE + INTERVAL '1 day', '14:00:00', 'CONFIRMED', CURRENT_DATE - INTERVAL '11 days', NULL, NULL),
('APT-1016', 'PT-1001', 'DOC-101', CURRENT_DATE + INTERVAL '1 day', '15:30:00', 'BOOKED', CURRENT_DATE - INTERVAL '14 days', NULL, NULL),

-- Day +2
('APT-1017', 'PT-1003', 'DOC-105', CURRENT_DATE + INTERVAL '2 days', '09:30:00', 'CONFIRMED', CURRENT_DATE - INTERVAL '6 days', NULL, NULL),
('APT-1018', 'PT-1002', 'DOC-106', CURRENT_DATE + INTERVAL '2 days', '10:15:00', 'CONFIRMED', CURRENT_DATE - INTERVAL '7 days', NULL, NULL),
('APT-1019', 'PT-1004', 'DOC-103', CURRENT_DATE + INTERVAL '2 days', '11:00:00', 'BOOKED', CURRENT_DATE - INTERVAL '21 days', NULL, NULL),
('APT-1020', 'PT-1010', 'DOC-104', CURRENT_DATE + INTERVAL '2 days', '14:30:00', 'CONFIRMED', CURRENT_DATE - INTERVAL '10 days', NULL, NULL),

-- Historical Appointments
('APT-1025', 'PT-1001', 'DOC-101', CURRENT_DATE - INTERVAL '28 days', '09:00:00', 'COMPLETED', CURRENT_DATE - INTERVAL '45 days', NULL, 'Echocardiogram normal. Continue monitoring.'),
('APT-1026', 'PT-1002', 'DOC-101', CURRENT_DATE - INTERVAL '21 days', '10:00:00', 'NO_SHOW', CURRENT_DATE - INTERVAL '35 days', 'Unreported absence', NULL),
('APT-1027', 'PT-1008', 'DOC-101', CURRENT_DATE - INTERVAL '14 days', '11:00:00', 'COMPLETED', CURRENT_DATE - INTERVAL '24 days', NULL, 'Stent check clear.'),
('APT-1028', 'PT-1012', 'DOC-103', CURRENT_DATE - INTERVAL '10 days', '14:00:00', 'COMPLETED', CURRENT_DATE - INTERVAL '20 days', NULL, 'Post-op knee flexion 115 degrees.'),
('APT-1029', 'PT-1004', 'DOC-103', CURRENT_DATE - INTERVAL '7 days', '15:00:00', 'CANCELLED', CURRENT_DATE - INTERVAL '14 days', 'Work conflict', NULL),
('APT-1030', 'PT-1014', 'DOC-106', CURRENT_DATE - INTERVAL '3 days', '09:30:00', 'COMPLETED', CURRENT_DATE - INTERVAL '10 days', NULL, 'Annual physical completed.')
ON CONFLICT (appointment_id) DO UPDATE SET appointment_status = EXCLUDED.appointment_status;

-- 8. Appointment Audit History
INSERT INTO public.appointment_history (appointment_id, previous_status, new_status, changed_at, remarks) VALUES
('APT-1001', 'BOOKED', 'CONFIRMED', NOW() - INTERVAL '2 days', 'Automated SMS confirmation received'),
('APT-1001', 'CONFIRMED', 'COMPLETED', NOW() - INTERVAL '2 hours', 'Consultation completed by Dr. Vance'),
('APT-1002', 'BOOKED', 'CANCELLED', NOW() - INTERVAL '3 hours', 'Patient called to report transport delay'),
('APT-1010', 'CONFIRMED', 'NO_SHOW', NOW() - INTERVAL '1 hour', 'Flagged no-show by reception');

-- 9. Machine Learning Predictions
INSERT INTO public.appointment_predictions (
    appointment_id, patient_id, probability, risk_level, model_version,
    prediction_method, contributing_factors, predicted_at, actual_outcome, evaluation_status
) VALUES
('APT-1003', 'PT-1003', 0.1850, 'LOW', '1.0.0-PROD', 'LOGISTIC_REGRESSION', 'Historical attendance 92%; Booking lead time 7 days (favorable); Prior no-shows: 0', NOW() - INTERVAL '1 day', NULL, 'PENDING'),
('APT-1004', 'PT-1007', 0.7240, 'HIGH', '1.0.0-PROD', 'LOGISTIC_REGRESSION', 'Booking lead time 25 days (high variance); Age 33 with 2 prior cancellations; Unconfirmed status', NOW() - INTERVAL '1 day', NULL, 'PENDING'),
('APT-1005', 'PT-1008', 0.2450, 'LOW', '1.0.0-PROD', 'LOGISTIC_REGRESSION', 'Senior age bracket 71; High attendance history 88%; Local residential proximity', NOW() - INTERVAL '1 day', NULL, 'PENDING'),
('APT-1006', 'PT-1010', 0.8120, 'HIGH', '1.0.0-PROD', 'LOGISTIC_REGRESSION', 'Extended lead time 35 days; Multiple unconfirmed reminders; Distance > 15 miles', NOW() - INTERVAL '1 day', NULL, 'PENDING'),
('APT-1007', 'PT-1006', 0.1420, 'LOW', '1.0.0-PROD', 'LOGISTIC_REGRESSION', 'Short lead time 4 days; Pediatric department parent active engagement', NOW() - INTERVAL '1 day', NULL, 'PENDING'),
('APT-1008', 'PT-1013', 0.6890, 'HIGH', '1.0.0-PROD', 'LOGISTIC_REGRESSION', 'Lead time 28 days; 1 prior unannounced no-show; Friday afternoon schedule slot', NOW() - INTERVAL '1 day', NULL, 'PENDING'),
('APT-1011', 'PT-1002', 0.5400, 'MEDIUM', '1.0.0-PROD', 'LOGISTIC_REGRESSION', '1 prior no-show recorded; Moderate lead time 12 days; Confirmed via outreach', NOW() - INTERVAL '6 hours', NULL, 'PENDING'),
('APT-1012', 'PT-1005', 0.2800, 'LOW', '1.0.0-PROD', 'LOGISTIC_REGRESSION', 'Consistent clinic attendance; Lead time 8 days', NOW() - INTERVAL '6 hours', NULL, 'PENDING'),
('APT-1014', 'PT-1015', 0.7450, 'HIGH', '1.0.0-PROD', 'LOGISTIC_REGRESSION', 'Unconfirmed outreach; Lead time 15 days; History of late cancellation', NOW() - INTERVAL '6 hours', NULL, 'PENDING');

-- 10. Waiting Queue
INSERT INTO public.waiting_queue (queue_id, patient_id, doctor_id, department, requested_date, preferred_time, priority_level, queue_status, notes) VALUES
('WQ-101', 'PT-1008', 'DOC-101', 'Cardiology', CURRENT_DATE, '08:00:00', 'URGENT', 'WAITING', 'Post-stent chest tightness check needed'),
('WQ-102', 'PT-1012', 'DOC-103', 'Orthopedics', CURRENT_DATE, '09:00:00', 'HIGH', 'WAITING', 'Acute knee swelling; flexible schedule'),
('WQ-103', 'PT-1003', 'DOC-102', 'Neurology', CURRENT_DATE, '13:00:00', 'STANDARD', 'WAITING', 'Routine vestibular therapy check'),
('WQ-104', 'PT-1005', 'DOC-104', 'Oncology', CURRENT_DATE, '11:00:00', 'HIGH', 'STANDBY', 'Immediate standby if afternoon slot opens'),
('WQ-105', 'PT-1006', 'DOC-105', 'Pediatrics', CURRENT_DATE + INTERVAL '1 day', '14:00:00', 'STANDARD', 'WAITING', 'Asthma inhaler technique review'),
('WQ-106', 'PT-1011', 'DOC-106', 'General Medicine', CURRENT_DATE, '10:00:00', 'URGENT', 'OFFERED', 'Offered vacated slot from APT-1002'),
('WQ-107', 'PT-1014', 'DOC-106', 'General Medicine', CURRENT_DATE + INTERVAL '1 day', '09:00:00', 'STANDARD', 'WAITING', 'Blood pressure follow-up'),
('WQ-108', 'PT-1002', 'DOC-101', 'Cardiology', CURRENT_DATE, '08:30:00', 'HIGH', 'WAITING', 'Cardiology medication review'),
('WQ-109', 'PT-1007', 'DOC-105', 'Pediatrics', CURRENT_DATE + INTERVAL '2 days', '09:30:00', 'STANDARD', 'WAITING', 'Ear infection check'),
('WQ-110', 'PT-1004', 'DOC-103', 'Orthopedics', CURRENT_DATE + INTERVAL '2 days', '11:00:00', 'HIGH', 'WAITING', 'Post-fracture x-ray consultation')
ON CONFLICT (queue_id) DO UPDATE SET queue_status = EXCLUDED.queue_status;

-- 11. Reminder Records
INSERT INTO public.reminder_records (
    appointment_id, patient_id, reminder_type, reminder_status,
    scheduled_time, sent_time, channel_response, risk_level_at_send
) VALUES
('APT-1001', 'PT-1001', 'SMS', 'CONFIRMED', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', 'Patient replied YES', 'LOW'),
('APT-1003', 'PT-1003', 'SMS', 'DELIVERED', NOW() - INTERVAL '24 hours', NOW() - INTERVAL '24 hours', 'Carrier delivered successfully', 'LOW'),
('APT-1004', 'PT-1007', 'EMAIL', 'SENT', NOW() - INTERVAL '20 hours', NOW() - INTERVAL '20 hours', 'Email opened by recipient', 'HIGH'),
('APT-1005', 'PT-1008', 'PHONE', 'CONFIRMED', NOW() - INTERVAL '18 hours', NOW() - INTERVAL '18 hours', 'Spoke with patient spouse; confirmed', 'LOW'),
('APT-1006', 'PT-1010', 'SMS', 'NO_RESPONSE', NOW() - INTERVAL '12 hours', NOW() - INTERVAL '12 hours', 'Delivered - no confirmation received', 'HIGH'),
('APT-1011', 'PT-1002', 'SMS', 'CONFIRMED', NOW() - INTERVAL '8 hours', NOW() - INTERVAL '8 hours', 'Patient replied YES', 'MEDIUM'),
('APT-1012', 'PT-1005', 'EMAIL', 'SENT', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours', 'Email delivered', 'LOW'),
('APT-1014', 'PT-1015', 'SMS', 'FAILED', NOW() - INTERVAL '4 hours', NOW() - INTERVAL '4 hours', 'Invalid carrier routing or handset unavail', 'HIGH');

-- 12. In-App Notifications
INSERT INTO public.notifications (user_id, title, message, type, is_read, created_at) VALUES
('a0000000-0000-0000-0000-000000000001', 'High Risk No-Show Alert', 'APT-1006 (Brandon Lee, Oncology) has an 81.2% predicted no-show probability. Standby patient WQ-104 is eligible for slot buffer.', 'ALERT', false, NOW() - INTERVAL '2 hours'),
('a0000000-0000-0000-0000-000000000001', 'Slot Vacancy Detected', 'APT-1002 was cancelled due to transport breakdown. Urgent standby WQ-106 has been automatically notified.', 'SYSTEM', false, NOW() - INTERVAL '3 hours'),
('d0000000-0000-0000-0000-000000000001', 'Daily Clinical Schedule', 'Good morning Dr. Vance. You have 3 appointments scheduled today and 1 slot tentatively flagged high-risk.', 'SCHEDULE', false, NOW() - INTERVAL '4 hours'),
('p0000000-0000-0000-0000-000000000001', 'Appointment Reminder', 'Your appointment with Dr. Robert Vance is scheduled for today at 08:30 AM in Cardiology Pavilion.', 'REMINDER', true, NOW() - INTERVAL '1 day'),
('p0000000-0000-0000-0000-000000000002', 'Upcoming Consultation', 'Your consultation with Dr. Robert Vance is scheduled for tomorrow at 09:00 AM.', 'REMINDER', false, NOW() - INTERVAL '6 hours');
