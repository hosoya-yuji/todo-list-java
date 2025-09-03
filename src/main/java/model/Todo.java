package model;

import java.time.LocalDate;

public class Todo {
	private int id;
	private String title;
	private String description;
	private LocalDate dueDate;
	private boolean isCompleted;

	public Todo(int id, String title, String description, LocalDate dueDate, boolean isCompleted) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.dueDate = dueDate;
		this.isCompleted = isCompleted;
	}

	public Todo() {
		// TODO 自動生成されたコンストラクター・スタブ
	}

	// Getter
	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public boolean isCompleted() {
		return isCompleted;
	}

	// Setter
	public void setId(int id) {
		this.id = id;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public void setCompleted(boolean isCompleted) {
		this.isCompleted = isCompleted;
	}
}
