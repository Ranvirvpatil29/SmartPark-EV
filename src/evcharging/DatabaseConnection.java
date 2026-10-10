
package evcharging;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/smartpark_ev";

    private static final String USER =
            System.getenv().getOrDefault("SMARTPARK_DB_USER", "root");

    private static final String PASSWORD =
            System.getenv().getOrDefault("SMARTPARK_DB_PASSWORD", "");

    private DatabaseConnection() {
        // Prevent object creation.
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
