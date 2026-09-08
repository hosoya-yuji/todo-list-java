package com.example.todo;

import java.time.LocalDate;

public record Task(
    long id,
    String title,
    String description,
    LocalDate dueDate,
    Status status,
    Long parentId,
    long sortOrder,
    long version) {
  public enum Status {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    DONE
  }
}
