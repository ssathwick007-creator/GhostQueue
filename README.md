# GhostQueue: Intelligent Predictive No-Show Detection & Dynamic Appointment Reallocation System

> **Capstone Project — Phase 1 Application Foundation**  
> Developed as a modern, enterprise-grade hospital appointment intelligence platform.

---

## 1. Executive Summary & Product Vision

**GhostQueue** is a specialized hospital operations and appointment intelligence desktop application designed for clinical operations coordinators, hospital administrators, and clinic triage teams.

### Core Objectives:
- **Active Appointment Tracking:** Manage patient appointment schedules across multiple clinical specialties.
- **Predictive No-Show Assessment:** Identify at-risk appointment slots using simulated attendance heuristics and contributing clinical risk factors.
- **Intelligent Waiting Queue:** Organize waiting/standby patients by clinical urgency, requested specialty, and availability.
- **Dynamic Slot Reallocation:** Automatically correlate cancelled and high-risk appointment openings with priority waiting candidates.
- **Capacity Recovery:** Minimize idle physician time and improve patient access through explicit human-in-the-loop reallocation approvals.

---

## 2. Technology Stack

- **Platform:** Java 21 (LTS)
- **GUI Framework:** JavaFX 21 (FXML + CSS + Modular Architecture)
- **Build System:** Apache Maven 3.10+
- **Architecture:** Layered Enterprise Architecture (MVC + Service + Repository Pattern)
- **Database Readiness:** JDBC abstraction layer with MySQL 9.1 connector dependency included

---

## 3. Design System & Palette

The user interface follows a clinical enterprise design language (reminiscent of Epic, Cerner, and modern clinical operations dashboards), avoiding toys, neon colors, glassmorphism, or generic AI patterns.

| Token | Hex Code | Purpose |
|---|---|---|
| **Main Background** | `#F5F6F4` | App canvas & work surface |
| **Sidebar Background** | `#172A3A` | Navigation bar & brand anchor |
| **Primary Accent** | `#168C83` | Brand buttons, active states & primary actions |
| **Primary Text** | `#202B35` | High-contrast typography & headings |
| **Secondary Text** | `#77838D` | Subtitles, labels, and metadata |
| **Card Background** | `#FFFFFF` | Surfaces, data cards, and tables |
| **Borders** | `#E4E9E8` | Crisp structural separation |
| **Soft Blue / Tint** | `#E8F0F5` | Info banners, tags, and subtle highlights |
| **Success / Low Risk** | `#438B70` | Confirmed status, Low Risk attendance badge |
| **Warning / Medium Risk** | `#D99A42` | Pending status, Medium Risk badge |
| **Danger / High Risk** | `#C95F5F` | Urgent triage, High Risk alert, Cancellations |

---

## 4. Application Architecture & Project Structure

