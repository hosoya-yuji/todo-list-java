package model;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TodoDaoDaoOnlyTest {

    private static final String STUB_URL = "jdbc:stub://todo-test";
    private static final StubJdbcDriver STUB_DRIVER = new StubJdbcDriver();

    @BeforeAll
    static void ensureTable() throws SQLException {
        DriverManager.registerDriver(STUB_DRIVER);
        try (Connection c = DBUtil.getConnection();
             Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS todos (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "title VARCHAR(100) NOT NULL," +
                    "description TEXT," +
                    "due_date DATE," +
                    "is_completed BOOLEAN NOT NULL DEFAULT FALSE," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")");
        }
    }

    @AfterAll
    static void deregisterStubDriver() throws SQLException {
        DriverManager.deregisterDriver(STUB_DRIVER);
    }

    @BeforeEach
    void clean() throws SQLException {
        try (Connection c = DBUtil.getConnection();
             Statement st = c.createStatement()) {
            st.execute("TRUNCATE TABLE todos");
        }
    }

    @Test
    @Order(1)
    void findAll_onEmpty_shouldReturnEmptyList() {
        List<Todo> all = new TodoDao().findAll();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    @Order(2)
    void insert_shouldSucceed_withDueDate() {
        TodoDao dao = new TodoDao();
        Todo t = new Todo("title1", "desc1", LocalDate.of(2030, 1, 1), false);
        assertTrue(dao.insert(t), "insert should return true");
        assertTrue(t.getId() > 0, "generated id should be set");

        List<Todo> all = dao.findAll();
        assertEquals(1, all.size());
        Todo a = all.get(0);
        assertEquals("title1", a.getTitle());
        assertEquals(LocalDate.of(2030, 1, 1), a.getDueDate());
        assertFalse(a.isCompleted());
    }

    @Test
    @Order(3)
    void insert_shouldSucceed_withNullDueDate() {
        TodoDao dao = new TodoDao();
        Todo t = new Todo("no date", "desc", null, false);
        assertTrue(dao.insert(t), "null dueDate should be accepted");

        List<Todo> all = dao.findAll();
        assertEquals(1, all.size());
        assertNull(all.get(0).getDueDate());
    }

    @Test
    @Order(4)
    void orderShouldBeByIdAsc() {
        TodoDao dao = new TodoDao();
        assertTrue(dao.insert(new Todo("a", "d", null, false)));
        assertTrue(dao.insert(new Todo("b", "d", LocalDate.of(2030, 1, 1), false)));

        List<Todo> all = dao.findAll();
        assertEquals(2, all.size());
        assertTrue(all.get(0).getId() < all.get(1).getId(), "id should be ascending");
        assertEquals("a", all.get(0).getTitle());
        assertEquals("b", all.get(1).getTitle());
    }

    @Test
    @Order(5)
    void insert_nullTitle_shouldReturnFalse() {
        TodoDao dao = new TodoDao();
        assertFalse(dao.insert(new Todo(null, "desc", null, false)));
        assertEquals(0, dao.findAll().size(), "failed insert must not add row");
    }

    @Test
    @Order(6)
    void update_shouldSucceed_andPersistChanges() {
        TodoDao dao = new TodoDao();
        Todo original = new Todo("before", "old", LocalDate.of(2030, 1, 1), false);
        assertTrue(dao.insert(original));

        Todo edited = new Todo(original.getId(), "after", "new", LocalDate.of(2031, 2, 2), true);
        assertTrue(dao.update(edited), "update should return true for existing id");

        Todo actual = dao.findById(original.getId());
        assertNotNull(actual);
        assertEquals("after", actual.getTitle());
        assertEquals("new", actual.getDescription());
        assertEquals(LocalDate.of(2031, 2, 2), actual.getDueDate());
        assertTrue(actual.isCompleted());
    }

    @Test
    @Order(7)
    void update_nonExistingId_shouldReturnFalse() {
        TodoDao dao = new TodoDao();
        Todo edited = new Todo(999999, "after", "new", LocalDate.of(2031, 2, 2), true);
        assertFalse(dao.update(edited));
    }

    @Test
    @Order(8)
    void update_nullTitle_shouldReturnFalse() {
        TodoDao dao = new TodoDao();
        Todo original = new Todo("before", "old", LocalDate.of(2030, 1, 1), false);
        assertTrue(dao.insert(original));

        Todo invalid = new Todo(original.getId(), null, "new", LocalDate.of(2031, 2, 2), true);
        assertFalse(dao.update(invalid));
    }

    @Test
    @Order(9)
    void update_shouldSucceed_withNullDueDate() {
        TodoDao dao = new TodoDao();
        Todo original = new Todo("before", "old", LocalDate.of(2030, 1, 1), false);
        assertTrue(dao.insert(original));

        Todo edited = new Todo(original.getId(), "after-null-date", "new", null, true);
        assertTrue(dao.update(edited));

        Todo actual = dao.findById(original.getId());
        assertNotNull(actual);
        assertNull(actual.getDueDate());
        assertEquals("after-null-date", actual.getTitle());
        assertTrue(actual.isCompleted());
    }

    @Test
    @Order(10)
    void delete_shouldSucceed_andRemoveRow() {
        TodoDao dao = new TodoDao();
        Todo t = new Todo("to delete", "desc", LocalDate.of(2030, 1, 1), false);
        assertTrue(dao.insert(t));

        assertTrue(dao.delete(t.getId()), "delete should return true for existing id");
        assertNull(dao.findById(t.getId()));
        assertTrue(dao.findAll().isEmpty());
    }

    @Test
    @Order(11)
    void delete_nonExistingId_shouldReturnFalse() {
        TodoDao dao = new TodoDao();
        assertFalse(dao.delete(999999));
    }

    @Test
    @Order(12)
    void findById_nonExisting_shouldReturnNull() {
        TodoDao dao = new TodoDao();
        assertNull(dao.findById(999999));
    }

    @Test
    @Order(13)
    void findById_existingWithNullDueDate_shouldReturnTodo() {
        TodoDao dao = new TodoDao();
        Todo t = new Todo("null date target", "desc", null, false);
        assertTrue(dao.insert(t));

        Todo actual = dao.findById(t.getId());
        assertNotNull(actual);
        assertNull(actual.getDueDate());
        assertEquals("null date target", actual.getTitle());
    }

    @Test
    @Order(14)
    void insert_onDbError_shouldReturnFalse() {
        String prev = System.getProperty("DB_URL");
        System.setProperty("DB_URL", "jdbc:invalid://broken");
        try {
            Todo t = new Todo("x", "y", LocalDate.of(2030, 1, 1), false);
            assertFalse(new TodoDao().insert(t));
        } finally {
            restoreDbUrl(prev);
        }
    }

    @Test
    @Order(15)
    void findAll_onDbError_shouldReturnEmptyList() {
        String prev = System.getProperty("DB_URL");
        System.setProperty("DB_URL", "jdbc:invalid://broken");
        try {
            List<Todo> all = new TodoDao().findAll();
            assertNotNull(all);
            assertTrue(all.isEmpty());
        } finally {
            restoreDbUrl(prev);
        }
    }

    @Test
    @Order(16)
    void update_onDbError_shouldReturnFalse() {
        String prev = System.getProperty("DB_URL");
        System.setProperty("DB_URL", "jdbc:invalid://broken");
        try {
            Todo t = new Todo(1, "x", "y", LocalDate.of(2030, 1, 1), false);
            assertFalse(new TodoDao().update(t));
        } finally {
            restoreDbUrl(prev);
        }
    }

    @Test
    @Order(17)
    void delete_onDbError_shouldReturnFalse() {
        String prev = System.getProperty("DB_URL");
        System.setProperty("DB_URL", "jdbc:invalid://broken");
        try {
            assertFalse(new TodoDao().delete(1));
        } finally {
            restoreDbUrl(prev);
        }
    }

    @Test
    @Order(18)
    void findById_onDbError_shouldReturnNull() {
        String prev = System.getProperty("DB_URL");
        System.setProperty("DB_URL", "jdbc:invalid://broken");
        try {
            assertNull(new TodoDao().findById(1));
        } finally {
            restoreDbUrl(prev);
        }
    }

    @Test
    @Order(19)
    void insert_stubDriver_noGeneratedKeys_shouldReturnTrue_andKeepIdZero() {
        String prev = System.getProperty("DB_URL");
        System.setProperty("DB_URL", STUB_URL);
        STUB_DRIVER.mode = StubMode.INSERT_ONE_NO_KEYS;
        try {
            Todo t = new Todo("stub-no-keys", "desc", null, false);
            assertTrue(new TodoDao().insert(t));
            assertEquals(0, t.getId());
        } finally {
            restoreDbUrl(prev);
            STUB_DRIVER.mode = StubMode.INSERT_ONE_NO_KEYS;
        }
    }

    @Test
    @Order(20)
    void insert_stubDriver_zeroRows_shouldReturnFalse() {
        String prev = System.getProperty("DB_URL");
        System.setProperty("DB_URL", STUB_URL);
        STUB_DRIVER.mode = StubMode.INSERT_ZERO_ROWS;
        try {
            Todo t = new Todo("stub-zero-rows", "desc", null, false);
            assertFalse(new TodoDao().insert(t));
            assertEquals(0, t.getId());
        } finally {
            restoreDbUrl(prev);
            STUB_DRIVER.mode = StubMode.INSERT_ONE_NO_KEYS;
        }
    }

    @Test
    @Order(21)
    void findById_stubDriver_prepareStatementThrows_shouldReturnNull() {
        String prev = System.getProperty("DB_URL");
        System.setProperty("DB_URL", STUB_URL);
        STUB_DRIVER.mode = StubMode.PREPARE_THROWS_SQL_EXCEPTION;
        try {
            assertNull(new TodoDao().findById(1));
        } finally {
            restoreDbUrl(prev);
            STUB_DRIVER.mode = StubMode.INSERT_ONE_NO_KEYS;
        }
    }

    private void restoreDbUrl(String prev) {
        if (prev == null) {
            System.clearProperty("DB_URL");
        } else {
            System.setProperty("DB_URL", prev);
        }
    }

    private enum StubMode {
        INSERT_ONE_NO_KEYS,
        INSERT_ZERO_ROWS,
        PREPARE_THROWS_SQL_EXCEPTION
    }

    private static final class StubJdbcDriver implements Driver {
        private volatile StubMode mode = StubMode.INSERT_ONE_NO_KEYS;

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) {
                return null;
            }
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (self, method, args) -> {
                        String name = method.getName();
                        if ("prepareStatement".equals(name)) {
                            if (mode == StubMode.PREPARE_THROWS_SQL_EXCEPTION) {
                                throw new SQLException("stub prepare failure");
                            }
                            return createPreparedStatementProxy();
                        }
                        if ("close".equals(name)) {
                            return null;
                        }
                        if ("isClosed".equals(name)) {
                            return false;
                        }
                        if ("isWrapperFor".equals(name)) {
                            return false;
                        }
                        if ("unwrap".equals(name)) {
                            throw new SQLException("not a wrapper");
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private PreparedStatement createPreparedStatementProxy() {
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (self, method, args) -> {
                        String name = method.getName();
                        if ("executeUpdate".equals(name)) {
                            return mode == StubMode.INSERT_ZERO_ROWS ? 0 : 1;
                        }
                        if ("getGeneratedKeys".equals(name)) {
                            return createGeneratedKeysResultSetProxy();
                        }
                        if ("close".equals(name)) {
                            return null;
                        }
                        if ("isWrapperFor".equals(name)) {
                            return false;
                        }
                        if ("unwrap".equals(name)) {
                            throw new SQLException("not a wrapper");
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private ResultSet createGeneratedKeysResultSetProxy() {
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (self, method, args) -> {
                        String name = method.getName();
                        if ("next".equals(name)) {
                            return false;
                        }
                        if ("close".equals(name)) {
                            return null;
                        }
                        if ("isWrapperFor".equals(name)) {
                            return false;
                        }
                        if ("unwrap".equals(name)) {
                            throw new SQLException("not a wrapper");
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:stub://");
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}
