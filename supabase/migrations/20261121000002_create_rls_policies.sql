-- =============================================================================
-- GHOSTQUEUE HEALTHCARE INTELLIGENCE PLATFORM
-- Migration 02: Row Level Security (RLS) & Role-Based Access Control Policies
-- Version: 1.0.0
-- =============================================================================

-- Enable Row Level Security on all public tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.departments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.patients ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.doctors ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.doctor_availability ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.appointment_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.appointment_predictions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.waiting_queue ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reallocation_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reminder_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.application_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- Grant permissions for PostgREST Data API
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO authenticated;
GRANT SELECT ON public.departments, public.doctors, public.doctor_availability, public.application_settings TO anon;

-- -----------------------------------------------------------------------------
-- Helper Functions for Role Checking
-- -----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = (SELECT auth.uid()) AND role = 'ADMIN'
  );
$$;

CREATE OR REPLACE FUNCTION public.current_doctor_id()
RETURNS VARCHAR(36)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT doctor_id FROM public.doctors WHERE profile_id = (SELECT auth.uid()) LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION public.current_patient_id()
RETURNS VARCHAR(36)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT patient_id FROM public.patients WHERE profile_id = (SELECT auth.uid()) LIMIT 1;
$$;

-- -----------------------------------------------------------------------------
-- 1. Profiles Policies
-- -----------------------------------------------------------------------------
CREATE POLICY profiles_select_policy ON public.profiles
FOR SELECT TO authenticated
USING (
  (SELECT auth.uid()) = id 
  OR public.is_admin() 
  OR role = 'DOCTOR'
);

CREATE POLICY profiles_update_policy ON public.profiles
FOR UPDATE TO authenticated
USING ((SELECT auth.uid()) = id OR public.is_admin())
WITH CHECK ((SELECT auth.uid()) = id OR public.is_admin());

CREATE POLICY profiles_insert_policy ON public.profiles
FOR INSERT TO authenticated
WITH CHECK ((SELECT auth.uid()) = id OR public.is_admin());

-- -----------------------------------------------------------------------------
-- 2. Departments Policies
-- -----------------------------------------------------------------------------
CREATE POLICY departments_select_policy ON public.departments
FOR SELECT TO anon, authenticated
USING (true);

CREATE POLICY departments_admin_modify ON public.departments
FOR ALL TO authenticated
USING (public.is_admin())
WITH CHECK (public.is_admin());

-- -----------------------------------------------------------------------------
-- 3. Doctors Policies
-- -----------------------------------------------------------------------------
CREATE POLICY doctors_select_policy ON public.doctors
FOR SELECT TO anon, authenticated
USING (true);

CREATE POLICY doctors_update_policy ON public.doctors
FOR UPDATE TO authenticated
USING (profile_id = (SELECT auth.uid()) OR public.is_admin())
WITH CHECK (profile_id = (SELECT auth.uid()) OR public.is_admin());

CREATE POLICY doctors_admin_insert ON public.doctors
FOR INSERT TO authenticated
WITH CHECK (public.is_admin());

CREATE POLICY doctors_admin_delete ON public.doctors
FOR DELETE TO authenticated
USING (public.is_admin());

-- -----------------------------------------------------------------------------
-- 4. Doctor Availability Policies
-- -----------------------------------------------------------------------------
CREATE POLICY doctor_avail_select ON public.doctor_availability
FOR SELECT TO anon, authenticated
USING (true);

CREATE POLICY doctor_avail_modify ON public.doctor_availability
FOR ALL TO authenticated
USING (doctor_id = public.current_doctor_id() OR public.is_admin())
WITH CHECK (doctor_id = public.current_doctor_id() OR public.is_admin());

-- -----------------------------------------------------------------------------
-- 5. Patients Policies
-- -----------------------------------------------------------------------------
CREATE POLICY patients_select_policy ON public.patients
FOR SELECT TO authenticated
USING (
  profile_id = (SELECT auth.uid())
  OR public.is_admin()
  OR patient_id IN (
    SELECT a.patient_id FROM public.appointments a 
    WHERE a.doctor_id = public.current_doctor_id()
  )
);

CREATE POLICY patients_insert_policy ON public.patients
FOR INSERT TO authenticated
WITH CHECK (profile_id = (SELECT auth.uid()) OR public.is_admin());

CREATE POLICY patients_update_policy ON public.patients
FOR UPDATE TO authenticated
USING (profile_id = (SELECT auth.uid()) OR public.is_admin())
WITH CHECK (profile_id = (SELECT auth.uid()) OR public.is_admin());

-- -----------------------------------------------------------------------------
-- 6. Appointments Policies
-- -----------------------------------------------------------------------------
CREATE POLICY appointments_select_policy ON public.appointments
FOR SELECT TO authenticated
USING (
  patient_id = public.current_patient_id()
  OR doctor_id = public.current_doctor_id()
  OR public.is_admin()
);

