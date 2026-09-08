package com.example.todo;

import static com.example.todo.Task.Status.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    properties = {
      "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:boardtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
      "spring.datasource.username=${TEST_DB_USER:sa}",
      "spring.datasource.password=${TEST_DB_PASS:}"
    })
@AutoConfigureMockMvc
class BoardIntegrationTest {
  @Autowired BoardService service;
  @Autowired JdbcTemplate db;
  @Autowired MockMvc mvc;

  @BeforeEach
  void clean() {
    db.update("UPDATE todos SET parent_id=NULL");
    db.update("DELETE FROM todos");
    db.update("UPDATE board_revision SET revision=0");
  }

  Task add(String title, Task.Status status, Long parent) {
    return service
        .save(
            null,
            new BoardService.Input(title, "", null, status, parent, service.board().revision()))
        .tasks()
        .stream()
        .filter(t -> t.title().equals(title))
        .findFirst()
        .orElseThrow();
  }

  Task get(long id) {
    return service.board().tasks().stream().filter(t -> t.id() == id).findFirst().orElseThrow();
  }

  void move(long id, Task.Status status) {
    service.move(id, new BoardService.Move(status, null, service.board().revision()));
  }

  @Test
  void parentCompletionRequiresAllChildrenAndDoesNotAutoComplete() {
    Task parent = add("親", OPEN, null), child = add("子", OPEN, parent.id());
    assertThrows(ResponseStatusException.class, () -> move(parent.id(), DONE));
    assertEquals(OPEN, get(parent.id()).status());
    move(child.id(), RESOLVED);
    assertThrows(ResponseStatusException.class, () -> move(parent.id(), DONE));
    move(child.id(), DONE);
    assertEquals(OPEN, get(parent.id()).status());
    move(parent.id(), DONE);
    assertEquals(DONE, get(parent.id()).status());
  }

  @Test
  void detailUpdateCannotBypassCompletionRule() {
    Task parent = add("親", OPEN, null);
    add("子", OPEN, parent.id());
    assertThrows(
        ResponseStatusException.class,
        () ->
            service.save(
                parent.id(),
                new BoardService.Input("親", "", null, DONE, null, service.board().revision())));
  }

  @Test
  void reopeningChildReopensParent() {
    Task parent = add("親", DONE, null), child = add("子", DONE, parent.id());
    move(child.id(), OPEN);
    assertEquals(IN_PROGRESS, get(parent.id()).status());
  }

  @Test
  void addingOrReparentingIncompleteChildReopensParent() {
    Task parent = add("親", DONE, null);
    add("子", OPEN, parent.id());
    assertEquals(IN_PROGRESS, get(parent.id()).status());
    Task second = add("親2", DONE, null), child = add("独立課題", RESOLVED, null);
    service.save(
        child.id(),
        new BoardService.Input(
            child.title(), "", null, RESOLVED, second.id(), service.board().revision()));
    assertEquals(IN_PROGRESS, get(second.id()).status());
  }

  @Test
  void rejectCyclesThirdLevelAndDeletingParent() {
    Task parent = add("親", OPEN, null), child = add("子", OPEN, parent.id());
    assertThrows(ResponseStatusException.class, () -> add("孫", OPEN, child.id()));
    assertThrows(
        ResponseStatusException.class,
        () ->
            service.save(
                parent.id(),
                new BoardService.Input(
                    "親", "", null, OPEN, parent.id(), service.board().revision())));
    assertThrows(
        ResponseStatusException.class,
        () ->
            service.save(
                parent.id(),
                new BoardService.Input(
                    "親", "", null, OPEN, child.id(), service.board().revision())));
    Task root = add("別の親", OPEN, null);
    assertThrows(
        ResponseStatusException.class,
        () ->
            service.save(
                parent.id(),
                new BoardService.Input(
                    "親", "", null, OPEN, root.id(), service.board().revision())));
    assertThrows(
        ResponseStatusException.class,
        () -> service.delete(parent.id(), service.board().revision()));
    service.save(
        child.id(), new BoardService.Input("子", "", null, OPEN, null, service.board().revision()));
    service.delete(parent.id(), service.board().revision());
    assertNull(get(child.id()).parentId());
  }

