package ch.ksrminecraft.akzuwoextension.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Simplified RankPoints API implementation using a connection pool.
 */
public class PointsAPI {

    private final Logger logger;
    private final boolean debug;
    private final HikariDataSource dataSource;

    public PointsAPI(String url, String user, String password, Logger logger, boolean debug) {
        this.logger = logger;
        this.debug = debug;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        this.dataSource = new HikariDataSource(config);
        ensureTables();
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void ensureTables() {
        try (Connection con = getConnection();
             Statement st = con.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS points (UUID VARCHAR(36) PRIMARY KEY,points INT NOT NULL DEFAULT 0)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS stafflist (UUID VARCHAR(36) PRIMARY KEY,name VARCHAR(50) NOT NULL)");
            if (debug) {
                logger.info("[RankPointsAPI] Table 'points' checked/created.");
                logger.info("[RankPointsAPI] Table 'stafflist' checked/created.");
            }
        } catch (SQLException e) {
            logger.severe("[RankPointsAPI] Could not ensure tables: " + e.getMessage());
        }
    }

    private boolean isStaff(UUID uuid) {
        String sql = "SELECT UUID FROM stafflist WHERE UUID = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.warning("[RankPointsAPI] Failed to check staff status: " + e.getMessage());
            return false;
        }
    }

    public void setPoints(UUID uuid, int points) {
        if (isStaff(uuid)) {
            if (debug) {
                logger.info("[RankPointsAPI] Attempt to modify staff member " + uuid);
            }
            return;
        }
        String sql = "INSERT INTO points (UUID, points) VALUES (?, ?) ON DUPLICATE KEY UPDATE points=?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, points);
            ps.setInt(3, points);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("[RankPointsAPI] Failed to set points: " + e.getMessage());
        }
    }

    public boolean addPoints(UUID uuid, int delta) {
        if (isStaff(uuid)) {
            if (debug) {
                logger.info("[RankPointsAPI] Attempt to modify staff member " + uuid);
            }
            return false;
        }
        String ensureSql = "INSERT INTO points (UUID, points) VALUES (?, 0) ON DUPLICATE KEY UPDATE UUID=UUID";
        String updateSql = "UPDATE points SET points = points + ? WHERE UUID=?";
        try (Connection con = getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(ensureSql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setInt(1, delta);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            logger.warning("[RankPointsAPI] Failed to add points: " + e.getMessage());
            return false;
        }
    }

    public int getPoints(UUID uuid) {
        String ensureSql = "INSERT INTO points (UUID, points) VALUES (?, 0) ON DUPLICATE KEY UPDATE UUID=UUID";
        String selectSql = "SELECT points FROM points WHERE UUID=?";
        try (Connection con = getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(ensureSql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(selectSql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("points");
                    }
                }
            }
        } catch (SQLException e) {
            logger.warning("[RankPointsAPI] Failed to get points: " + e.getMessage());
        }
        return 0;
    }

    public void close() throws SQLException {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
