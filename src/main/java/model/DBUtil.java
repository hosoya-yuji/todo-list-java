package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {

    // 既存の値をデフォルトとして残す（通常実行時はこちらが使われる）
    private static final String DEFAULT_URL  =
        "jdbc:mysql://localhost:3306/tododb?useSSL=false&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "jagabata5";

    // まず System.getProperty を見て、無ければ環境変数 → 最後にデフォルト
    private static String get(String key, String def) {
        String v = System.getProperty(key);              // 例: -DDB_URL=...
        if (v == null || v.isBlank()) v = System.getenv(key); // 例: 環境変数 DB_URL
        return (v == null || v.isBlank()) ? def : v;
    }
    private static String url()  { return get("DB_URL",  DEFAULT_URL); }
    private static String user() { return get("DB_USER", DEFAULT_USER); }
    private static String pass() { return get("DB_PASS", DEFAULT_PASS); }

    // ドライバは一度だけロード（JDBC 4 以降は不要なことが多いが、明示ロードで安定）
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // ここで失敗する場合は mysql-connector-j-*.jar がクラスパスに無い
            throw new IllegalStateException("MySQL JDBC Driver not found", e);
            // ※落としたくない場合は上の行をコメントアウトし、代わりに e.printStackTrace(); にする
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url(), user(), pass());
    }
}
