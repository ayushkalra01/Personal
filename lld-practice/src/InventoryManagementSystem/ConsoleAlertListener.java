package InventoryManagementSystem;

public class ConsoleAlertListener implements AlertListener{
    @Override
    public void onLowStock(String message) {
    System.out.println("console Alert " + message);
    }
}
