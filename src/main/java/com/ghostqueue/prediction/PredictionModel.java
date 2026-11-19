package com.ghostqueue.prediction;

import com.ghostqueue.model.Appointment;
import com.ghostqueue.model.Patient;

import java.util.List;

public interface PredictionModel {
    double predictNoShowProbability(Appointment appointment, Patient patient);
    List<String> extractRiskFactors(Appointment appointment, Patient patient);
    double getModelConfidence();
}