  @Test
  void reorderAndCrossColumnMovePersist() {
    Task a = add("A", OPEN, null), b = add("B", OPEN, null), c = add("C", IN_PROGRESS, null);
    service.move(b.id(), new BoardService.Move(OPEN, a.id(), service.board().revision()));
    assertEquals(
        List.of(b.id(), a.id()),
        service.board().tasks().stream().filter(t -> t.status() == OPEN).map(Task::id).toList());
    service.move(b.id(), new BoardService.Move(IN_PROGRESS, c.id(), service.board().revision()));
    assertEquals(
        List.of(b.id(), c.id()),
        service.board().tasks().stream()
            .filter(t -> t.status() == IN_PROGRESS)
            .map(Task::id)
            .toList());
  }

  @Test
  void invalidDestinationRollsBack() {
    Task a = add("A", OPEN, null), b = add("B", DONE, null);
    long revision = service.board().revision();
    assertThrows(
        ResponseStatusException.class,
        () -> service.move(a.id(), new BoardService.Move(OPEN, b.id(), revision)));
    assertEquals(revision, service.board().revision());
    assertEquals(OPEN, get(a.id()).status());
  }

  @Test
  void staleRevisionCannotOverwrite() {
    Task t = add("A", OPEN, null);
    long old = service.board().revision();
    move(t.id(), DONE);
    assertThrows(
        ResponseStatusException.class,
        () -> service.move(t.id(), new BoardService.Move(OPEN, null, old)));
    assertEquals(DONE, get(t.id()).status());
  }

  @Test
  void simultaneousUpdatesHaveOneWinner() throws Exception {
    Task t = add("A", OPEN, null);
    long revision = service.board().revision();
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      CountDownLatch start = new CountDownLatch(1);
      Callable<Boolean> job =
          () -> {
            start.await();
            try {
              service.move(t.id(), new BoardService.Move(DONE, null, revision));
              return true;
            } catch (ResponseStatusException e) {
              return false;
            }
          };
      Future<Boolean> a = pool.submit(job), b = pool.submit(job);
      start.countDown();
      assertNotEquals(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void validateTitleAndPreserveExistingOverdueTasks() {
    assertThrows(ResponseStatusException.class, () -> add("   ", OPEN, null));
    assertThrows(ResponseStatusException.class, () -> add("a".repeat(101), OPEN, null));
    assertThrows(
        ResponseStatusException.class,
        () ->
            service.save(
                null,
                new BoardService.Input(
                    "過去",
                    "",
                    LocalDate.now().minusDays(1),
                    OPEN,
                    null,
                    service.board().revision())));
    Task t = add("期限切れ", OPEN, null);
    LocalDate past = LocalDate.now().minusDays(3);
    db.update("UPDATE todos SET due_date=? WHERE id=?", past, t.id());
    service.save(
        t.id(), new BoardService.Input("編集済み", "", past, DONE, null, service.board().revision()));
    assertEquals(DONE, get(t.id()).status());
  }

  @Test
  void pageAndApiReturnUsableResponses() throws Exception {
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Taskboard")));
    mvc.perform(
            post("/api/tasks")
                .contentType("application/json")
                .content("{\"title\":\"API課題\",\"status\":\"OPEN\",\"revision\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasks[0].title").value("API課題"));
    mvc.perform(
            post("/api/tasks")
                .contentType("application/json")
                .content("{\"title\":\"不正\",\"status\":\"INVALID\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
    mvc.perform(
            post("/api/tasks")
                .contentType("application/json")
                .content("{\"title\":\"競合\",\"status\":\"OPEN\",\"revision\":0}"))
        .andExpect(status().isConflict());
  }
}
