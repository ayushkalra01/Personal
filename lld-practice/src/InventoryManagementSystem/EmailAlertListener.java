package InventoryManagementSystem;

public class EmailAlertListener implements AlertListener{
    @Override
    public void onLowStock(String message) {
    System.out.println("Email Alert" + message);
    }
}
