package InventoryManagementSystem;

public class AlertFactory {

    // 1. Changed return type from void to AlertListener
    // 2. Made it static so AlertRule can call it directly without "new AlertFactory()"
    public static AlertListener getAlertListener(AlertEnum alertEnum) {

        // 3. Added the 'return' keyword to yield the result of the switch expression
        return switch (alertEnum) {
            case Email -> new EmailAlertListener();
            case Console -> new ConsoleAlertListener();
            default -> new ConsoleAlertListener();
        };
    }
}