package InventoryManagementSystem;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Warehouse {
    String warehouseId;
    String location;
    Map<String, Inventory> productInventoryMap;

    public Warehouse(String warehouseId, String location) {
        productInventoryMap = new ConcurrentHashMap<>();
        this.warehouseId = warehouseId;
        this.location = location;
    }

    public void addProduct(String productId, String name, int initialStock, List<AlertRule> rules) {
        Inventory newProduct = new Inventory(productId, name, initialStock, rules);
        productInventoryMap.put(productId, newProduct);
    }
    public void addStock(String productId, int qty) {
        Inventory currInventory = productInventoryMap.get(productId);
        if(currInventory != null) {
            currInventory.addStock(qty);
        }
    }
    public void removeStock(String productId, int qty) {
        Inventory currInventory = productInventoryMap.get(productId);
        if(currInventory != null) {
            currInventory.removeStock(qty);
        }
    }
    public boolean hasAvailableStock(String productId, int qty) {
        Inventory currInventory = productInventoryMap.get(productId);
        boolean b = false;
        if(currInventory != null) {
             b = currInventory.getStock() >= qty;
        }
        return b;
    }
    public Inventory getInventory(String productId) {
        return productInventoryMap.get(productId);
    }
}
