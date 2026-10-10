package evcharging;

public class DynamicPricing {

    private static final double DEFAULT_MEDIUM_MULTIPLIER = 1.10;
    private static final double DEFAULT_HIGH_MULTIPLIER = 1.20;

    public double calculatePrice(
            double basePrice,
            int totalChargers,
            int availableChargers) {

        return calculatePrice(
                basePrice,
                totalChargers,
                availableChargers,
                DEFAULT_MEDIUM_MULTIPLIER,
                DEFAULT_HIGH_MULTIPLIER
        );
    }

    public double calculatePrice(
            double basePrice,
            int totalChargers,
            int availableChargers,
            double mediumDemandMultiplier,
            double highDemandMultiplier) {

        if (!Double.isFinite(basePrice) || basePrice < 0) {
            throw new IllegalArgumentException(
                    "Base price must be non-negative.");
        }

        if (totalChargers <= 0
                || availableChargers < 0
                || availableChargers > totalChargers) {
            throw new IllegalArgumentException(
                    "Invalid charger availability values.");
        }

        if (!Double.isFinite(mediumDemandMultiplier)
                || mediumDemandMultiplier < 1) {
            throw new IllegalArgumentException(
                    "Medium-demand multiplier must be at least 1.");
        }

        if (!Double.isFinite(highDemandMultiplier)
                || highDemandMultiplier < 1) {
            throw new IllegalArgumentException(
                    "High-demand multiplier must be at least 1.");
        }

        double availabilityRatio =
                (double) availableChargers / totalChargers;

        if (availabilityRatio <= 0.20) {
            return basePrice * highDemandMultiplier;
        } else if (availabilityRatio <= 0.50) {
            return basePrice * mediumDemandMultiplier;
        }

        return basePrice;
    }
}