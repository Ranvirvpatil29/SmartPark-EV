package evcharging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PricingDAO {

    public double getBasePrice() throws SQLException {
        String sql = "SELECT base_price FROM pricing_rules " +
                     "ORDER BY rule_id DESC LIMIT 1";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                double basePrice = resultSet.getDouble("base_price");

                if (!Double.isFinite(basePrice) || basePrice < 0) {
                    throw new SQLException(
                            "The base price in the database is invalid.");
                }

                return basePrice;
            }
        }

        throw new SQLException(
                "No pricing rule found. Add a row to pricing_rules.");
    }

    public double getMediumDemandMultiplier() throws SQLException {
        return getMultiplier("medium_demand_multiplier");
    }

    public double getHighDemandMultiplier() throws SQLException {
        return getMultiplier("high_demand_multiplier");
    }

    private double getMultiplier(String columnName) throws SQLException {
        // Only the two predefined column names are allowed.
        if (!columnName.equals("medium_demand_multiplier")
                && !columnName.equals("high_demand_multiplier")) {
            throw new IllegalArgumentException("Invalid pricing column.");
        }

        String sql = "SELECT " + columnName +
                     " FROM pricing_rules ORDER BY rule_id DESC LIMIT 1";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                double multiplier = resultSet.getDouble(1);

                if (!Double.isFinite(multiplier) || multiplier < 1) {
                    throw new SQLException(
                            "The pricing multiplier in the database is invalid.");
                }

                return multiplier;
            }
        }

        throw new SQLException(
                "No pricing rule found. Add a row to pricing_rules.");
    }
}