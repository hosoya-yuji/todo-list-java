<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="model.Todo,java.util.List" %>
<%
List<Todo> todos = (List<Todo>) request.getAttribute("todos");
if (todos == null) {
    response.sendRedirect(request.getContextPath() + "/list");
    return;
}

int completedCount = 0;
for (Todo t : todos) {
    if (t.isCompleted()) {
        completedCount++;
    }
}
int openCount = todos.size() - completedCount;
%>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Todo List</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/app.css">
</head>
<body>
    <div class="bg-orb orb-1"></div>
    <div class="bg-orb orb-2"></div>

    <main class="app-shell">
        <section class="panel">
            <header class="topbar">
                <div>
                    <p class="eyebrow">Task Manager</p>
                    <h1>Todo List</h1>
                </div>
                <a class="btn btn-primary" href="<%=request.getContextPath()%>/form">+ Add Task</a>
            </header>

            <div class="stats-row">
                <div class="stat-card">
                    <span class="stat-label">Total</span>
                    <strong class="stat-value"><%= todos.size() %></strong>
                </div>
                <div class="stat-card">
                    <span class="stat-label">Open</span>
                    <strong class="stat-value"><%= openCount %></strong>
                </div>
                <div class="stat-card">
                    <span class="stat-label">Done</span>
                    <strong class="stat-value"><%= completedCount %></strong>
                </div>
            </div>

            <div class="table-wrap">
                <table class="todo-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Title</th>
                            <th>Description</th>
                            <th>Due Date</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                    <% if (todos.isEmpty()) { %>
                        <tr>
                            <td colspan="6" class="empty-cell">No tasks yet. Add your first task.</td>
                        </tr>
                    <% } else { %>
                        <% for (Todo t : todos) { %>
                        <tr>
                            <td>#<%= t.getId() %></td>
                            <td class="title-cell"><%= t.getTitle() %></td>
                            <td><%= t.getDescription() == null ? "" : t.getDescription() %></td>
                            <td><%= t.getDueDate() %></td>
                            <td>
                                <span class="<%= t.isCompleted() ? "badge badge-done" : "badge badge-open" %>">
                                    <%= t.isCompleted() ? "Done" : "Open" %>
                                </span>
                            </td>
                            <td>
                                <div class="actions">
                                    <a class="btn btn-ghost" href="<%=request.getContextPath()%>/form?id=<%=t.getId()%>">Edit</a>
                                    <form action="<%=request.getContextPath()%>/delete" method="post" class="inline-form">
                                        <input type="hidden" name="id" value="<%=t.getId()%>">
                                        <input class="btn btn-danger" type="submit" value="Delete" onclick="return confirm('Delete this item?');">
                                    </form>
                                </div>
                            </td>
                        </tr>
                        <% } %>
                    <% } %>
                    </tbody>
                </table>
            </div>
        </section>
    </main>
</body>
</html>
