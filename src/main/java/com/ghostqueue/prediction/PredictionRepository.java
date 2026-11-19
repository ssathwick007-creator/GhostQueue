package com.ghostqueue.prediction;

import com.ghostqueue.model.Department;
import com.ghostqueue.model.RiskLevel;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access contract for appointment predictions persistence in MySQL.
 */
public interface PredictionRepository {
    void save(PredictionResult result);
    void saveBatch(List<PredictionResult> results);
    Optional<PredictionResult> findById(String predictionId);
    Optional<PredictionResult> findLatestByAppointmentId(String appointmentId);
    List<PredictionResult> findAll();
    List<PredictionResult> findLatestPredictions();
    List<PredictionResult> filter(String query, Department department, String doctorId, RiskLevel riskLevel, LocalDate date);
    void updateActualOutcome(String predictionId, String outcome, String evaluationStatus);
    int countTotal();
    int countByRiskLevel(RiskLevel riskLevel);
    double getAverageProbability();
}
