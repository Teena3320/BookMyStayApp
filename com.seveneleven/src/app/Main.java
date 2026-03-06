package app;

import model.InventorySnapshot;
import services.InventoryService;

import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;

/**
 * Use Case 1: Room Inventory Setup & Management
 * - Single source of truth for room counts and prices.
 * - O(1) lookups with HashMap.
 * - Thread-safe via ReadWriteLock.
 */
public class Main {

 private static final Scanner SC = new Scanner(System.in);

 public static void main(String[] args) {
     InventoryService inventory = new InventoryService();
     seedDefaults(inventory); // optional seeding

     boolean running = true;
     while (running) {
         printMenu();
         int choice = readInt("Choose an option: ");
         switch (choice) {
             case 1 -> addRoomType(inventory);
             case 2 -> updateCount(inventory);
             case 3 -> updatePrice(inventory);
             case 4 -> viewCounts(inventory);
             case 5 -> viewPrices(inventory);
             case 6 -> viewSnapshot(inventory);
             case 7 -> getCount(inventory);  
             case 8 -> getPrice(inventory);  
             case 9 -> running = false;
             default -> System.out.println("Invalid option. Try again.");
         }
         if (running) {
             System.out.println("\nPress ENTER to continue...");
             SC.nextLine();
         }
     }
     System.out.println("Goodbye!");
 }

 private static void printMenu() {
     System.out.println("\n=== BookMyStay Inventory Management ===");
     System.out.println("1) Add Room Type");
     System.out.println("2) Update Count (±delta)");
     System.out.println("3) Update Price");
     System.out.println("4) View All Counts");
     System.out.println("5) View All Prices");
     System.out.println("6) View Full Snapshot");
     System.out.println("7) Get Count by Room Type ( e.g., Single, Double)");
     System.out.println("8) Get Price by Room Type ( e.g., Single, Double)");
     System.out.println("9) Exit");
 }

 private static void addRoomType(InventoryService inventory) {
     String type = readString("Enter room type name (e.g., Single, Double, Suite): ");
     int count = readInt("Enter initial count (integer >= 0): ");
     double price = readDouble("Enter price per night (number >= 0): ");
     try {
         inventory.addRoomType(type, count, price);
         System.out.println("Added room type '" + type + "' with count=" + count + ", price=" + price);
     } catch (IllegalArgumentException ex) {
         System.out.println("Error: " + ex.getMessage());
     }
 }

 private static void updateCount(InventoryService inventory) {
     String type = readString("Enter room type name to update (e.g., Single): ");
     int delta = readInt("Enter count delta (integer; positive to add, negative to reduce): ");
     try {
         inventory.updateCount(type, delta);
         System.out.println("Updated '" + type + "' by delta " + delta +
                 ". New count: " + inventory.getAvailableCount(type));
     } catch (IllegalArgumentException ex) {
         System.out.println("Error: " + ex.getMessage());
     }
 }

 private static void updatePrice(InventoryService inventory) {
     String type = readString("Enter room type name to update (e.g., Double): ");
     double price = readDouble("Enter new price per night (number >= 0): ");
     try {
         inventory.updatePrice(type, price);
         System.out.println("Updated price for '" + type + "' to " + String.format("%.2f", inventory.getPrice(type)));
     } catch (IllegalArgumentException ex) {
         System.out.println("Error: " + ex.getMessage());
     }
 }

 private static void viewCounts(InventoryService inventory) {
     System.out.println("\n-- Room Counts --");
     Map<String, Integer> counts = inventory.snapshotCounts();
     if (counts.isEmpty()) {
         System.out.println("  (no room types found)");
         return;
     }
     for (Map.Entry<String, Integer> e : counts.entrySet()) {
         System.out.printf("  %-12s : %d%n", e.getKey(), e.getValue());
     }
 }

 private static void viewPrices(InventoryService inventory) {
     System.out.println("\n-- Room Prices --");
     Map<String, Double> prices = inventory.snapshotPrices();
     if (prices.isEmpty()) {
         System.out.println("  (no room types found)");
         return;
     }
     for (Map.Entry<String, Double> e : prices.entrySet()) {
         System.out.printf("  %-12s : %.2f%n", e.getKey(), e.getValue());
     }
 }

 private static void viewSnapshot(InventoryService inventory) {
     InventorySnapshot snapshot = inventory.snapshot();
     System.out.println("\n-- Inventory Snapshot --");
     System.out.println("Counts:");
     if (snapshot.getCounts().isEmpty()) {
         System.out.println("  (no room types found)");
     } else {
         for (Map.Entry<String, Integer> e : snapshot.getCounts().entrySet()) {
             System.out.printf("  %-12s : %d%n", e.getKey(), e.getValue());
         }
     }
     System.out.println("Prices:");
     if (snapshot.getPrices().isEmpty()) {
         System.out.println("  (no room types found)");
     } else {
         for (Map.Entry<String, Double> e : snapshot.getPrices().entrySet()) {
             System.out.printf("  %-12s : %.2f%n", e.getKey(), e.getValue());
         }
     }
 }

 private static void getCount(InventoryService inventory) {
     String type = readString("Enter room type name to check count (e.g., Suite): ");
     try {
         int count = inventory.getAvailableCount(type);
         System.out.println("Available '" + type + "': " + count);
     } catch (IllegalArgumentException ex) {
         System.out.println("Error: " + ex.getMessage());
     }
 }

 private static void getPrice(InventoryService inventory) {
     String type = readString("Enter room type name to check price (e.g., Single): ");
     try {
         double price = inventory.getPrice(type);
         System.out.printf("Price per night for '%s': %.2f%n", type, price);
     } catch (IllegalArgumentException ex) {
         System.out.println("Error: " + ex.getMessage());
     }
 }

 private static void seedDefaults(InventoryService inventory) {
     try {
         inventory.addRoomType("Single", 10, 2999.00);
         inventory.addRoomType("Double", 8, 4599.50);
         inventory.addRoomType("Suite", 3, 11999.00);
     } catch (IllegalArgumentException ignored) {
     }
 }


 private static String readString(String prompt) {
     System.out.print(prompt);
     String line = SC.nextLine();
     while (line == null || line.trim().isEmpty()) {
         System.out.print("Input cannot be blank. " + prompt);
         line = SC.nextLine();
     }
     return line.trim();
 }

 private static int readInt(String prompt) {
     while (true) {
         System.out.print(prompt);
         try {
             String line = SC.nextLine();
             return Integer.parseInt(line.trim());
         } catch (NumberFormatException | InputMismatchException e) {
             System.out.println("Please enter a valid integer.");
         }
     }
 }

 private static double readDouble(String prompt) {
     while (true) {
         System.out.print(prompt);
         try {
             String line = SC.nextLine();
             return Double.parseDouble(line.trim());
         } catch (NumberFormatException | InputMismatchException e) {
             System.out.println("Please enter a valid number.");
         }
     }
 }
}
