package com.example.common.db;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Creates the service's PostgreSQL database (e.g. hometutor_auth) the first time the
 * service starts, so nobody has to create databases by hand in pgAdmin. Tables are then
 * created by Flyway migrations. Enabled with app.database.auto-create=true.
 */
public class DatabaseAutoCreator implements EnvironmentPostProcessor, Ordered {
    private static final Pattern SAFE_DB_NAME = Pattern.compile("[A-Za-z0-9_]+");
    private static final String PREFIX = "jdbc:postgresql://";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("app.database.auto-create", Boolean.class, false)) {
            return;
        }
        String url = environment.getProperty("spring.datasource.url");
        if (url == null || !url.startsWith(PREFIX)) {
            return;
        }
        String rest = url.substring(PREFIX.length());
        int slash = rest.indexOf('/');
        if (slash < 0) {
            return;
        }
        String hostAndPort = rest.substring(0, slash);
        String pathAndQuery = rest.substring(slash + 1);
        int question = pathAndQuery.indexOf('?');
        String databaseName = question < 0 ? pathAndQuery : pathAndQuery.substring(0, question);
        String query = question < 0 ? "" : pathAndQuery.substring(question);
        if (!SAFE_DB_NAME.matcher(databaseName).matches()) {
            return;
        }

        String adminUrl = PREFIX + hostAndPort + "/postgres" + query;
        Properties credentials = new Properties();
        credentials.setProperty("user", environment.getProperty("spring.datasource.username", "postgres"));
        credentials.setProperty("password", environment.getProperty("spring.datasource.password", ""));
        credentials.setProperty("connectTimeout", "10");

        try {
            Driver driver = (Driver) Class.forName("org.postgresql.Driver", true,
                    Thread.currentThread().getContextClassLoader()).getDeclaredConstructor().newInstance();
            try (Connection connection = driver.connect(adminUrl, credentials)) {
                if (connection == null) {
                    return;
                }
                if (!databaseExists(connection, databaseName)) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("CREATE DATABASE \"" + databaseName + "\"");
                    }
                    System.out.println("[database] Created PostgreSQL database '" + databaseName + "' on " + hostAndPort);
                }
            }
        } catch (SQLException exception) {
            System.err.println("[database] Could not check/create database '" + databaseName + "' on " + hostAndPort
                    + ": " + exception.getMessage());
            System.err.println("[database] Make sure PostgreSQL is running and DB_USERNAME / DB_PASSWORD in the .env file are correct.");
        } catch (ReflectiveOperationException exception) {
            System.err.println("[database] PostgreSQL JDBC driver not found: " + exception.getMessage());
        }
    }

    private static boolean databaseExists(Connection connection, String databaseName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
            statement.setString(1, databaseName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    @Override
    public int getOrder() {
        // Run after application.yml and the .env file have been loaded.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
