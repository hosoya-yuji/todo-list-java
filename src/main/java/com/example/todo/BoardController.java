package com.example.todo;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class BoardController {
  private final BoardService service;

  public BoardController(BoardService service) {
    this.service = service;
  }

  @GetMapping({"/", "/list"})
  public String index() {
    return "board";
  }

  @GetMapping("/api/tasks")
  @ResponseBody
  public BoardService.Board list() {
    return service.board();
  }

  @PostMapping("/api/tasks")
  @ResponseBody
  public BoardService.Board add(@RequestBody BoardService.Input input) {
    return service.save(null, input);
  }

  @PutMapping("/api/tasks/{id}")
  @ResponseBody
  public BoardService.Board edit(@PathVariable long id, @RequestBody BoardService.Input input) {
    return service.save(id, input);
  }

  @PatchMapping("/api/tasks/{id}/position")
  @ResponseBody
  public BoardService.Board move(@PathVariable long id, @RequestBody BoardService.Move input) {
    return service.move(id, input);
  }

  @DeleteMapping("/api/tasks/{id}")
  @ResponseBody
  public BoardService.Board delete(@PathVariable long id, @RequestParam Long revision) {
    return service.delete(id, revision);
  }

  @ExceptionHandler(ResponseStatusException.class)
  @ResponseBody
  public ResponseEntity<?> error(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
  }

  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  @ResponseBody
  public ResponseEntity<?> invalid() {
    return ResponseEntity.badRequest().body(Map.of("message", "入力形式が不正です。日付と状態を確認してください。"));
  }
}
