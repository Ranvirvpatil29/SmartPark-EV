package evcharging;

public class ChargingSession {

    private final int sessionId;
    private final Charger charger;
    private final double energyConsumedKWh;
    private final double pricePerKWh;

    public ChargingSession(
            int sessionId,
            Charger charger,
            double energyConsumedKWh,
            double pricePerKWh) {

        if (sessionId <= 0) {
            throw new IllegalArgumentException("Session ID must be positive.");
        }
        if (charger == null) {
            throw new IllegalArgumentException("Charger cannot be null.");
        }
        if (!Double.isFinite(energyConsumedKWh) || energyConsumedKWh < 0) {
            throw new IllegalArgumentException(
                    "Energy consumed must be non-negative.");
        }
        if (!Double.isFinite(pricePerKWh) || pricePerKWh < 0) {
            throw new IllegalArgumentException(
                    "Price per kWh must be non-negative.");
        }

        this.sessionId = sessionId;
        this.charger = charger;
        this.energyConsumedKWh = energyConsumedKWh;
        this.pricePerKWh = pricePerKWh;
    }

    public int getSessionId() {
        return sessionId;
    }

    public Charger getCharger() {
        return charger;
    }

    public double getEnergyConsumedKWh() {
        return energyConsumedKWh;
    }

    public double getPricePerKWh() {
        return pricePerKWh;
    }

    public double calculateCost() {
        return energyConsumedKWh * pricePerKWh;
    }

    public void displayDetails() {
        System.out.println("Session ID: " + sessionId);
        System.out.println("Charger ID: " + charger.getChargerId());
        System.out.printf("Energy consumed: %.2f kWh%n",
                energyConsumedKWh);
        System.out.printf("Price per kWh: Rs. %.2f%n", pricePerKWh);
        System.out.printf("Total cost: Rs. %.2f%n", calculateCost());
    }
}