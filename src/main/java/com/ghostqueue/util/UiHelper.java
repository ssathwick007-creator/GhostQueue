package com.ghostqueue.util;

import com.ghostqueue.model.AppointmentStatus;
import com.ghostqueue.model.QueuePriority;
import com.ghostqueue.model.QueueStatus;
import com.ghostqueue.model.RiskLevel;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public final class UiHelper {

    private UiHelper() {}

    public static Label createStatusBadge(AppointmentStatus status) {
        Label badge = new Label(status.getDisplayName());
        badge.getStyleClass().addAll("badge", status.getStyleClass());
        return badge;
    }

    public static Label createRiskBadge(RiskLevel riskLevel, int percent) {
        Label badge = new Label(riskLevel.getLabel() + " (" + percent + "%)");
        badge.getStyleClass().addAll("risk-badge", riskLevel.getStyleClass());
        return badge;
    }

    public static Label createPriorityBadge(QueuePriority priority) {
        Label badge = new Label(priority.getLabel());
        badge.getStyleClass().addAll("priority-badge", priority.getStyleClass());
        return badge;
    }

    public static Label createQueueStatusBadge(QueueStatus status) {
        Label badge = new Label(status.getDisplayName());
        badge.getStyleClass().addAll("badge", status.getStyleClass());
        return badge;
    }
}
