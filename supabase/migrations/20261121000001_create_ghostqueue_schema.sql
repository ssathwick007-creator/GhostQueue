-- =============================================================================
-- GHOSTQUEUE HEALTHCARE INTELLIGENCE PLATFORM
-- Migration 01: Core Relational Database Schema
-- Version: 1.0.0
-- =============================================================================

-- 1. Profiles Table (Coupled to Supabase Auth Users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    phone TEXT,
    role TEXT NOT NULL CHECK (role IN ('PATIENT', 'DOCTOR', 'ADMIN')),
    account_status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (account_status IN ('ACTIVE', 'PENDING_APPROVAL', 'SUSPENDED')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Departments Table
CREATE TABLE IF NOT EXISTS public.departments (
    department_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Patients Table
CREATE TABLE IF NOT EXISTS public.patients (
    patient_id VARCHAR(36) PRIMARY KEY,
    profile_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    patient_identifier VARCHAR(50),
    full_name TEXT NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(20),
    phone VARCHAR(30),
    email VARCHAR(100),
    address TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Doctors Table
CREATE TABLE IF NOT EXISTS public.doctors (
    doctor_id VARCHAR(36) PRIMARY KEY,
    profile_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    doctor_identifier VARCHAR(50),
    full_name TEXT NOT NULL,
    department VARCHAR(50) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(100),
    availability_status VARCHAR(30) DEFAULT 'AVAILABLE',
    account_status VARCHAR(30) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Doctor Availability Schedules Table
CREATE TABLE IF NOT EXISTS public.doctor_availability (
    id BIGSERIAL PRIMARY KEY,
    doctor_id VARCHAR(36) REFERENCES public.doctors(doctor_id) ON DELETE CASCADE,
    day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INT DEFAULT 30,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. Appointments Table
CREATE TABLE IF NOT EXISTS public.appointments (
    appointment_id VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) REFERENCES public.patients(patient_id) ON DELETE CASCADE,
    doctor_id VARCHAR(36) REFERENCES public.doctors(doctor_id) ON DELETE CASCADE,
    department VARCHAR(50) NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    end_time TIME,
    appointment_status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    booking_date DATE DEFAULT CURRENT_DATE,
    cancellation_reason TEXT,
    clinical_notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. Appointment Audit History Table
CREATE TABLE IF NOT EXISTS public.appointment_history (
    history_id BIGSERIAL PRIMARY KEY,
    appointment_id VARCHAR(36) REFERENCES public.appointments(appointment_id) ON DELETE CASCADE,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_at TIMESTAMPTZ DEFAULT NOW(),
    remarks TEXT
);

-- 8. Appointment Predictions Table (Machine Learning Engine)
CREATE TABLE IF NOT EXISTS public.appointment_predictions (
    prediction_id BIGSERIAL PRIMARY KEY,
    appointment_id VARCHAR(36) REFERENCES public.appointments(appointment_id) ON DELETE CASCADE,
    patient_id VARCHAR(36),
    probability NUMERIC(5, 4) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    model_version VARCHAR(30) NOT NULL,
    prediction_method VARCHAR(50) DEFAULT 'LOGISTIC_REGRESSION',
    contributing_factors TEXT,
    predicted_at TIMESTAMPTZ DEFAULT NOW(),
    actual_outcome VARCHAR(30),
    evaluation_status VARCHAR(30) DEFAULT 'PENDING'
);

-- 9. Waiting Queue Table (Triage Engine)
CREATE TABLE IF NOT EXISTS public.waiting_queue (
    queue_id VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) REFERENCES public.patients(patient_id) ON DELETE CASCADE,
    doctor_id VARCHAR(36) REFERENCES public.doctors(doctor_id) ON DELETE SET NULL,
    department VARCHAR(50) NOT NULL,
    requested_date DATE NOT NULL,
    preferred_time TIME,
    priority_level VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    queue_status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 10. Reallocation History Table (Smart Dynamic Reallocation)
CREATE TABLE IF NOT EXISTS public.reallocation_history (
    history_id BIGSERIAL PRIMARY KEY,
    original_appointment_id VARCHAR(36) REFERENCES public.appointments(appointment_id) ON DELETE CASCADE,
    candidate_patient_id VARCHAR(36) REFERENCES public.patients(patient_id) ON DELETE CASCADE,
    doctor_id VARCHAR(36) REFERENCES public.doctors(doctor_id) ON DELETE CASCADE,
    target_date DATE NOT NULL,
    target_time TIME NOT NULL,
    compatibility_score NUMERIC(5, 2) NOT NULL,
    recommendation_reason TEXT,
    decision VARCHAR(30) NOT NULL,
    decided_by VARCHAR(100),
    decided_at TIMESTAMPTZ DEFAULT NOW()
);

-- 11. Reminder Outreach Records Table
CREATE TABLE IF NOT EXISTS public.reminder_records (
    reminder_id BIGSERIAL PRIMARY KEY,
    appointment_id VARCHAR(36) REFERENCES public.appointments(appointment_id) ON DELETE CASCADE,
    patient_id VARCHAR(36) REFERENCES public.patients(patient_id) ON DELETE CASCADE,
    reminder_type VARCHAR(20) NOT NULL,
    reminder_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    scheduled_time TIMESTAMPTZ NOT NULL,
    sent_time TIMESTAMPTZ,
    channel_response TEXT,
    risk_level_at_send VARCHAR(20),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 12. In-App Notifications Table
CREATE TABLE IF NOT EXISTS public.notifications (
    notification_id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 13. Application Settings Table
CREATE TABLE IF NOT EXISTS public.application_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 14. Audit Logs Table
CREATE TABLE IF NOT EXISTS public.audit_logs (
    log_id BIGSERIAL PRIMARY KEY,
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50),
    details TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for Query Performance and Foreign Keys
CREATE INDEX IF NOT EXISTS idx_patients_profile_id ON public.patients(profile_id);
CREATE INDEX IF NOT EXISTS idx_doctors_profile_id ON public.doctors(profile_id);
CREATE INDEX IF NOT EXISTS idx_appointments_patient_id ON public.appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_id ON public.appointments(doctor_id);
CREATE INDEX IF NOT EXISTS idx_appointments_date ON public.appointments(appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON public.appointments(appointment_status);
CREATE INDEX IF NOT EXISTS idx_queue_patient_id ON public.waiting_queue(patient_id);
CREATE INDEX IF NOT EXISTS idx_queue_status ON public.waiting_queue(queue_status);
CREATE INDEX IF NOT EXISTS idx_predictions_appt ON public.appointment_predictions(appointment_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON public.notifications(user_id);
