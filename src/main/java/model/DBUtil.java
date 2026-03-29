package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/tododb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "Jagabata5";

    private static String get(String key, String def) {
        String value = blankToNull(System.getProperty(key));
        if (value == null) {
            value = blankToNull(System.getenv(key));
        }
        return value == null ? def : value;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static String url() {
        return normalizeMysqlUrl(get("DB_URL", DEFAULT_URL));
    }

    private static String user() {
        return get("DB_USER", DEFAULT_USER);
    }

    private static String pass() {
        return get("DB_PASS", DEFAULT_PASS);
    }

    private static String normalizeMysqlUrl(String rawUrl) {
        String url = rawUrl;
        if (!url.contains("allowPublicKeyRetrieval=")) {
            url = appendQueryParam(url, "allowPublicKeyRetrieval=true");
        }
        if (!url.contains("serverTimezone=")) {
            url = appendQueryParam(url, "serverTimezone=UTC");
        }
        if (!url.contains("sslMode=") && !url.contains("useSSL=")) {
            url = appendQueryParam(url, "sslMode=DISABLED");
        }
        return url;
    }

    private static String appendQueryParam(String url, String param) {
        return url + (url.contains("?") ? "&" : "?") + param;
    }

    static {
        loadDriverClass("com.mysql.cj.jdbc.Driver");
    }

    static void loadDriverClass(String driverClassName) {
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC Driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url(), user(), pass());
    }
}
