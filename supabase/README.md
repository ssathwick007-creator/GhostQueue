# GhostQueue Supabase Cloud Architecture & Migrations

This directory contains the database schema, Row Level Security (RLS) policies, and seed data for **GhostQueue**.

## 1. Cloud Project Configuration
- **Supabase Project ID:** `zvlmtheyizmpzcktfsva`
- **Region:** `ap-south-1`
- **API Endpoint:** `https://zvlmtheyizmpzcktfsva.supabase.co`
- **Auth Endpoint:** `https://zvlmtheyizmpzcktfsva.supabase.co/auth/v1`
- **Data (REST) Endpoint:** `https://zvlmtheyizmpzcktfsva.supabase.co/rest/v1`
- **Client Key:** Publishable `anon` key (Zero service-role keys in JavaFX client)

## 2. Directory Structure
```
supabase/
├── migrations/
│   ├── 20261121000001_create_ghostqueue_schema.sql  # 14 Core tables, constraints & indexes
│   └── 20261121000002_create_rls_policies.sql        # RLS policies & security functions
├── seed.sql                                         # Demo users, doctors, patients, appointments
└── README.md
```

## 3. Seed User Accounts & Roles
The database includes pre-configured demo accounts for all three portals:

| Portal | Email | Password | Role | Associated Entity | Status |
|---|---|---|---|---|---|
| **Admin Portal** | `admin@ghostqueue.com` | `Admin@123` | `ADMIN` | Chief Medical Ops (Dr. Sarah Chen) | `ACTIVE` |
| **Doctor Portal** | `doctor.vance@ghostqueue.com` | `Doctor@123` | `DOCTOR` | Dr. Robert Vance (`DOC-101`, Cardiology) | `ACTIVE` |
| **Doctor Portal** | `doctor.wilson@ghostqueue.com` | `Doctor@123` | `DOCTOR` | Dr. James Wilson (`DOC-103`, Orthopedics) | `ACTIVE` |
| **Doctor Onboarding** | `doctor.pending@ghostqueue.com` | `Doctor@123` | `DOCTOR` | Dr. Alexander Hayes (`DOC-107`, Neurology) | `PENDING_APPROVAL` |
| **Patient Portal** | `patient.eleanor@ghostqueue.com` | `Patient@123` | `PATIENT` | Eleanor Vance (`PT-1001`) | `ACTIVE` |
| **Patient Portal** | `patient.arthur@ghostqueue.com` | `Patient@123` | `PATIENT` | Arthur Pendelton (`PT-1002`) | `ACTIVE` |

## 4. Row Level Security (RLS) Matrix
- **`profiles`:** Authenticated users may read their own profile, doctors, or all profiles if Admin. Self-role elevation is prohibited.
- **`patients`:** Scoped strictly to the authenticated patient (`profile_id = auth.uid()`), doctors with active consultations for that patient, or hospital administrators.
- **`doctors`:** Public directory for active doctors. Updates restricted to the doctor's own profile or Admin.
- **`appointments`:** Patients see and cancel only their appointments; doctors view and update clinical statuses (`ARRIVED`, `COMPLETED`, `NO_SHOW`, notes) for their schedule; Admins retain full operational oversight.
- **`appointment_predictions`:** Restricted to Admins and assigned Doctors. Patients cannot view internal no-show risk scores.
- **`notifications`:** Strictly scoped to `user_id = auth.uid()`.
