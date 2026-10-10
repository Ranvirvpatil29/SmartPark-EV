package evcharging;

public class Charger {

    private final int chargerId;
    private final String location;
    private final double powerKW;
    private boolean available;

    public Charger(int chargerId, String location, double powerKW) {
        if (chargerId <= 0) {
            throw new IllegalArgumentException("Charger ID must be positive.");
        }
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location cannot be empty.");
        }
        if (!Double.isFinite(powerKW) || powerKW <= 0) {
            throw new IllegalArgumentException("Charger power must be positive.");
        }

        this.chargerId = chargerId;
        this.location = location;
        this.powerKW = powerKW;
        this.available = true;
    }

    public int getChargerId() {
        return chargerId;
    }

    public String getLocation() {
        return location;
    }

    public double getPowerKW() {
        return powerKW;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void displayDetails() {
        System.out.println("Charger ID: " + chargerId);
        System.out.println("Location: " + location);
        System.out.printf("Power: %.1f kW%n", powerKW);
        System.out.println("Available: " + available);
    }
}