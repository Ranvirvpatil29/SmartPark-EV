package evcharging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ChargerDAO {

    public List<Charger> getAllChargers() throws SQLException {
        List<Charger> chargers = new ArrayList<>();

        String sql = "SELECT charger_id, location, power_kw, is_available " +
                     "FROM chargers ORDER BY charger_id";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Charger charger = new Charger(
                        resultSet.getInt("charger_id"),
                        resultSet.getString("location"),
                        resultSet.getDouble("power_kw")
                );

                charger.setAvailable(resultSet.getBoolean("is_available"));
                chargers.add(charger);
            }
        }

        return chargers;
    }

    public int countAvailableChargers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM chargers WHERE is_available = TRUE";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }

        return 0;
    }
}