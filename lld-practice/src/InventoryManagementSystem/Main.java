package InventoryManagementSystem;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // 1. Setup the System
        InventoryManager manager = new InventoryManager();

        Warehouse newYork = new Warehouse("W_NY", "New York");
        Warehouse boston = new Warehouse("W_BOS", "Boston");

        manager.addWarehouse(newYork);
        manager.addWarehouse(boston);

        // 2. Add Products (Using empty alert lists to keep it simple for this test)
        String productId = "PROD_IPHONE";
        newYork.addProduct(productId, "iPhone 15", 100, new ArrayList<>());
        boston.addProduct(productId, "iPhone 15", 50, new ArrayList<>());

        System.out.println("Initial Stock NY: " + newYork.getInventory(productId).getStock());
        System.out.println("Initial Stock BOS: " + boston.getInventory(productId).getStock());
        System.out.println("-------------------------------------------------");

        // 3. Setup Concurrency Test
        // A CountDownLatch acts like a starting gun in a race.
        // It holds all threads at the starting line until we count down to zero.
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(2);

        // Thread 1: Transfer 10 iPhones from NY to BOS
        Thread thread1 = new Thread(() -> {
            try {
                startGun.await(); // Wait for the green light
                System.out.println("Thread 1: Attempting transfer NY -> BOS");
                manager.transferInventory("W_NY", "W_BOS", productId, 10);
            } catch (Exception e) {
                System.err.println("Thread 1 Failed: " + e.getMessage());
            } finally {
                finishLine.countDown();
            }
        });

        // Thread 2: Transfer 5 iPhones from BOS to NY
        Thread thread2 = new Thread(() -> {
            try {
                startGun.await(); // Wait for the green light
                System.out.println("Thread 2: Attempting transfer BOS -> NY");
                manager.transferInventory("W_BOS", "W_NY", productId, 5);
            } catch (Exception e) {
                System.err.println("Thread 2 Failed: " + e.getMessage());
            } finally {
                finishLine.countDown();
            }
        });

        // 4. Start the threads (they will pause at startGun.await())
        thread1.start();
        thread2.start();

        // 5. Fire the starting gun! Both threads hit the locks at the exact same time.
        startGun.countDown();

        // 6. Wait for both threads to finish before printing final results
        finishLine.await();

        System.out.println("-------------------------------------------------");
        System.out.println("Final Stock NY: " + newYork.getInventory(productId).getStock());
        System.out.println("Final Stock BOS: " + boston.getInventory(productId).getStock());
    }
}