package com.example.todo;

import static com.example.todo.Task.Status.*;

import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BoardService {
  private final JdbcTemplate db;

  public BoardService(JdbcTemplate db) {
    this.db = db;
  }

  public record Board(long revision, List<Task> tasks) {}

  public record Input(
      String title,
      String description,
      LocalDate dueDate,
      Task.Status status,
      Long parentId,
      Long revision) {}

  public record Move(Task.Status status, Long beforeId, Long revision) {}

  private ResponseStatusException bad(String text) {
    return new ResponseStatusException(HttpStatus.CONFLICT, text);
  }

  private List<Task> tasks() {
    return db.query(
        "SELECT * FROM todos ORDER BY sort_order,id",
        (r, n) ->
            new Task(
                r.getLong("id"),
                r.getString("title"),
                r.getString("description"),
                r.getObject("due_date", LocalDate.class),
                Task.Status.valueOf(r.getString("status")),
                r.getObject("parent_id") == null ? null : r.getLong("parent_id"),
                r.getLong("sort_order"),
                r.getLong("version")));
  }

  // Serialize board writes so hierarchy checks and updates share one transaction.
  private long lock() {
    return db.queryForObject(
        "SELECT revision FROM board_revision WHERE id=1 FOR UPDATE", Long.class);
  }

  private void check(Long expected, long actual) {
    if (expected == null || expected != actual) throw bad("別の画面で更新されました。最新の内容を確認してやり直してください。");
  }

  private void bump() {
    db.update("UPDATE board_revision SET revision=revision+1 WHERE id=1");
  }

  @Transactional
  public Board board() {
    long rev = lock();
    return new Board(rev, tasks());
  }

  private Task find(List<Task> all, long id) {
    return all.stream()
        .filter(t -> t.id() == id)
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "課題が見つかりません。"));
  }

  private void complete(List<Task> all, long id, Task.Status status) {
    if (status == DONE
        && all.stream().anyMatch(t -> Objects.equals(t.parentId(), id) && t.status() != DONE))
      throw bad("未完了の子課題があります。すべての子課題を完了してください。");
  }

  private void reopenParent(Long parent, Task.Status status) {
    if (parent != null && status != DONE)
      db.update(
          "UPDATE todos SET status='IN_PROGRESS',version=version+1 WHERE id=? AND status='DONE'",
          parent);
  }

  @Transactional
  public Board save(Long id, Input in) {
    check(in.revision(), lock());
    var all = tasks();
    Task old = id == null ? null : find(all, id);
    String title = in.title() == null ? "" : in.title().trim();
    if (title.isEmpty() || title.length() > 100) throw bad("タイトルは1〜100文字で入力してください。");
    if (in.description() != null && in.description().length() > 10000)
      throw bad("詳細は10000文字以内で入力してください。");
    if (in.status() == null) throw bad("状態を指定してください。");
    if (in.dueDate() != null
        && in.dueDate().isBefore(LocalDate.now())
        && (old == null || !Objects.equals(old.dueDate(), in.dueDate())))
      throw bad("新しく設定する期限日は今日以降を指定してください。");
    if (in.parentId() != null) {
      Task parent = find(all, in.parentId());
      if (Objects.equals(id, in.parentId())
          || parent.parentId() != null
          || id != null && all.stream().anyMatch(t -> Objects.equals(t.parentId(), id)))
        throw bad("親子課題は2階層までです。自分自身を親にはできません。");
    }
    if (id != null) complete(all, id, in.status());
    long order = all.stream().mapToLong(Task::sortOrder).max().orElse(0) + 1;
    if (old == null)
      db.update(
          "INSERT INTO todos(title,description,due_date,status,parent_id,sort_order,version)"
              + " VALUES(?,?,?,?,?,?,0)",
          title,
          in.description(),
          in.dueDate(),
          in.status().name(),
          in.parentId(),
          order);
    else
      db.update(
          "UPDATE todos SET"
              + " title=?,description=?,due_date=?,status=?,parent_id=?,sort_order=?,version=version+1"
              + " WHERE id=?",
          title,
          in.description(),
          in.dueDate(),
          in.status().name(),
          in.parentId(),
          old.status() == in.status() ? old.sortOrder() : order,
          id);
    reopenParent(in.parentId(), in.status());
    bump();
    return board();
  }

  @Transactional
  public Board move(long id, Move in) {
    check(in.revision(), lock());
    var all = tasks();
    Task task = find(all, id);
    if (in.status() == null) throw bad("状態を指定してください。");
    complete(all, id, in.status());
    var column =
        new ArrayList<>(
            all.stream().filter(t -> t.status() == in.status() && t.id() != id).toList());
    int index = column.size();
    if (in.beforeId() != null) {
      index = -1;
      for (int i = 0; i < column.size(); i++) if (column.get(i).id() == in.beforeId()) index = i;
      if (index < 0) throw bad("移動先が変わりました。再度操作してください。");
    }
    column.add(index, task);
    for (int i = 0; i < column.size(); i++)
      db.update(
          "UPDATE todos SET status=?,sort_order=?,version=version+1 WHERE id=?",
          in.status().name(),
          i,
          column.get(i).id());
    reopenParent(task.parentId(), in.status());
    bump();
    return board();
  }

  @Transactional
  public Board delete(long id, Long revision) {
    check(revision, lock());
    var all = tasks();
    find(all, id);
    if (all.stream().anyMatch(t -> Objects.equals(t.parentId(), id)))
      throw bad("子課題があるため削除できません。子課題を削除するか、親との関連を解除してください。");
    db.update("DELETE FROM todos WHERE id=?", id);
    bump();
    return board();
  }
}
