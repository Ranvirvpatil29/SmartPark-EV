
package evcharging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ChargingSessionDAO {

    /**
     * Saves a completed charging session.
     */
    public void saveSession(ChargingSession session) throws SQLException {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null.");
        }

        if (session.getSessionId() <= 0) {
            throw new IllegalArgumentException("Session ID must be positive.");
        }

        validateNonNegativeFinite(
                session.getEnergyConsumedKWh(), "Energy consumed");
        validateNonNegativeFinite(
                session.getPricePerKWh(), "Price per kWh");

        String sql = "INSERT INTO charging_sessions " +
                "(session_id, charger_id, energy_kwh, price_per_kwh, session_status) " +
                "VALUES (?, ?, ?, ?, 'COMPLETED')";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, session.getSessionId());
            statement.setInt(2, session.getCharger().getChargerId());
            statement.setDouble(3, session.getEnergyConsumedKWh());
            statement.setDouble(4, session.getPricePerKWh());

            statement.executeUpdate();
            System.out.println("Charging session saved successfully.");

        } catch (SQLException e) {
            throw new SQLException(
                    "Unable to save charging session. Check the session ID, charger ID, and database constraints.",
                    e);
        }
    }

    /**
     * Displays all charging sessions.
     */
    public void displayAllSessions() throws SQLException {
        String sql = "SELECT session_id, charger_id, energy_kwh, " +
                "price_per_kwh, session_status, started_at " +
                "FROM charging_sessions ORDER BY session_id";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            System.out.println("\n========== CHARGING SESSION HISTORY ==========");

            boolean found = false;

            while (resultSet.next()) {
                found = true;

                System.out.println("----------------------------------------------");
                System.out.println("Session ID: " +
                        resultSet.getInt("session_id"));
                System.out.println("Charger ID: " +
                        resultSet.getInt("charger_id"));
                System.out.printf("Energy (kWh): %.2f%n",
                        resultSet.getDouble("energy_kwh"));
                System.out.printf("Price per kWh: Rs. %.2f%n",
                        resultSet.getDouble("price_per_kwh"));
                System.out.println("Status: " +
                        resultSet.getString("session_status"));
                System.out.println("Started at: " +
                        resultSet.getTimestamp("started_at"));
            }

            if (!found) {
                System.out.println("No charging sessions found.");
            }

            System.out.println("==============================================");

        } catch (SQLException e) {
            throw new SQLException(
                    "Unable to retrieve charging session history.", e);
        }
    }

    /**
     * Reserves an available charger and creates an ACTIVE session.
     * Returns false if the charger is no longer available.
     */
    public boolean startChargingSession(ChargingSession session)
            throws SQLException {

        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null.");
        }

        if (session.getSessionId() <= 0) {
            throw new IllegalArgumentException("Session ID must be positive.");
        }

        if (session.getCharger() == null) {
            throw new IllegalArgumentException("A charger must be selected.");
        }

        validateNonNegativeFinite(
                session.getPricePerKWh(), "Price per kWh");

        String reserveSql =
                "UPDATE chargers SET is_available = FALSE " +
                "WHERE charger_id = ? AND is_available = TRUE";

        String insertSql =
                "INSERT INTO charging_sessions " +
                "(session_id, charger_id, energy_kwh, price_per_kwh, session_status) " +
                "VALUES (?, ?, 0, ?, 'ACTIVE')";

        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);

                try (PreparedStatement reserveStatement =
                             connection.prepareStatement(reserveSql)) {

                    reserveStatement.setInt(
                            1, session.getCharger().getChargerId());

                    int reserved = reserveStatement.executeUpdate();

                    if (reserved != 1) {
                        connection.rollback();
                        System.out.println(
                                "Booking failed: charger is unavailable or does not exist.");
                        return false;
                    }
                }

                try (PreparedStatement insertStatement =
                             connection.prepareStatement(insertSql)) {

                    insertStatement.setInt(1, session.getSessionId());
                    insertStatement.setInt(
                            2, session.getCharger().getChargerId());
                    insertStatement.setDouble(
                            3, session.getPricePerKWh());

                    insertStatement.executeUpdate();
                }

                connection.commit();

                System.out.println("Charging session booked successfully.");
                System.out.println("Session ID: " + session.getSessionId());
                System.out.println(
                        "Charger ID: " + session.getCharger().getChargerId());

                return true;

            } catch (SQLException | RuntimeException e) {
                rollbackSafely(connection, e);
                throw e;

            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }

        } catch (SQLException e) {
            throw new SQLException(
                    "Unable to book charging session. The session ID may already exist, or a database constraint may have been violated.",
                    e);
        }
    }

    /**
     * Completes an ACTIVE session and releases its charger.
     * Returns false if the session is not active or does not exist.
     */
    public boolean completeChargingSession(int sessionId, double energyKWh)
            throws SQLException {

        if (sessionId <= 0) {
            throw new IllegalArgumentException(
                    "Session ID must be positive.");
        }

        if (!Double.isFinite(energyKWh) || energyKWh <= 0) {
            throw new IllegalArgumentException(
                    "Energy consumed must be a finite value greater than zero.");
        }

        String findSql =
                "SELECT charger_id FROM charging_sessions " +
                "WHERE session_id = ? AND session_status = 'ACTIVE' " +
                "FOR UPDATE";

        String completeSql =
                "UPDATE charging_sessions " +
                "SET energy_kwh = ?, session_status = 'COMPLETED' " +
                "WHERE session_id = ? AND session_status = 'ACTIVE'";

        String releaseSql =
                "UPDATE chargers SET is_available = TRUE " +
                "WHERE charger_id = ?";

        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);

                int chargerId;

                try (PreparedStatement findStatement =
                             connection.prepareStatement(findSql)) {

                    findStatement.setInt(1, sessionId);

                    try (ResultSet resultSet =
                                 findStatement.executeQuery()) {

                        if (!resultSet.next()) {
                            connection.rollback();
                            System.out.println(
                                    "Completion failed: active session not found.");
                            return false;
                        }

                        chargerId = resultSet.getInt("charger_id");
                    }
                }

                try (PreparedStatement completeStatement =
                             connection.prepareStatement(completeSql)) {

                    completeStatement.setDouble(1, energyKWh);
                    completeStatement.setInt(2, sessionId);

                    if (completeStatement.executeUpdate() != 1) {
                        connection.rollback();
                        System.out.println(
                                "Completion failed: session status changed.");
                        return false;
                    }
                }

                try (PreparedStatement releaseStatement =
                             connection.prepareStatement(releaseSql)) {

                    releaseStatement.setInt(1, chargerId);

                    if (releaseStatement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Charger could not be released. Transaction cancelled.");
                    }
                }

                connection.commit();

                System.out.println(
                        "Charging completed. Charger released!");
                return true;

            } catch (SQLException | RuntimeException e) {
                rollbackSafely(connection, e);
                throw e;

            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }

        } catch (SQLException e) {
            throw new SQLException(
                    "Unable to complete charging session " + sessionId + ".", e);
        }
    }

    /**
     * Retrieves the stored price per kWh for a session.
     */
    public double getSessionPrice(int sessionId) throws SQLException {
        String sql =
                "SELECT price_per_kwh FROM charging_sessions " +
                "WHERE session_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, sessionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("price_per_kwh");
                }
            }

            throw new SQLException(
                    "No charging session found with ID " + sessionId + ".");

        } catch (SQLException e) {
            throw new SQLException(
                    "Unable to retrieve price for session " + sessionId + ".", e);
        }
    }

    /**
     * Displays total revenue from completed sessions only.
     */
    public void displayRevenueReport() throws SQLException {
        String sql =
                "SELECT COUNT(*) AS total_sessions, " +
                "COALESCE(SUM(energy_kwh), 0) AS total_energy, " +
                "COALESCE(SUM(energy_kwh * price_per_kwh), 0) AS total_revenue " +
                "FROM charging_sessions WHERE session_status = 'COMPLETED'";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (resultSet.next()) {
                System.out.println("\n========== REVENUE REPORT ==========");
                System.out.println("Completed sessions: " +
                        resultSet.getInt("total_sessions"));
                System.out.printf("Total energy: %.2f kWh%n",
                        resultSet.getDouble("total_energy"));
                System.out.printf("Total revenue: Rs. %.2f%n",
                        resultSet.getDouble("total_revenue"));
                System.out.println("====================================");
            }

        } catch (SQLException e) {
            throw new SQLException("Unable to generate revenue report.", e);
        }
    }

    /**
     * Displays revenue grouped by charger.
     */
    public void displayChargerRevenueReport() throws SQLException {
        String sql =
                "SELECT c.charger_id, c.location, " +
                "COUNT(s.session_id) AS total_sessions, " +
                "COALESCE(SUM(s.energy_kwh), 0) AS total_energy, " +
                "COALESCE(SUM(s.energy_kwh * s.price_per_kwh), 0) AS total_revenue " +
                "FROM chargers c " +
                "LEFT JOIN charging_sessions s " +
                "ON c.charger_id = s.charger_id " +
                "AND s.session_status = 'COMPLETED' " +
                "GROUP BY c.charger_id, c.location " +
                "ORDER BY c.charger_id";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            System.out.println("\n========== REVENUE BY CHARGER ==========");

            boolean found = false;

            while (resultSet.next()) {
                found = true;

                System.out.println("----------------------------------------");
                System.out.println("Charger ID: " +
                        resultSet.getInt("charger_id"));
                System.out.println("Location: " +
                        resultSet.getString("location"));
                System.out.println("Completed sessions: " +
                        resultSet.getInt("total_sessions"));
                System.out.printf("Energy: %.2f kWh%n",
                        resultSet.getDouble("total_energy"));
                System.out.printf("Revenue: Rs. %.2f%n",
                        resultSet.getDouble("total_revenue"));
            }

            if (!found) {
                System.out.println("No chargers found.");
            }

            System.out.println("========================================");

        } catch (SQLException e) {
            throw new SQLException(
                    "Unable to generate revenue by charger report.", e);
        }
    }

    /**
     * Validates finite, non-negative numeric values.
     */
    private void validateNonNegativeFinite(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be a finite, non-negative value.");
        }
    }

    /**
     * Attempts rollback without hiding the original exception.
     */
    private void rollbackSafely(Connection connection, Throwable originalError) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            originalError.addSuppressed(rollbackError);
        }
    }
}
