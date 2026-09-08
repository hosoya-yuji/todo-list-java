package com.example.todo;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class MigrationTest {
  @Test
  void migrateLegacyDataWithoutLosingFields() throws Exception {
    String url = "jdbc:h2:mem:legacy;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    try (Connection c = DriverManager.getConnection(url, "sa", "");
        Statement s = c.createStatement()) {
      s.execute(
          "CREATE TABLE todos(id INT AUTO_INCREMENT PRIMARY KEY,title VARCHAR(100) NOT"
              + " NULL,description TEXT,due_date DATE,is_completed BOOLEAN)");
      s.execute(
          "INSERT INTO todos VALUES(7,'既存課題','説明','2025-01-01',TRUE),(8,'未完了','内容',NULL,FALSE)");
    }
    Flyway.configure()
        .dataSource(url, "sa", "")
        .baselineOnMigrate(true)
        .baselineVersion("1")
        .load()
        .migrate();
    try (Connection c = DriverManager.getConnection(url, "sa", "");
        Statement s = c.createStatement();
        ResultSet r = s.executeQuery("SELECT * FROM todos ORDER BY id")) {
      assertTrue(r.next());
      assertEquals(7, r.getLong("id"));
      assertEquals("既存課題", r.getString("title"));
      assertEquals("説明", r.getString("description"));
      assertEquals("2025-01-01", r.getString("due_date"));
      assertEquals("DONE", r.getString("status"));
      assertNull(r.getObject("parent_id"));
      assertTrue(r.next());
      assertEquals("OPEN", r.getString("status"));
      assertNull(r.getDate("due_date"));
    }
  }
}
