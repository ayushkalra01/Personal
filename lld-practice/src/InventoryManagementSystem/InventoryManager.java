package InventoryManagementSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class InventoryManager {
    // Initialized as a ConcurrentHashMap to handle multiple warehouses being added safely
    private final Map<String, Warehouse> warehouseMap = new ConcurrentHashMap<>();

    public void addWarehouse(Warehouse warehouse) {
        if (warehouse != null && warehouse.warehouseId != null) {
            warehouseMap.put(warehouse.warehouseId, warehouse);
        }
    }

    // Returns a list of Warehouse IDs that can fulfill the quantity
    public List<String> checkAvailability(String productId, int qty) {
        List<String> availableWarehouses = new ArrayList<>();

        // Iterate through all registered warehouses
        for (Warehouse warehouse : warehouseMap.values()) {
            if (warehouse.hasAvailableStock(productId, qty)) {
                availableWarehouses.add(warehouse.warehouseId);
            }
        }

        return availableWarehouses;
    }

    public void transferInventory(String fromId, String toId, String productId, int qty) throws InterruptedException {
        // ... standard validations (check if warehouses and products exist) ...

        Inventory fromInventory = warehouseMap.get(fromId).getInventory(productId);
        Inventory toInventory = warehouseMap.get(toId).getInventory(productId);

        // 1. Sort locks to prevent Deadlocks (Lock Ordering)
        ReentrantLock firstLock = fromId.compareTo(toId) < 0 ? fromInventory.getLock() : toInventory.getLock();
        ReentrantLock secondLock = fromId.compareTo(toId) < 0 ? toInventory.getLock() : fromInventory.getLock();

        // 2. Try to acquire the first lock with a timeout
        if (!firstLock.tryLock(3, TimeUnit.SECONDS)) {
            throw new RuntimeException("System busy: Could not acquire first lock for transfer.");
        }

        try {
            // 3. Try to acquire the second lock with a timeout
            if (!secondLock.tryLock(3, TimeUnit.SECONDS)) {
                // If this fails, we throw an exception.
                // The 'finally' block below will automatically release the first lock!
                throw new RuntimeException("System busy: Could not acquire second lock for transfer.");
            }

            try {
                // --- CRITICAL TRANSACTION SECTION ---
                // We now hold BOTH locks. Now (and ONLY now) we check and modify data.

                if (fromInventory.getStock() < qty) {
                    throw new IllegalStateException("Insufficient stock to complete transfer.");
                }

                warehouseMap.get(fromId).removeStock(productId, qty);
                warehouseMap.get(toId).addStock(productId, qty);

                System.out.println("Transfer successful!");

            } finally {
                // Always release the second lock
                secondLock.unlock();
            }
        } finally {
            // Always release the first lock
            firstLock.unlock();
        }
    }
}