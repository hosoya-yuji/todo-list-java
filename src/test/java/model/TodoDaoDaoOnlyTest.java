package model;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.*;

/**
 *  テスト内容
 *   - 空一覧（0件）
 *   - 追加（期限あり）
 *   - 追加（期限なし = null）
 *   - 並び順（id 昇順）
 *   - タイトル null のとき DB 制約（NOT NULL）で失敗する
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TodoDaoDaoOnlyTest {

    @BeforeAll
    static void ensureTable() throws SQLException {
        // スキーマ（存在しなければ作る）
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
            // 外部キー等が無い前提で TRUNCATE（AUTO_INCREMENT もリセット）
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
        Todo t = new Todo("title1", "desc1", LocalDate.of(2030,1,1), false);
        assertTrue(dao.insert(t), "insert should return true");

        List<Todo> all = dao.findAll();
        assertEquals(1, all.size());
        Todo a = all.get(0);
        assertEquals("title1", a.getTitle());
        assertEquals(LocalDate.of(2030,1,1), a.getDueDate());
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
        assertTrue(dao.insert(new Todo("b", "d", LocalDate.of(2030,1,1), false)));

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
        // DB の NOT NULL 制約違反を想定（例外→catch→false を返す設計）
        assertFalse(dao.insert(new Todo(null, "desc", null, false)));
        assertEquals(0, dao.findAll().size(), "failed insert must not add row");
    }
}
