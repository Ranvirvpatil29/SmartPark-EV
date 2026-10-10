
import evcharging.Charger;
import evcharging.ChargerDAO;
import evcharging.ChargingSession;
import evcharging.ChargingSessionDAO;
import evcharging.DatabaseConnection;
import evcharging.DynamicPricing;
import evcharging.PricingDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    private static final ChargerDAO chargerDAO = new ChargerDAO();
    private static final ChargingSessionDAO sessionDAO =
            new ChargingSessionDAO();
    private static final PricingDAO pricingDAO = new PricingDAO();
    private static final DynamicPricing dynamicPricing =
            new DynamicPricing();

    public static void main(String[] args) {
        System.out.println("Welcome to SmartPark-EV!");

        boolean running = true;

        while (running) {
            displayMenu();

            int choice = readInt("Choose an option: ");

            try {
                switch (choice) {
                    case 1:
                        viewChargers();
                        break;

                    case 2:
                        bookChargingSession();
                        break;

                    case 3:
                        sessionDAO.displayRevenueReport();
                        break;

                    case 4:
                        sessionDAO.displayChargerRevenueReport();
                        break;

                    case 5:
                        completeChargingSession();
                        break;

                    case 6:
                        running = false;
                        System.out.println("Thank you for using SmartPark-EV!");
                        break;

                    default:
                        System.out.println(
                                "Invalid option. Please choose 1 to 6.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid input: " + e.getMessage());
            }

            System.out.println();
        }

        scanner.close();
    }

    private static void displayMenu() {
        System.out.println("===== SMARTPARK-EV =====");
        System.out.println("1. View chargers");
        System.out.println("2. Book a charging session");
        System.out.println("3. View total revenue report");
        System.out.println("4. View revenue by charger");
        System.out.println("5. Complete a charging session");
        System.out.println("6. Exit");
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                double value = Double.parseDouble(input);

                if (!Double.isFinite(value)) {
                    System.out.println("Please enter a finite number.");
                    continue;
                }

                return value;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static void viewChargers() throws SQLException {
        List<Charger> chargers = chargerDAO.getAllChargers();

        System.out.println("\n========== AVAILABLE CHARGERS ==========");

        if (chargers.isEmpty()) {
            System.out.println("No chargers found.");
            return;
        }

        for (Charger charger : chargers) {
            charger.displayDetails();
            System.out.println("----------------------------------------");
        }
    }

    private static void bookChargingSession() throws SQLException {
        List<Charger> chargers = chargerDAO.getAllChargers();

        if (chargers.isEmpty()) {
            System.out.println("No chargers are registered.");
            return;
        }

        int totalChargers = chargers.size();
        int availableChargers = 0;
        Charger selectedCharger = null;

        for (Charger charger : chargers) {
            if (charger.isAvailable()) {
                availableChargers++;

                if (selectedCharger == null) {
                    selectedCharger = charger;
                }
            }
        }

        if (selectedCharger == null) {
            System.out.println("Sorry, no chargers are currently available.");
            return;
        }

        double basePrice = pricingDAO.getBasePrice();
        double mediumMultiplier =
                pricingDAO.getMediumDemandMultiplier();
        double highMultiplier =
                pricingDAO.getHighDemandMultiplier();

        double currentPrice = dynamicPricing.calculatePrice(
                basePrice,
                totalChargers,
                availableChargers,
                mediumMultiplier,
                highMultiplier
        );

        System.out.printf("Current price: Rs. %.2f/kWh%n", currentPrice);
        System.out.println(
                "Available chargers: " + availableChargers
                        + " out of " + totalChargers);
        System.out.println(
                "Selected charger: " + selectedCharger.getChargerId());

        int sessionId = readInt("Enter a new session ID: ");

        if (sessionId <= 0) {
            System.out.println("Session ID must be positive.");
            return;
        }

        ChargingSession session = new ChargingSession(
                sessionId,
                selectedCharger,
                0.0,
                currentPrice
        );

        boolean booked = sessionDAO.startChargingSession(session);

        if (booked) {
            System.out.println(
                    "Booking successful! Session ID: " + sessionId);
        } else {
            System.out.println(
                    "Booking was not completed. Please check charger availability.");
        }
    }

    private static void completeChargingSession() throws SQLException {
        int sessionId = readInt("Enter active session ID: ");

        if (sessionId <= 0) {
            System.out.println("Session ID must be positive.");
            return;
        }

        double energyKWh = readDouble("Enter energy consumed (kWh): ");

        if (energyKWh <= 0) {
            System.out.println("Energy consumed must be greater than zero.");
            return;
        }

        boolean completed =
                sessionDAO.completeChargingSession(sessionId, energyKWh);

        if (completed) {
            double pricePerKWh = sessionDAO.getSessionPrice(sessionId);
            double finalCost = energyKWh * pricePerKWh;

            System.out.println("Charging session completed!");
            System.out.printf("Final cost: Rs. %.2f%n", finalCost);
        } else {
            System.out.println(
                    "No active session was completed. Check the session ID.");
        }
    }
}
