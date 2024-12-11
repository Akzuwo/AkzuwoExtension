package ch.ksrminecraft.akzuwoextension.utils;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private String url;
    private String username;
    private String password;
    private Connection connection;

    public DatabaseManager(String host, int port, String database, String username, String password) {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false";
        this.username = username;
        this.password = password;
    }

    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) return;
        connection = DriverManager.getConnection(url, username, password);
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) connect();
        return connection;
    }

    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
