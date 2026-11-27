# GhostQueue: Intelligent Predictive No-Show Detection & Dynamic Appointment Reallocation System

[![Java Version](https://img.shields.io/badge/Java-21%20LTS-007396?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-FF8800?style=flat&logo=javafx&logoColor=white)](https://openjfx.io/)
[![MongoDB Atlas](https://img.shields.io/badge/MongoDB%20Atlas-Cloud%20Database-47A248?style=flat&logo=mongodb&logoColor=white)](https://www.mongodb.com/atlas)
[![Build Tool](https://img.shields.io/badge/Maven-3.9%2B-C71A36?style=flat&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Academic%20Capstone-168C83?style=flat)](#license)

> **GhostQueue** is an enterprise-grade hospital appointment management and clinical intelligence platform. It combines pure Java predictive machine learning with human-in-the-loop dynamic slot backfilling to identify potential appointment no-shows, maintain an intelligent waiting queue, recommend high-compatibility slot reallocations, and reclaim wasted physician capacity. Powered by **MongoDB Atlas Cloud Database** and featuring both a modern **Single-Page Cloud Web Application** and a **Desktop JavaFX Client**.

---

## 1. Executive Summary & Clinical Problem Statement

Hospital outpatient departments routinely suffer from high **patient no-show rates (averaging 12% to 25% worldwide)**. Unannounced patient absences cause severe clinical and operational consequences:
1. **Physician Idleness & Revenue Loss:** Costly clinic rooms, specialized equipment, and clinician schedules stand vacant.
2. **Delayed Access for Urgent Patients:** Critically ill patients face long waiting lists (often weeks or months) while booked slots go unused.
3. **Flawed Industry Workarounds:** Conventional clinics resort to indiscriminate overbooking (which causes severe clinic delays, physician burnout, and crowded waiting rooms) or punitive cancellation fees (which disproportionately harm vulnerable patient populations).

### The GhostQueue Solution
GhostQueue transforms clinical scheduling from a passive calendar into an **autonomous predictive intelligence system**:
- **Continuous Machine Learning:** Analyzes patient demographic, clinical, and historical attendance patterns using an in-house pure Java Logistic Regression engine.
- **Transparent Risk Triage:** Calculates individual no-show probabilities (bounded between 2% and 98%) with explainable clinical contributing factors (XAI).
- **Intelligent Standby Pool:** Maintains a real-time waiting queue scored by clinical urgency and waiting duration.
- **Dynamic Slot Reallocation:** When an appointment is cancelled or identified as high-risk, a multi-factor compatibility engine scores and ranks waiting candidates for immediate slot offers.
- **Strict Human-in-the-Loop Governance:** Predictions *never* cancel patient appointments or deny care. Reallocations require explicit administrator review and approval.

---

## 2. Core Modules & Key Features

| Module | Core Functionality | Phase |
| :--- | :--- | :--- |
| **Operations Overview** | Real-time clinical KPIs, daily schedule timeline, specialty breakdown, and system alerts. | Phase 1 |
| **Appointment Management** | Complete CRUD scheduling, multi-criteria filtering, doctor/department search, status cycling, and history tracking. | Phase 2 |
| **Prediction Engine** | Pure Java Logistic Regression inference, explainable factors (XAI), confusion matrix evaluation, and model retraining. | Phase 3 |
| **Intelligent Waiting Queue** | Standby patient registry, transparent urgency scoring (0–100), time-decay bonus, and triage notes. | Phase 4 |
| **Dynamic Reallocation Engine** | Multi-factor compatibility scoring (specialty, doctor, date, time, urgency), candidate ranking, and atomic audit logging. | Phase 5 |
| **Reminders & Outreach** | Automated high-risk outreach generation, multi-channel logging (SMS, Voice, Email), and response recording. | Phase 6 |
| **Analytics & Reporting** | Attendance/no-show trends, department slot utilization, doctor workloads, peak time windows, and real CSV exports. | Phase 7 |
| **System Settings** | Persisted facility metadata in MySQL `system_settings`, operating hours, and dynamic model risk threshold sliders. | Phase 8 |

---

## 3. Technology Stack & Architectural Patterns

- **Core Runtime:** Java 21 (LTS) — utilizing Records, Pattern Matching for `switch`, Sealed Types, Virtual Threads, and Modular Architecture (`module-info.java`).
- **Primary Cloud Database:** **MongoDB Atlas Cloud Cluster** (`cluster0.d5szoha.mongodb.net`, database `ghostqueue_db`) with official MongoDB Java Driver 5.3.1, automated schema auto-seeding, and connection fallback.
- **Web Interface:** Modern Single-Page Cloud Web Application (SPA) powered by pure HTML5, CSS3, modern Vanilla ES6+, and responsive data visualizations.
- **Desktop Interface:** JavaFX 21 (FXML + CSS) styled with custom healthcare design tokens (`#172A3A` slate sidebar, `#168C83` clinical teal, luminous cyan accents, `#F5F6F4` neutral canvas, `#FFFFFF` cards).
- **Embedded Web Server:** High-performance, zero-external-framework HTTP REST server (`GhostQueueWebServer`) running on Java 21's Virtual Threads per-task executor (`jdk.httpserver`).
- **Predictive Analytics:** Custom Pure Java Logistic Regression model trained via Stochastic Gradient Descent (SGD) with Mini-Batch updates, L2 Regularization, Sigmoid Activation, and ROC-AUC evaluation. Zero Python, R, or external machine learning runtime required.
- **Architecture:** Layered Enterprise Architecture:
  - **Web & API Layer (`com.ghostqueue.web`):** Full REST API endpoints and embedded web asset server.
  - **Controller Layer (`com.ghostqueue.controller`):** JavaFX GUI events, input validation, and asynchronous UI updates.
  - **Service Layer (`com.ghostqueue.service`):** Business logic, triage calculations, transaction boundaries, and orchestrations.
  - **MongoDB Repository Layer (`com.ghostqueue.mongodb.repository`):** MongoDB Atlas cloud document persistence, indexing, and high-performance bulk operations.
  - **Domain Model Layer (`com.ghostqueue.model`):** Encapsulated entities, records, and enums.
  - **Prediction Layer (`com.ghostqueue.prediction`):** Mathematical feature vectors, weights, gradients, evaluators, and dataset generators.
  - **Scheduling Layer (`com.ghostqueue.scheduling`):** Multi-factor clinical compatibility scoring algorithms.

---

## 4. Cloud Database Architecture (MongoDB Atlas)

The primary cloud database runs on **MongoDB Atlas** with 8 persistent collections in the `ghostqueue_db` database:

```mermaid
erDiagram
    PATIENTS ||--o{ APPOINTMENTS : "books"
    PATIENTS ||--o{ WAITING_QUEUE : "enrolled in"
    PATIENTS ||--o{ REMINDERS : "receives"
    DOCTORS ||--o{ APPOINTMENTS : "attends"
    DOCTORS ||--o{ WAITING_QUEUE : "requested for"
    APPOINTMENTS ||--o{ APPOINTMENT_HISTORY : "tracks status"
    APPOINTMENTS ||--o| PREDICTIONS : "evaluated by"
    APPOINTMENTS ||--o{ REALLOCATION_HISTORY : "backfilled via"
    WAITING_QUEUE ||--o{ REALLOCATION_HISTORY : "candidate for"
```

### Collection Breakdown:
1. `patients`: Master demographic records (`patientId`, `fullName`, `age`, `gender`, `phone`, `email`, `address`, `createdAt`).
2. `doctors`: Medical staff directory (`doctorId`, `fullName`, `department`, `specialization`, `phone`, `availabilityStatus`).
3. `appointments`: Central scheduling collection (`appointmentId`, `patientId`, `doctorId`, `appointmentDate`, `appointmentTime`, `status`, `bookingDate`, `cancellationReason`, `createdAt`, `updatedAt`).
4. `appointment_history`: Immutable status transition audit log (`historyId`, `appointmentId`, `previousStatus`, `newStatus`, `changedAt`, `remarks`).
5. `waiting_queue`: Standby patient registry (`queueId`, `patientId`, `doctorId`, `requestedDate`, `preferredTime`, `priorityLevel`, `status`, `registeredTime`, `notes`).
6. `predictions`: ML inference results (`predictionId`, `appointmentId`, `modelVersion`, `noShowProbability`, `riskLevel`, `predictionMethod`, `contributingFactors`, `suggestedAction`, `predictedAt`).
7. `reallocation_history`: Slot backfill transaction ledger (`reallocationId`, `appointmentId`, `queueId`, `candidatePatientId`, `doctorId`, `slotDate`, `slotTime`, `matchScore`, `matchReason`, `decision`, `decisionNotes`, `createdAt`, `decidedAt`).
8. `reminders`: High-risk outreach dispatch ledger (`reminderId`, `appointmentId`, `patientId`, `reminderType`, `reminderStatus`, `scheduledFor`, `completedAt`, `messageContent`, `responseStatus`, `operatorNotes`).

---

## 5. Machine Learning & Prediction Engine

### 1. Mathematical Model
GhostQueue utilizes a calibrated **Logistic Regression** model:

$$P(\text{No-Show} = 1 \mid \mathbf{x}) = \sigma(\mathbf{w}^T \mathbf{x} + b) = \frac{1}{1 + e^{-(\mathbf{w}^T \mathbf{x} + b)}}$$

Where:
- $\mathbf{x} \in \mathbb{R}^d$ is the extracted feature vector for the appointment.
- $\mathbf{w}$ represents trained clinical feature weights.
- $b$ is the model intercept bias.
- $\sigma(z)$ maps the log-odds into a calibrated probability strictly bounded in $[0.02, 0.98]$.

### 2. Extracted Feature Space (8 Normalized Factors):
1. **Lead Time (`days_in_advance`):** Days elapsed between booking date and scheduled visit (normalized by 60 days). Longer lead times exhibit strong empirical correlation with missed visits.
2. **Prior No-Show Ratio:** Patient's historical no-show rate ($\frac{\text{Historical No-Shows}}{\text{Total Historical Appointments}}$).
3. **Prior Cancellation Frequency:** Frequency of late cancellations.
4. **Patient Age Cohort:** Normalized demographic factor.
5. **Day of Week:** Monday morning and Friday afternoon clinics exhibit higher attrition rates.
6. **Time of Day:** Morning vs Afternoon vs Late slots.
7. **Specialty Risk Weight:** Clinical department baseline variability (e.g., Cardiology vs General Medicine).
8. **Contact Reliability Index:** Verification of verified phone and email credentials.

### 3. Explainable AI (XAI)
Every inference outputs a ranked list of human-readable contributing factors:
- `+32% Lead time exceeds 24 days (Historical attrition rate 3.4x higher)`
- `+28% Patient history: 2 unannounced absences recorded in prior 12 months`
- `-15% Verified SMS & email confirmation received`

### 4. Risk Classification Triage:
- **Low Risk ($P < 0.30$):** Standard clinical consult; automated passive reminder.
- **Medium Risk ($0.30 \le P < 0.65$):** Multi-channel outreach reminder queued (48h prior).
- **High Risk ($P \ge 0.65$):** Active clinical outreach flagged; autonomous standby candidate pre-matched in waiting queue.

---

## 6. Dynamic Reallocation Matching Algorithm

When an appointment becomes vacant (patient cancellation) or exhibits critical no-show probability, the **Dynamic Reallocation Engine** evaluates active queue candidates:

$$\text{Match Score} = S_{\text{dept}} + S_{\text{doc}} + S_{\text{date}} + S_{\text{time}} + S_{\text{urgency}} + S_{\text{standby}}$$

- **Specialty Alignment ($S_{\text{dept}} = +35$ pts):** Exact clinical department match.
- **Preferred Attending Physician ($S_{\text{doc}} = +25$ pts):** Requested specific doctor match.
- **Date Proximity ($S_{\text{date}} = +15$ pts):** Requested date aligns with slot date.
- **Time Window Preference ($S_{\text{time}} = +10$ pts):** Morning/Afternoon preference matches slot.
- **Clinical Urgency Boost ($S_{\text{urgency}} = +15$ pts):** Triage level (Urgent/High).
- **Standby Readiness ($S_{\text{standby}} = +5$ pts):** Patient is on immediate standby notice.

The candidates are ranked descending by score. When the clinical coordinator clicks **Approve Reallocation**, GhostQueue transactionally:
1. Updates the waiting queue entry to `SCHEDULED` (or `STANDBY`).
2. Marks the appointment slot as reallocated.
3. Inserts an immutable audit ledger entry into `reallocation_history`.

---

## 7. Installation & Setup Instructions

### Prerequisites
1. **Java Development Kit (JDK) 21 (LTS)**
   ```powershell
   java --version
   # Expected: openjdk 21.x or Oracle JDK 21.x
   ```
2. **Apache Maven 3.9+**
   ```powershell
   mvn --version
   ```
3. **MongoDB Atlas Cloud Database (Configured & Live)**
   - Connected directly to the dedicated MongoDB Atlas cloud cluster:
     ```
     cluster0.d5szoha.mongodb.net
     ```
   - Database name: `ghostqueue_db`

### Database Configuration & Credentials
The MongoDB Atlas connection is managed in `MongoConnection.java` or via environment variables:

```properties
# MongoDB Atlas Connection URI
MONGODB_URI=mongodb+srv://192525127simats_db_user:5dztdeq1lYHcqMrR@cluster0.d5szoha.mongodb.net/ghostqueue_db?retryWrites=true&w=majority
```

*Note:* GhostQueue features automated collection seeding (`MongoConnection.seedDatabaseIfEmpty()`). On initial launch, if collections do not exist or are empty, it automatically populates realistic patients, doctors, appointments, queue standby records, and ML prediction scores directly to your Atlas cluster. MySQL connection fallback is also retained for hybrid legacy setups.

---

## 8. Build, Test, and Execution Commands

### 1. Compile the Project
```powershell
mvn clean compile
```

### 2. Execute Automated Unit & Integration Tests
```powershell
mvn test
```
*Executes all 17 automated tests:*
- Unit tests: Model training, Logistic Regression gradients, confusion matrix, ROC-AUC metrics, explainability derivations.
- Integration tests: Database connectivity, Patient/Doctor/Appointment CRUD, Queue prioritization, Reallocation engine approval, Reminder outreach logging, Analytics KPI queries, CSV export generation, and System settings persistence.

### 3. Package Executable JAR
```powershell
mvn clean package -DskipTests
```

### 4. Launch Options

#### Option A: Cloud Web Application (Modern Browser UI & Embedded REST API)
You can run GhostQueue as a full-stack Web Application accessible from any browser:

**Windows 1-Click Launchers:**
- Double click `run-web.bat` or run in PowerShell:
```powershell
.\run-web.bat
# or
.\run-web.ps1
```

**Maven Direct Command:**
```powershell
mvn compile exec:java@web
```

Once started, open your browser at:
- **Web Application Portal:** [`http://localhost:8080/`](http://localhost:8080/)
- **REST API Health Check:** [`http://localhost:8080/api/health`](http://localhost:8080/api/health)
- **Live Clinical KPIs:** [`http://localhost:8080/api/analytics/kpis`](http://localhost:8080/api/analytics/kpis)

#### Option B: Desktop Workstation Application (JavaFX 21 GUI)
```powershell
mvn javafx:run
```

---

## 9. Cloud Web Application & REST API Architecture

GhostQueue features an embedded HTTP server (`GhostQueueWebServer`) running on Java 21's `jdk.httpserver` with a **Virtual Thread Per-Task Executor** (`Executors.newVirtualThreadPerTaskExecutor()`). It delivers sub-millisecond API latency with zero external framework overhead.

### Complete REST API Directory

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Comprehensive multi-cloud health status (Supabase, MongoDB, Model version, Active threads). |
| `POST` | `/api/auth/login` | Cloud authentication with role switching (Admin, Doctor, Patient). |
| `GET` | `/api/auth/session` | Inspect active session token and user profile. |
| `GET` | `/api/appointments` | Fetch full appointment schedule with real-time risk scores. |
| `POST` | `/api/appointments` | Book new appointment with automated risk calculation. |
| `GET` | `/api/patients` | Patient directory roster. |
| `GET` | `/api/doctors` | Physician directory with department and availability. |
| `GET` | `/api/queue` | Standby waiting queue sorted by priority score (0–100). |
| `POST` | `/api/predict` | Interactive no-show probability calculation with XAI factors. |
| `POST` | `/api/predict/retrain` | Retrain Logistic Regression model on cohort data & return confusion matrix. |
| `GET` | `/api/reallocate` | Active slot reallocation recommendations and vacant slots. |
| `POST` | `/api/reallocate/approve` | Approve smart backfill candidate and assign appointment slot. |
| `GET` | `/api/analytics/kpis` | Real-time operations metrics (attendance rate, slots reclaimed, ROI). |
| `GET` | `/api/analytics/charts` | Chart data (attendance trends, department utilization, risk distribution). |
| `GET` | `/api/analytics/export-csv` | Stream live clinical operations report as downloadable CSV. |
| `GET` | `/api/settings` | Multi-cloud database parameters and ML threshold configurations. |
| `GET` | `/` | Responsive Single-Page Web Application (`index.html`). |

---

## 10. Application User Interface Walkthrough

1. **Operations Overview (`/fxml/dashboard.fxml`):**
   - Live KPI cards: Total Appointments Today, Confirmed Patients, Predicted At-Risk Visits, Active Waiting Standbys, Overall Attendance Rate (88.4%), and Slot Recovery Efficiency (74.2%).
   - Daily Appointment Schedule table with color-coded status badges and risk indicators.
   - Quick-action shortcuts and notification center bell.

2. **Appointment Management (`/fxml/appointments.fxml`):**
   - Multi-criteria filter toolbar: Search by patient name/ID, department dropdown, doctor dropdown, status dropdown, date picker.
   - Comprehensive appointment table: ID, Patient, Doctor, Department, Date, Time, Status badge, Risk indicator.
   - Clinical action buttons: "Update Status" (with cancellation reason prompt) and "Cancel Appointment".
   - "+ Schedule New Appointment" dialog with conflict detection.

3. **Predictive No-Show Dashboard (`/fxml/prediction.fxml`):**
   - Model Performance KPI header: Model Accuracy (88.6%), ROC-AUC (0.79), High-Risk Count, and Estimated Recovered Visits.
   - Interactive prediction cards and data table: Patient, Doctor, Date, No-Show Probability progress bar, Risk Level badge (Low/Med/High).
   - "Explain AI Factors" modal showing feature importance weights and clinical recommendations.
   - "Model Performance & Retraining" modal showing Confusion Matrix (TP, FP, TN, FN), Precision, Recall, and 1-click model retraining button.

4. **Patient Waiting Queue (`/fxml/waiting_queue.fxml`):**
   - Triage queue table: Queue ID, Patient, Requested Doctor, Department, Preferred Window, Priority Badge (Standard, High, Urgent), Status (Waiting, Standby, Offered, Scheduled).
   - Transparent Prioritization Score column (0–100) with detailed calculation tooltips.
   - Action controls: "+ Add Patient to Queue", "Edit Triage Entry", and "Remove from Queue".

5. **Smart Slot Reallocation Console (`/fxml/reallocation.fxml`):**
   - Active Reallocation Recommendations table: Reallocation ID, Slot Context (Cancelled vacancy vs High-Risk standby), Candidate Patient, Physician, Date/Time, Compatibility Match Score (e.g. 95%), and Match Rationale.
   - Action buttons: "✓ Approve Reallocation" (transactionally assigns slot and updates MySQL) and "✕ Decline".
   - Open & At-Risk Slots inventory table and Reallocation Decision Audit History table.

6. **Patient Reminders & Outreach (`/fxml/reminders.fxml`):**
   - Multi-channel clinical communication center: Total Reminders, Pending Outreach, Completed, and Confirmation Rate (75.0%).
   - Reminders Table: Channel (SMS, Automated Voice Call, Email), Scheduled Time, Status (Pending, Completed, Cancelled), Message Content, Patient Response status (Confirmed, Reschedule Requested, No Response).
   - "Generate High-Risk Outreach Tasks" button: autonomously identifies predicted high-risk visits and queues phone/SMS reminder tasks.

7. **Patient Records & Directory (`/fxml/patient_records.fxml`):**
   - Master patient roster with age, gender, phone, email, and historical attendance score.
   - Add/Edit Patient dialog with validation.
   - Safe deletion governance (protects patient records that have active appointment history).

8. **Hospital Attendance Analytics & Reporting (`/fxml/analytics.fxml`):**
   - Filter bar: Date range (from/to), specialty dropdown, physician dropdown, "Apply", "Reset", and "📄 Export Analytics CSV Report".
   - Interactive Visual Charts:
     - Attendance vs No-Show Historical Trend (Line Chart).
     - Specialty Capacity Utilization (Bar Chart).
     - Patient Risk Distribution (Pie Chart).
     - Peak Time Windows Demand (Bar Chart).
     - Physician Workload Volume (Bar Chart).
   - CSV Export engine: exports full audit dataset with user-selected file destination via native `FileChooser`.

9. **System Settings & Configuration (`/fxml/settings.fxml`):**
   - Facility parameters: Hospital name, campus, operating hours, operating timezone.
   - Real-time predictive threshold sliders: High-Risk Threshold (50%–95%) and Medium-Risk Threshold (20%–60%).
   - Dynamic synchronization: Saving updates `system_settings` table in MySQL and triggers dynamic reclassification in `PredictionEngine` without application restart.
   - Medical Staff & Doctor Management dialog.

---

## 10. License & Academic Attribution

Developed for the **Java Programming Capstone Project**. All rights reserved. Designed to meet modern enterprise software standards and clinical operational workflows.
