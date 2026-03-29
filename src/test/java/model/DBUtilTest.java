package model;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DBUtilTest {

    @Test
    void normalizeMysqlUrl_shouldAppendMissingParams() throws Exception {
        String raw = "jdbc:mysql://localhost:3306/tododb";
        String normalized = invokePrivateStringMethod(
                "normalizeMysqlUrl",
                new Class<?>[]{String.class},
                raw);

        assertTrue(normalized.contains("allowPublicKeyRetrieval=true"));
        assertTrue(normalized.contains("serverTimezone=UTC"));
        assertTrue(normalized.contains("sslMode=DISABLED"));
    }

    @Test
    void normalizeMysqlUrl_shouldKeepExistingSettings() throws Exception {
        String raw = "jdbc:mysql://localhost:3306/tododb?allowPublicKeyRetrieval=true&serverTimezone=UTC&useSSL=false";
        String normalized = invokePrivateStringMethod(
                "normalizeMysqlUrl",
                new Class<?>[]{String.class},
                raw);

        assertEquals(raw, normalized);
    }

    @Test
    void normalizeMysqlUrl_shouldKeepExistingSslMode() throws Exception {
        String raw = "jdbc:mysql://localhost:3306/tododb?allowPublicKeyRetrieval=true&serverTimezone=UTC&sslMode=DISABLED";
        String normalized = invokePrivateStringMethod(
                "normalizeMysqlUrl",
                new Class<?>[]{String.class},
                raw);

        assertEquals(raw, normalized);
    }

    @Test
    void appendQueryParam_shouldUseQuestionMarkWhenNoQuery() throws Exception {
        String actual = invokePrivateStringMethod(
                "appendQueryParam",
                new Class<?>[]{String.class, String.class},
                "jdbc:mysql://localhost:3306/tododb",
                "k=v");

        assertEquals("jdbc:mysql://localhost:3306/tododb?k=v", actual);
    }

    @Test
    void appendQueryParam_shouldUseAmpersandWhenQueryExists() throws Exception {
        String actual = invokePrivateStringMethod(
                "appendQueryParam",
                new Class<?>[]{String.class, String.class},
                "jdbc:mysql://localhost:3306/tododb?x=1",
                "k=v");

        assertEquals("jdbc:mysql://localhost:3306/tododb?x=1&k=v", actual);
    }

    @Test
    void get_shouldPreferSystemProperty() throws Exception {
        String key = "TEST_DBUTIL_KEY";
        String prev = System.getProperty(key);
        System.setProperty(key, "value-from-property");
        try {
            String actual = invokePrivateStringMethod(
                    "get",
                    new Class<?>[]{String.class, String.class},
                    key,
                    "default-value");
            assertEquals("value-from-property", actual);
        } finally {
            restoreProperty(key, prev);
        }
    }

    @Test
    void get_shouldFallbackToDefaultWhenBlankProperty() throws Exception {
        String key = "TEST_DBUTIL_KEY_BLANK";
        String prev = System.getProperty(key);
        System.setProperty(key, "   ");
        try {
            String actual = invokePrivateStringMethod(
                    "get",
                    new Class<?>[]{String.class, String.class},
                    key,
                    "default-value");
            assertEquals("default-value", actual);
        } finally {
            restoreProperty(key, prev);
        }
    }

    @Test
    void get_shouldReturnDefaultWhenPropertyAndEnvAreMissing() throws Exception {
        String key = "TEST_DBUTIL_MISSING_" + UUID.randomUUID();
        String actual = invokePrivateStringMethod(
                "get",
                new Class<?>[]{String.class, String.class},
                key,
                "default-value");
        assertEquals("default-value", actual);
    }

    @Test
    void get_shouldUseEnvironmentValueWhenPropertyMissing() throws Exception {
        Map.Entry<String, String> env = System.getenv().entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .findFirst()
                .orElseThrow();

        String key = env.getKey();
        String expected = env.getValue();
        String prev = System.getProperty(key);
        System.clearProperty(key);
        try {
            String actual = invokePrivateStringMethod(
                    "get",
                    new Class<?>[]{String.class, String.class},
                    key,
                    "default-value");
            assertEquals(expected, actual);
        } finally {
            restoreProperty(key, prev);
        }
    }

    @Test
    void getConnection_shouldThrowSQLExceptionWhenUrlIsInvalid() {
        String prevUrl = System.getProperty("DB_URL");
        System.setProperty("DB_URL", "jdbc:invalid://broken");
        try {
            assertThrows(SQLException.class, DBUtil::getConnection);
        } finally {
            restoreProperty("DB_URL", prevUrl);
        }
    }

    @Test
    void loadDriverClass_unknownDriver_shouldThrowIllegalStateException() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> DBUtil.loadDriverClass("no.such.DriverClass"));
        assertEquals("MySQL JDBC Driver not found", ex.getMessage());
        assertNotNull(ex.getCause());
    }

    @Test
    void blankToNull_shouldHandleNullBlankAndValue() throws Exception {
        assertNull(invokePrivateStringMethod(
                "blankToNull",
                new Class<?>[]{String.class},
                new Object[]{null}));

        assertNull(invokePrivateStringMethod(
                "blankToNull",
                new Class<?>[]{String.class},
                "   "));

        assertEquals("abc", invokePrivateStringMethod(
                "blankToNull",
                new Class<?>[]{String.class},
                "abc"));
    }

    private static String invokePrivateStringMethod(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = DBUtil.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return (String) m.invoke(null, args);
    }

    private static void restoreProperty(String key, String prev) {
        if (prev == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, prev);
        }
    }
}
