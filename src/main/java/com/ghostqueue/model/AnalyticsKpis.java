package com.ghostqueue.model;

/**
 * Encapsulates operational healthcare analytics key performance indicators (KPIs).
 */
public record AnalyticsKpis(
        int totalAppointments,
        int completedCount,
        int noShowCount,
        int cancelledCount,
        int bookedCount,
        double attendanceRate,
        double noShowRate,
        double slotRecoveryEfficiency,
        int activeQueueStandbys
) {
    public static AnalyticsKpis empty() {
        return new AnalyticsKpis(0, 0, 0, 0, 0, 0.0, 0.0, 0.0, 0);
    }
}
