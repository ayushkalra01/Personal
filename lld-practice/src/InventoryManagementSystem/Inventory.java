package InventoryManagementSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Inventory {
    String productId;
    String name;
    int stock;
    List<AlertRule> alertRules;

    // Create a dedicated lock for this specific product's inventory
    private final ReentrantLock lock;

    public Inventory(String productId, String name, int initialStock, List<AlertRule> alertRules) {
        this.productId = productId;
        this.name = name;
        this.stock = initialStock;
        this.alertRules = alertRules;
        this.lock = new ReentrantLock();
    }

    public void removeStock(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        List<AlertRule> rulesToFire = new ArrayList<>();
        int currentStock;

        // 1. Acquire the lock
        lock.lock();
        try {
            // --- CRITICAL SECTION START ---
            if (this.stock < qty) {
                throw new IllegalStateException("Insufficient stock for product: " + productId);
            }

            this.stock -= qty;
            currentStock = this.stock;

            // Check rules and update state safely inside the lock
            if (alertRules != null) {
                for (AlertRule rule : alertRules) {
                    if (this.stock <= rule.getThreshold() && !rule.isTriggered()) {
                        rule.setTriggered(true);
                        rulesToFire.add(rule); // Queue it up!
                    }
                }
            }
            // --- CRITICAL SECTION END ---

        } finally {
            // 2. ALWAYS release in a finally block so an exception doesn't freeze the product
            lock.unlock();
        }

        // 3. Trigger the slow I/O (emails/webhooks) OUTSIDE the lock
        for (AlertRule rule : rulesToFire) {
            rule.trigger("Stock for " + name + " dropped to " + currentStock);
        }
    }

    public void addStock(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        lock.lock();
        try {
            this.stock += qty;

            // Resetting just modifies booleans, so it's safe to do inside the lock
            if (alertRules != null) {
                for (AlertRule rule : alertRules) {
                    if (this.stock > rule.getThreshold() && rule.isTriggered()) {
                        rule.setTriggered(false);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public int getStock() {
        lock.lock();
        try {
            return this.stock;
        } finally {
            lock.unlock();
        }
    }

    public ReentrantLock getLock() {
        return lock;
    }
}