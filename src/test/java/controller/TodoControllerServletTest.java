package controller;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.DBUtil;
import model.Todo;
import model.TodoDao;

public class TodoControllerServletTest {

    @BeforeAll
    static void ensureTable() throws SQLException {
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

    @BeforeEach
    void clean() throws SQLException {
        try (Connection c = DBUtil.getConnection();
             Statement st = c.createStatement()) {
            st.execute("TRUNCATE TABLE todos");
        }
    }

    @Test
    void add_doGet_shouldForwardForm() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();

        new TodoAddServlet().doGet(req.proxy, resp.proxy);

        assertEquals("/form.jsp", req.forwardedPath);
        assertNull(resp.redirectLocation);
    }

    @Test
    void add_doPost_valid_shouldInsertAndRedirect() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.contextPath = "/todo";
        req.params.put("title", "  add-title  ");
        req.params.put("description", "desc");
        req.params.put("dueDate", futureDate());
        req.params.put("isCompleted", "on");

        new TodoAddServlet().doPost(req.proxy, resp.proxy);

        assertEquals("/todo/list", resp.redirectLocation);
        assertNull(req.forwardedPath);

        List<Todo> all = new TodoDao().findAll();
        assertEquals(1, all.size());
        assertEquals("add-title", all.get(0).getTitle());
        assertTrue(all.get(0).isCompleted());
    }

    @Test
    void add_doPost_invalid_shouldReturn400AndForward() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("title", " ");
        req.params.put("description", "desc");
        req.params.put("dueDate", "");

        new TodoAddServlet().doPost(req.proxy, resp.proxy);

        assertEquals(Integer.valueOf(HttpServletResponse.SC_BAD_REQUEST), resp.status);
        assertEquals("/form.jsp", req.forwardedPath);
        assertNotNull(req.attributes.get("error"));
        assertNotNull(req.attributes.get("errorTitle"));
        assertNull(req.attributes.get("errorDue"));
    }

    @Test
    void add_doPost_nullTitle_shouldReturn400AndForward() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("description", "desc");
        req.params.put("dueDate", futureDate());

        new TodoAddServlet().doPost(req.proxy, resp.proxy);

        assertEquals(Integer.valueOf(HttpServletResponse.SC_BAD_REQUEST), resp.status);
        assertEquals("/form.jsp", req.forwardedPath);
        assertNotNull(req.attributes.get("errorTitle"));
    }

    @Test
    void add_doPost_nullDueDate_shouldInsertAndRedirect() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.contextPath = "/todo";
        req.params.put("title", "ok");
        req.params.put("description", "desc");

        new TodoAddServlet().doPost(req.proxy, resp.proxy);

        assertEquals("/todo/list", resp.redirectLocation);
        assertNull(req.forwardedPath);

        List<Todo> all = new TodoDao().findAll();
        assertEquals(1, all.size());
        assertNull(all.get(0).getDueDate());
    }

    @Test
    void add_doPost_titleTooLong_shouldReturn400AndForward() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("title", "x".repeat(101));
        req.params.put("description", "desc");
        req.params.put("dueDate", futureDate());

        new TodoAddServlet().doPost(req.proxy, resp.proxy);

        assertEquals(Integer.valueOf(HttpServletResponse.SC_BAD_REQUEST), resp.status);
        assertEquals("/form.jsp", req.forwardedPath);
        assertNotNull(req.attributes.get("errorTitle"));
    }

    @Test
    void add_doPost_dueFormatError_shouldReturn400AndForward() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("title", "ok");
        req.params.put("description", "desc");
        req.params.put("dueDate", "bad-date");

        new TodoAddServlet().doPost(req.proxy, resp.proxy);

        assertEquals(Integer.valueOf(HttpServletResponse.SC_BAD_REQUEST), resp.status);
        assertEquals("/form.jsp", req.forwardedPath);
        assertNotNull(req.attributes.get("errorDue"));
    }

    @Test
    void add_doPost_titleAndDueError_shouldJoinMessages() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("title", " ");
        req.params.put("description", "desc");
        req.params.put("dueDate", "bad-date");

        new TodoAddServlet().doPost(req.proxy, resp.proxy);

        assertEquals(Integer.valueOf(HttpServletResponse.SC_BAD_REQUEST), resp.status);
        assertEquals("/form.jsp", req.forwardedPath);
        assertNotNull(req.attributes.get("errorTitle"));
        assertNotNull(req.attributes.get("errorDue"));
        assertTrue(((String) req.attributes.get("error")).contains("<br>"));
    }

    @Test
    void add_doPost_pastDate_shouldReturn400AndForward() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("title", "ok");
        req.params.put("description", "desc");
        req.params.put("dueDate", pastDate());

        new TodoAddServlet().doPost(req.proxy, resp.proxy);

        assertEquals(Integer.valueOf(HttpServletResponse.SC_BAD_REQUEST), resp.status);
        assertEquals("/form.jsp", req.forwardedPath);
        assertNotNull(req.attributes.get("errorDue"));
    }

    @Test
    void add_doPost_dbError_shouldForwardWithError() throws Exception {
        withBrokenDbUrl(() -> {
            MockRequest req = new MockRequest();
            MockResponse resp = new MockResponse();
            req.params.put("title", "ok");
            req.params.put("description", "desc");
            req.params.put("dueDate", futureDate());

            new TodoAddServlet().doPost(req.proxy, resp.proxy);

            assertNull(resp.redirectLocation);
            assertEquals("/form.jsp", req.forwardedPath);
            assertNotNull(req.attributes.get("error"));
        });
    }

    @Test
    void delete_doPost_validId_shouldDeleteAndRedirect() throws Exception {
        TodoDao dao = new TodoDao();
        Todo t = new Todo("delete-target", "desc", LocalDate.now().plusDays(1), false);
        assertTrue(dao.insert(t));

        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.contextPath = "/todo";
        req.params.put("id", String.valueOf(t.getId()));

        new TodoDeleteServlet().doPost(req.proxy, resp.proxy);

        assertEquals("/todo/list", resp.redirectLocation);
        assertNull(dao.findById(t.getId()));
    }

    @Test
    void delete_doPost_invalidId_shouldStillRedirect() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.contextPath = "/todo";
        req.params.put("id", "abc");

        new TodoDeleteServlet().doPost(req.proxy, resp.proxy);

        assertEquals("/todo/list", resp.redirectLocation);
    }

    @Test
    void delete_doPost_blankId_shouldStillRedirect() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.contextPath = "/todo";
        req.params.put("id", " ");

        new TodoDeleteServlet().doPost(req.proxy, resp.proxy);

        assertEquals("/todo/list", resp.redirectLocation);
    }

    @Test
    void delete_doPost_nullId_shouldStillRedirect() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.contextPath = "/todo";

        new TodoDeleteServlet().doPost(req.proxy, resp.proxy);

        assertEquals("/todo/list", resp.redirectLocation);
    }

    @Test
    void form_doGet_noId_shouldForwardFormOnly() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();

        new TodoFormServlet().doGet(req.proxy, resp.proxy);

        assertEquals("/form.jsp", req.forwardedPath);
        assertNull(req.attributes.get("isEdit"));
    }

    @Test
    void form_doGet_blankId_shouldForwardFormOnly() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("id", " ");

        new TodoFormServlet().doGet(req.proxy, resp.proxy);

        assertEquals("/form.jsp", req.forwardedPath);
        assertNull(req.attributes.get("isEdit"));
    }

    @Test
    void form_doGet_existingTodo_withDueDate_shouldSetAttributes() throws Exception {
        TodoDao dao = new TodoDao();
        Todo t = new Todo("form-target", "desc", LocalDate.now().plusDays(1), true);
        assertTrue(dao.insert(t));

        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("id", String.valueOf(t.getId()));

        new TodoFormServlet().doGet(req.proxy, resp.proxy);

        assertEquals("/form.jsp", req.forwardedPath);
        assertEquals(t.getId(), req.attributes.get("id"));
        assertEquals("form-target", req.attributes.get("title"));
        assertEquals("desc", req.attributes.get("description"));
        assertEquals(t.getDueDate().toString(), req.attributes.get("dueDate"));
        assertEquals(Boolean.TRUE, req.attributes.get("isCompleted"));
        assertEquals(Boolean.TRUE, req.attributes.get("isEdit"));
    }

    @Test
    void form_doGet_existingTodo_withoutDueDate_shouldNotSetDueDate() throws Exception {
        TodoDao dao = new TodoDao();
        Todo t = new Todo("form-null-date", "desc", null, false);
        assertTrue(dao.insert(t));

        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("id", String.valueOf(t.getId()));

        new TodoFormServlet().doGet(req.proxy, resp.proxy);

        assertEquals("/form.jsp", req.forwardedPath);
        assertEquals("form-null-date", req.attributes.get("title"));
        assertNull(req.attributes.get("dueDate"));
    }

    @Test
    void form_doGet_invalidId_shouldForwardFormOnly() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("id", "bad-id");

        new TodoFormServlet().doGet(req.proxy, resp.proxy);

        assertEquals("/form.jsp", req.forwardedPath);
        assertNull(req.attributes.get("isEdit"));
    }

    @Test
    void form_doGet_nonExistingNumericId_shouldForwardFormOnly() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("id", "999999");

        new TodoFormServlet().doGet(req.proxy, resp.proxy);

        assertEquals("/form.jsp", req.forwardedPath);
        assertNull(req.attributes.get("isEdit"));
    }

    @Test
    void list_doGet_shouldSetTodosAndForward() throws Exception {
        TodoDao dao = new TodoDao();
        assertTrue(dao.insert(new Todo("a", "d", null, false)));
        assertTrue(dao.insert(new Todo("b", "d", LocalDate.now().plusDays(1), true)));

        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();

        new TodoListServlet().doGet(req.proxy, resp.proxy);

        assertEquals("/index.jsp", req.forwardedPath);
        Object todosAttr = req.attributes.get("todos");
        assertTrue(todosAttr instanceof List<?>);
        assertEquals(2, ((List<?>) todosAttr).size());
    }

    @Test
    void update_doPost_valid_shouldUpdateAndRedirect() throws Exception {
        TodoDao dao = new TodoDao();
        Todo original = new Todo("before", "old", LocalDate.now().plusDays(1), false);
        assertTrue(dao.insert(original));

        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.contextPath = "/todo";
        req.params.put("id", String.valueOf(original.getId()));
        req.params.put("title", "  updated-title ");
        req.params.put("description", "new");
        req.params.put("dueDate", LocalDate.now().plusDays(2).toString());
        req.params.put("isCompleted", "on");

        new TodoUpdateServlet().doPost(req.proxy, resp.proxy);

        assertEquals("/todo/list", resp.redirectLocation);
        Todo actual = dao.findById(original.getId());
        assertNotNull(actual);
        assertEquals("updated-title", actual.getTitle());
        assertTrue(actual.isCompleted());
    }

    @Test
    void update_doPost_validationError_shouldReturn400AndForward() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("id", "");
        req.params.put("title", "");
        req.params.put("description", "x");
        req.params.put("dueDate", "");

        new TodoUpdateServlet().doPost(req.proxy, resp.proxy);

        assertEquals(Integer.valueOf(HttpServletResponse.SC_BAD_REQUEST), resp.status);
        assertEquals("/form.jsp", req.forwardedPath);
        assertNotNull(req.attributes.get("error"));
        assertNotNull(req.attributes.get("errorTitle"));
        assertNull(req.attributes.get("errorDue"));
        assertEquals(Boolean.TRUE, req.attributes.get("isEdit"));
    }

    @Test
    void update_doPost_nullTitle_shouldReturn400AndForward() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("id", "1");
        req.params.put("description", "x");
        req.params.put("dueDate", futureDate());

        new TodoUpdateServlet().doPost(req.proxy, resp.proxy);

        assertEquals(Integer.valueOf(HttpServletResponse.SC_BAD_REQUEST), resp.status);
        assertEquals("/form.jsp", req.forwardedPath);
        assertNotNull(req.attributes.get("errorTitle"));
    }

    @Test
    void update_doPost_nullDueDate_shouldUpdateAndRedirect() throws Exception {
        TodoDao dao = new TodoDao();
        Todo original = new Todo("before", "old", LocalDate.now().plusDays(1), false);
        assertTrue(dao.insert(original));

        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.contextPath = "/todo";
        req.params.put("id", String.valueOf(original.getId()));
        req.params.put("title", "ok");
        req.params.put("description", "x");

        new TodoUpdateServlet().doPost(req.proxy, resp.proxy);

        assertEquals("/todo/list", resp.redirectLocation);
        Todo actual = dao.findById(original.getId());
        assertNotNull(actual);
        assertNull(actual.getDueDate());
    }

    @Test
    void update_doPost_titleTooLong_shouldReturn400AndForward() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("id", "1");
        req.params.put("title", "x".repeat(101));
        req.params.put("description", "x");
        req.params.put("dueDate", futureDate());

        new TodoUpdateServlet().doPost(req.proxy, resp.proxy);

        assertEquals(Integer.valueOf(HttpServletResponse.SC_BAD_REQUEST), resp.status);
        assertEquals("/form.jsp", req.forwardedPath);
        assertNotNull(req.attributes.get("errorTitle"));
    }

    @Test
    void update_doPost_nullId_shouldReturn400AndForward() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("title", "ok");
        req.params.put("description", "x");
        req.params.put("dueDate", futureDate());

        new TodoUpdateServlet().doPost(req.proxy, resp.proxy);

        assertEquals(Integer.valueOf(HttpServletResponse.SC_BAD_REQUEST), resp.status);
        assertEquals("/form.jsp", req.forwardedPath);
        assertNotNull(req.attributes.get("error"));
    }

    @Test
    void update_doPost_dueFormatError_shouldReturn400AndForward() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("id", "1");
        req.params.put("title", "ok");
        req.params.put("description", "x");
        req.params.put("dueDate", "bad-date");

        new TodoUpdateServlet().doPost(req.proxy, resp.proxy);

        assertEquals(Integer.valueOf(HttpServletResponse.SC_BAD_REQUEST), resp.status);
        assertEquals("/form.jsp", req.forwardedPath);
        assertNotNull(req.attributes.get("errorDue"));
    }

    @Test
    void update_doPost_titleAndDueError_shouldJoinMessages() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("id", "1");
        req.params.put("title", " ");
        req.params.put("description", "x");
        req.params.put("dueDate", "bad-date");

        new TodoUpdateServlet().doPost(req.proxy, resp.proxy);

        assertEquals(Integer.valueOf(HttpServletResponse.SC_BAD_REQUEST), resp.status);
        assertEquals("/form.jsp", req.forwardedPath);
        assertNotNull(req.attributes.get("errorTitle"));
        assertNotNull(req.attributes.get("errorDue"));
        assertTrue(((String) req.attributes.get("error")).contains("<br>"));
    }

    @Test
    void update_doPost_pastDate_shouldReturn400AndForward() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("id", "1");
        req.params.put("title", "ok");
        req.params.put("description", "x");
        req.params.put("dueDate", pastDate());

        new TodoUpdateServlet().doPost(req.proxy, resp.proxy);

        assertEquals(Integer.valueOf(HttpServletResponse.SC_BAD_REQUEST), resp.status);
        assertEquals("/form.jsp", req.forwardedPath);
        assertNotNull(req.attributes.get("errorDue"));
    }

    @Test
    void update_doPost_nonExistingId_shouldForwardWithError() throws Exception {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("id", "999999");
        req.params.put("title", "ok");
        req.params.put("description", "x");
        req.params.put("dueDate", futureDate());

        new TodoUpdateServlet().doPost(req.proxy, resp.proxy);

        assertNull(resp.redirectLocation);
        assertEquals("/form.jsp", req.forwardedPath);
        assertNotNull(req.attributes.get("error"));
    }

    @Test
    void update_doPost_dbError_shouldForwardWithError() throws Exception {
        withBrokenDbUrl(() -> {
            MockRequest req = new MockRequest();
            MockResponse resp = new MockResponse();
            req.params.put("id", "1");
            req.params.put("title", "ok");
            req.params.put("description", "x");
            req.params.put("dueDate", futureDate());

            new TodoUpdateServlet().doPost(req.proxy, resp.proxy);

            assertNull(resp.redirectLocation);
            assertEquals("/form.jsp", req.forwardedPath);
            assertNotNull(req.attributes.get("error"));
        });
    }

    @Test
    void update_doPost_nonNumericId_withValidFields_shouldThrow() {
        MockRequest req = new MockRequest();
        MockResponse resp = new MockResponse();
        req.params.put("id", "abc");
        req.params.put("title", "ok");
        req.params.put("description", "x");
        req.params.put("dueDate", futureDate());

        assertThrows(NumberFormatException.class, () -> new TodoUpdateServlet().doPost(req.proxy, resp.proxy));
    }

    private static void withBrokenDbUrl(ThrowingRunnable action) throws Exception {
        String prev = System.getProperty("DB_URL");
        System.setProperty("DB_URL", "jdbc:invalid://broken");
        try {
            action.run();
        } finally {
            if (prev == null) {
                System.clearProperty("DB_URL");
            } else {
                System.setProperty("DB_URL", prev);
            }
        }
    }

    private static String futureDate() {
        return LocalDate.now().plusDays(1).toString();
    }

    private static String pastDate() {
        return LocalDate.now().minusDays(1).toString();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class MockRequest {
        final Map<String, String> params = new HashMap<>();
        final Map<String, Object> attributes = new HashMap<>();
        final HttpServletRequest proxy;
        String contextPath = "";
        String characterEncoding;
        String forwardedPath;

        MockRequest() {
            this.proxy = (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (self, method, args) -> handle(self, method.getName(), method.getReturnType(), args));
        }

        private Object handle(Object self, String name, Class<?> returnType, Object[] args)
                throws ServletException, IOException {
            if ("getParameter".equals(name)) {
                return params.get((String) args[0]);
            }
            if ("setAttribute".equals(name)) {
                attributes.put((String) args[0], args[1]);
                return null;
            }
            if ("getAttribute".equals(name)) {
                return attributes.get((String) args[0]);
            }
            if ("removeAttribute".equals(name)) {
                attributes.remove((String) args[0]);
                return null;
            }
            if ("setCharacterEncoding".equals(name)) {
                characterEncoding = (String) args[0];
                return null;
            }
            if ("getCharacterEncoding".equals(name)) {
                return characterEncoding;
            }
            if ("getContextPath".equals(name)) {
                return contextPath;
            }
            if ("getRequestDispatcher".equals(name)) {
                String path = (String) args[0];
                return Proxy.newProxyInstance(
                        RequestDispatcher.class.getClassLoader(),
                        new Class<?>[]{RequestDispatcher.class},
                        (dSelf, dMethod, dArgs) -> {
                            String dName = dMethod.getName();
                            if ("forward".equals(dName)) {
                                forwardedPath = path;
                                return null;
                            }
                            if ("include".equals(dName)) {
                                return null;
                            }
                            if ("toString".equals(dName)) {
                                return "MockRequestDispatcher(" + path + ")";
                            }
                            if ("hashCode".equals(dName)) {
                                return System.identityHashCode(dSelf);
                            }
                            if ("equals".equals(dName)) {
                                return dSelf == dArgs[0];
                            }
                            return defaultValue(dMethod.getReturnType());
                        });
            }
            if ("toString".equals(name)) {
                return "MockHttpServletRequest";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(self);
            }
            if ("equals".equals(name)) {
                return self == args[0];
            }
            return defaultValue(returnType);
        }
    }

    private static final class MockResponse {
        final HttpServletResponse proxy;
        Integer status;
        String redirectLocation;

        MockResponse() {
            this.proxy = (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (self, method, args) -> handle(self, method.getName(), method.getReturnType(), args));
        }

        private Object handle(Object self, String name, Class<?> returnType, Object[] args) {
            if ("setStatus".equals(name)) {
                status = (Integer) args[0];
                return null;
            }
            if ("getStatus".equals(name)) {
                return status == null ? 0 : status;
            }
            if ("sendRedirect".equals(name)) {
                redirectLocation = (String) args[0];
                return null;
            }
            if ("toString".equals(name)) {
                return "MockHttpServletResponse";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(self);
            }
            if ("equals".equals(name)) {
                return self == args[0];
            }
            return defaultValue(returnType);
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