```
GhostQueue/
├── pom.xml                               # Maven project descriptor & build configuration
├── run.bat                               # Windows 1-click batch launcher
├── run.ps1                               # PowerShell launcher script
├── README.md                             # Documentation & user guide
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── module-info.java          # Java 21 module definition
    │   │   └── com/ghostqueue/
    │   │       ├── MainApp.java          # JavaFX Application entry point
    │   │       ├── Launcher.java         # Non-modular bootstrap fallback
    │   │       ├── component/
    │   │       │   └── IconHelper.java   # Scalable vector SVG icon generator
    │   │       ├── controller/
    │   │       │   ├── MainLayoutController.java         # Sidebar nav & app shell
    │   │       │   ├── DashboardController.java          # Screen A: Operations Overview
    │   │       │   ├── AppointmentsController.java       # Screen B: Appointment Management
    │   │       │   ├── AddAppointmentDialogController.java # Modal dialog for scheduling
    │   │       │   ├── PredictionController.java         # Screen C: No-Show Prediction
    │   │       │   ├── WaitingQueueController.java       # Screen D: Waiting Queue
    │   │       │   ├── AddQueueDialogController.java     # Modal dialog for queue registration
    │   │       │   ├── ReallocationController.java       # Screen E: Smart Reallocation
    │   │       │   ├── PatientRecordsController.java     # Screen F: Patient Index & History
    │   │       │   ├── AnalyticsController.java          # Screen G: Attendance Analytics
    │   │       │   └── SettingsController.java           # Screen H: System Configuration
    │   │       ├── model/
    │   │       │   ├── Appointment.java
    │   │       │   ├── AppointmentStatus.java
    │   │       │   ├── Department.java
    │   │       │   ├── Doctor.java
    │   │       │   ├── Patient.java
    │   │       │   ├── PredictionRecord.java
    │   │       │   ├── QueueEntry.java
    │   │       │   ├── QueuePriority.java
    │   │       │   ├── QueueStatus.java
    │   │       │   ├── ReallocationSuggestion.java
    │   │       │   └── RiskLevel.java
    │   │       ├── prediction/
    │   │       │   ├── PredictionModel.java
    │   │       │   └── HeuristicRiskScorer.java
    │   │       ├── repository/
    │   │       │   ├── AppointmentRepository.java
    │   │       │   ├── PatientRepository.java
    │   │       │   ├── QueueRepository.java
    │   │       │   └── mock/
    │   │       │       ├── MockAppointmentRepository.java
    │   │       │       ├── MockPatientRepository.java
    │   │       │       └── MockQueueRepository.java
    │   │       ├── scheduling/
    │   │       │   └── ReallocationEngine.java
    │   │       ├── service/
    │   │       │   ├── AnalyticsService.java
    │   │       │   ├── AppointmentService.java
    │   │       │   ├── PatientService.java
    │   │       │   ├── PredictionService.java
    │   │       │   ├── QueueService.java
    │   │       │   ├── ReallocationService.java
    │   │       │   └── ServiceRegistry.java
    │   │       └── util/
    │   │           ├── DateTimeUtils.java
    │   │           ├── NotificationManager.java
    │   │           └── UiHelper.java
    │   └── resources/
    │       ├── css/
    │       │   └── theme.css              # Complete design system CSS rules
    │       └── fxml/
    │           ├── main_layout.fxml       # Shell with persistent sidebar & top nav
    │           ├── dashboard.fxml         # Screen A
    │           ├── appointments.fxml      # Screen B
    │           ├── prediction.fxml        # Screen C
    │           ├── waiting_queue.fxml     # Screen D
    │           ├── reallocation.fxml      # Screen E
    │           ├── patient_records.fxml   # Screen F
    │           ├── analytics.fxml         # Screen G
    │           ├── settings.fxml          # Screen H
    │           └── dialogs/
    │               ├── add_appointment_dialog.fxml
    │               └── add_queue_dialog.fxml
    └── test/
        └── java/com/ghostqueue/
            └── AppIntegrationTest.java    # Automated verification test suite
```

---

## 5. Overview of Navigable Screens

### A. Operations Overview (Dashboard)
- **Real-Time Stat Cards:** Total Appointments (20), Confirmed (14), Predicted High-Risk (4), Completed (1), and Cancellations (1).
- **Interactive Charts:**
  - Department Load BarChart showing scheduled consultation distribution across specialties.
  - No-show Risk Distribution PieChart (Low Risk, Medium Risk, High Risk).
- **Today's Operational Schedule Table:** With live status badges, calculated risk score badges, and quick actions ("Confirm", "View Details").
- **Quick Actions:** "⚡ Evaluate Risks", "⇄ View Reallocations", "↻ Refresh".

### B. Appointments
- Comprehensive appointment directory with search by Patient Name, MRN ID, and Physician.
- Live dropdown filters for Department, Status, and Date.
- Dynamic count badge showing matching appointments.
- **Interactive Table:** Inline status transition ("Status"), safe cancellation ("Cancel" which prompts confirmation and opens slot for reallocation), and modal viewing.
- **"+ Add Appointment" Modal:** Form dialog with clinical department selection, time slots, physician, and real-time validation.

