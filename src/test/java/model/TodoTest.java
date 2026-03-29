package model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class TodoTest {

    @Test
    void noArgsConstructorAndSetters_shouldPopulateFields() {
        Todo todo = new Todo();
        LocalDate due = LocalDate.of(2030, 1, 1);

        todo.setId(10);
        todo.setTitle("title");
        todo.setDescription("description");
        todo.setDueDate(due);
        todo.setCompleted(true);

        assertEquals(10, todo.getId());
        assertEquals("title", todo.getTitle());
        assertEquals("description", todo.getDescription());
        assertEquals(due, todo.getDueDate());
        assertTrue(todo.isCompleted());
    }

    @Test
    void argsConstructorWithoutId_shouldSetFieldsAndDefaultIdZero() {
        LocalDate due = LocalDate.of(2031, 2, 2);
        Todo todo = new Todo("title", "description", due, false);

        assertEquals(0, todo.getId());
        assertEquals("title", todo.getTitle());
        assertEquals("description", todo.getDescription());
        assertEquals(due, todo.getDueDate());
        assertFalse(todo.isCompleted());
    }

    @Test
    void argsConstructorWithId_shouldSetAllFields() {
        LocalDate due = LocalDate.of(2032, 3, 3);
        Todo todo = new Todo(99, "title2", "description2", due, true);

        assertEquals(99, todo.getId());
        assertEquals("title2", todo.getTitle());
        assertEquals("description2", todo.getDescription());
        assertEquals(due, todo.getDueDate());
        assertTrue(todo.isCompleted());
    }
}