CREATE POLICY appointments_insert_policy ON public.appointments
FOR INSERT TO authenticated
WITH CHECK (
  patient_id = public.current_patient_id()
  OR public.is_admin()
);

CREATE POLICY appointments_update_policy ON public.appointments
FOR UPDATE TO authenticated
USING (
  patient_id = public.current_patient_id()
  OR doctor_id = public.current_doctor_id()
  OR public.is_admin()
)
WITH CHECK (
  patient_id = public.current_patient_id()
  OR doctor_id = public.current_doctor_id()
  OR public.is_admin()
);

-- -----------------------------------------------------------------------------
-- 7. Appointment History Policies
-- -----------------------------------------------------------------------------
CREATE POLICY appointment_history_select ON public.appointment_history
FOR SELECT TO authenticated
USING (
  appointment_id IN (
    SELECT appointment_id FROM public.appointments 
    WHERE patient_id = public.current_patient_id() 
       OR doctor_id = public.current_doctor_id() 
       OR public.is_admin()
  )
);

CREATE POLICY appointment_history_insert ON public.appointment_history
FOR INSERT TO authenticated
WITH CHECK (true);

-- -----------------------------------------------------------------------------
-- 8. Predictions Policies (Restricted from Patients)
-- -----------------------------------------------------------------------------
CREATE POLICY predictions_select_policy ON public.appointment_predictions
FOR SELECT TO authenticated
USING (
  public.is_admin()
  OR appointment_id IN (
    SELECT appointment_id FROM public.appointments WHERE doctor_id = public.current_doctor_id()
  )
);

CREATE POLICY predictions_admin_write ON public.appointment_predictions
FOR ALL TO authenticated
USING (public.is_admin())
WITH CHECK (public.is_admin());

-- -----------------------------------------------------------------------------
-- 9. Waiting Queue Policies
-- -----------------------------------------------------------------------------
CREATE POLICY waiting_queue_select ON public.waiting_queue
FOR SELECT TO authenticated
USING (
  patient_id = public.current_patient_id()
  OR public.is_admin()
);

CREATE POLICY waiting_queue_insert ON public.waiting_queue
FOR INSERT TO authenticated
WITH CHECK (
  patient_id = public.current_patient_id()
  OR public.is_admin()
);

CREATE POLICY waiting_queue_admin_modify ON public.waiting_queue
FOR UPDATE TO authenticated
USING (public.is_admin())
WITH CHECK (public.is_admin());

CREATE POLICY waiting_queue_admin_delete ON public.waiting_queue
FOR DELETE TO authenticated
USING (public.is_admin());

-- -----------------------------------------------------------------------------
-- 10. Reallocation History Policies
-- -----------------------------------------------------------------------------
CREATE POLICY reallocation_history_select ON public.reallocation_history
FOR SELECT TO authenticated
USING (public.is_admin());

CREATE POLICY reallocation_history_write ON public.reallocation_history
FOR ALL TO authenticated
USING (public.is_admin())
WITH CHECK (public.is_admin());

-- -----------------------------------------------------------------------------
-- 11. Reminder Outreach Policies
-- -----------------------------------------------------------------------------
CREATE POLICY reminders_select ON public.reminder_records
FOR SELECT TO authenticated
USING (
  patient_id = public.current_patient_id()
  OR public.is_admin()
);

CREATE POLICY reminders_admin_write ON public.reminder_records
FOR ALL TO authenticated
USING (public.is_admin())
WITH CHECK (public.is_admin());

-- -----------------------------------------------------------------------------
-- 12. In-App Notifications Policies
-- -----------------------------------------------------------------------------
CREATE POLICY notifications_select ON public.notifications
FOR SELECT TO authenticated
USING (user_id = (SELECT auth.uid()) OR public.is_admin());

CREATE POLICY notifications_insert ON public.notifications
FOR INSERT TO authenticated
WITH CHECK (true);

CREATE POLICY notifications_update ON public.notifications
FOR UPDATE TO authenticated
USING (user_id = (SELECT auth.uid()) OR public.is_admin())
WITH CHECK (user_id = (SELECT auth.uid()) OR public.is_admin());

-- -----------------------------------------------------------------------------
-- 13. Application Settings Policies
-- -----------------------------------------------------------------------------
CREATE POLICY settings_select ON public.application_settings
FOR SELECT TO anon, authenticated
USING (true);

CREATE POLICY settings_admin_write ON public.application_settings
FOR ALL TO authenticated
USING (public.is_admin())
WITH CHECK (public.is_admin());

-- -----------------------------------------------------------------------------
-- 14. Audit Logs Policies
-- -----------------------------------------------------------------------------
CREATE POLICY audit_logs_select ON public.audit_logs
FOR SELECT TO authenticated
USING (public.is_admin());

CREATE POLICY audit_logs_insert ON public.audit_logs
FOR INSERT TO authenticated
WITH CHECK (true);