### C. No-show Prediction
- Dedicated risk dashboard with simulated baseline scoring.
- Prominent disclaimer banner clarifying simulated evaluation for capstone testing.
- **KPI Metrics:** High-Risk Count, Average Risk Score (28.4%), Simulated ROC-AUC Accuracy (88.6%), Projected Prevented No-Shows.
- **Prediction Roster Table:** Showing No-Show probability %, Risk Level pill, primary contributing factors (e.g. "Prior history: 4 missed visits", "Lead time > 30 days"), and "Follow-up" action dispatch.

### D. Waiting Queue
- Standby and unallocated patient pool.
- Priority levels: `Urgent` (Red pill), `High` (Amber pill), `Standard` (Teal pill).
- Tracking of requested specialty, preferred physician, time window, waiting duration, and status (`Waiting`, `Slot Offered`, `On Standby`, `Reallocated`).
- **"+ Add to Waiting Queue" Modal:** Register triage patients directly into queue.
- **Action Triggers:** "Offer Slot" and "Remove" with confirmation dialogs.

### E. Smart Reallocation
- **Safety-First Principle:** No automated cancellation or force-reassignment; requires operator consent.
- **Recommendation Pairings Table:** Correlates cancelled and high-risk slots with top-matching waiting patients based on department, urgency, and standby proximity.
- **Match Scoring:** Calculates match compatibility % and human-readable clinical rationale.
- **Decision Controls:** "Approve" (prompts confirmation and marks candidate as Reallocated) and "Decline / Hold".
- **Vacant Slots Audit Table:** Live list of open slots available for backfill.

### F. Patient Records
- Master Patient Index with search filtering.
- Table columns for MRN ID, Full Name, Age/Sex, Contact, Primary Physician, Attendance Count, and Adherence Rate %.
- **Patient Detail Panel:** Automatically updates upon row selection to display demographic summary, physician notes, and chronological past appointment history with status badges.

### G. Attendance Analytics
- Hospital KPI summary: Overall Attendance Rate (88.4%), No-Show Rate (11.6%), Slot Reclaim Efficiency (74.2%), and Average Clearance Time (18.5 hrs).
- **Charts:**
  - 6-month historical attendance vs unannounced no-show LineChart.
  - Specialty slot utilization BarChart.
- **Clinical Behavioral Insights:** Lead-time sensitivity analysis, time-of-day attrition observations, and capacity recovery impact.

### H. System Configuration (Settings)
- Facility & campus parameters.
- Predictive model risk cutoff tuning sliders (High-Risk default: 70%, Medium-Risk default: 35%).
- Safety policies: Strict operator confirmation requirement, urgent triage prioritization, and SMS alerts.
- Interface controls: "Save Configuration" with persistent memory and "Reset to Defaults".

---

## 6. How to Run the Application

### Option A: Using the 1-Click Launch Scripts (Recommended)
Double-click `run.bat` or in PowerShell run:
```powershell
.\run.ps1
```

### Option B: Using Maven CLI
```powershell
& "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd" compile javafx:run
```
*(Or simply `mvn compile javafx:run` if Maven is in your global PATH)*

### Option C: Using Standalone Java Module Launch
```powershell
java --module-path "target\lib;target\ghostqueue-app-1.0.0-SNAPSHOT.jar" --module com.ghostqueue/com.ghostqueue.MainApp
```

---

## 7. Automated Testing & Verification

Run the integration test suite:
```powershell
& "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd" test
```

Results: **7 of 7 integration tests passing** covering FXML existence, model integrity, risk scoring heuristics, queue operations, slot reallocation pairing, and analytics aggregation.
#   P r o m e d y x  
 