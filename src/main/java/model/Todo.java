package model;

import java.time.LocalDate;

public class Todo {
    private int id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private boolean isCompleted;

    public Todo() { } // デフォルト

    // 登録・テスト用（IDなし）
    public Todo(String title, String description, LocalDate dueDate, boolean isCompleted) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.isCompleted = isCompleted;
    }

    // 検索結果用（IDあり）
    public Todo(int id, String title, String description, LocalDate dueDate, boolean isCompleted) {
        this(title, description, dueDate, isCompleted);
        this.id = id;
    }

    // --- getters ---
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getDueDate() { return dueDate; }
    public boolean isCompleted() { return isCompleted; }

    // --- setters ---
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setCompleted(boolean isCompleted) { this.isCompleted = isCompleted; }
}
