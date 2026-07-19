package InventoryManagementSystem;

public class AlertRule {
    int threshold;
    AlertListener listener;
    AlertEnum alertType;
    boolean hasTriggered;

    public AlertRule(int threshold, AlertEnum alertType) {
        this.threshold = threshold;
        this.alertType = alertType;
        this.hasTriggered = false;

        // Let the factory handle the creation of the specific listener
        // (e.g., EmailListener, SMSListener) based on the enum provided.
        this.listener = AlertFactory.getAlertListener(alertType);
    }

    public int getThreshold() {
        return threshold;
    }

    public boolean isTriggered() {
        return hasTriggered;
    }

    public void setTriggered(boolean hasTriggered) {
        this.hasTriggered = hasTriggered;
    }

    public AlertEnum getAlertType() {
        return alertType;
    }

    public void trigger(String message) {
        // The rule just delegates the actual sending to the factory-provided listener
        if (listener != null) {
            listener.onLowStock(message);
        }
    }
}