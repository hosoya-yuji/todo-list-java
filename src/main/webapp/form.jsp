<%@ page contentType="text/html; charset=UTF-8" %>
<%
 String error = (String) request.getAttribute("error");
 String errorTitle = (String) request.getAttribute("errorTitle");
 String errorDue   = (String) request.getAttribute("errorDue");

 String titleVal = (String) request.getAttribute("title");
 String descVal  = (String) request.getAttribute("description");
 String dueVal   = (String) request.getAttribute("dueDate");
 Boolean checked = (Boolean) request.getAttribute("isCompleted");
 if (checked == null) checked = false;
 Boolean isEdit = (Boolean) request.getAttribute("isEdit");
 if (isEdit == null) isEdit = false;
 Object idObj = request.getAttribute("id");
 String idVal = (idObj != null) ? idObj.toString() : "";

 String today = java.time.LocalDate.now().toString();
%>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title><%= isEdit ? "Edit Task" : "New Task" %></title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/app.css">
</head>
<body>
  <div class="bg-orb orb-1"></div>
  <div class="bg-orb orb-2"></div>

  <main class="app-shell app-shell-narrow">
    <section class="panel">
      <header class="topbar">
        <div>
          <p class="eyebrow">Task Editor</p>
          <h1><%= isEdit ? "Edit Task" : "Create Task" %></h1>
        </div>
        <a class="btn btn-ghost" href="<%= request.getContextPath() %>/list">Back to List</a>
      </header>

      <% if (error != null) { %>
        <div class="alert"><%= error %></div>
      <% } %>

      <form class="todo-form" action="<%= request.getContextPath() %><%= isEdit ? "/update" : "/add" %>" method="post" novalidate>
        <% if (isEdit) { %>
          <input type="hidden" name="id" value="<%= idVal != null ? idVal : "" %>">
        <% } %>

        <label for="title">Title</label>
        <input id="title" type="text" name="title" required maxlength="100" value="<%= titleVal != null ? titleVal : "" %>">
        <% if (errorTitle != null) { %>
          <p class="field-error"><%= errorTitle %></p>
        <% } %>

        <label for="description">Description</label>
        <textarea id="description" name="description" rows="4"><%= descVal != null ? descVal : "" %></textarea>

        <label for="dueDate">Due Date</label>
        <input id="dueDate" type="date" name="dueDate" min="<%= today %>" value="<%= dueVal != null ? dueVal : "" %>">
        <% if (errorDue != null) { %>
          <p class="field-error"><%= errorDue %></p>
        <% } %>

        <% if (isEdit) { %>
          <label class="check-row" for="isCompleted">
            <input id="isCompleted" type="checkbox" name="isCompleted" <%= checked ? "checked" : "" %>>
            <span>Completed</span>
          </label>
        <% } %>

        <div class="form-actions">
          <input class="btn btn-primary" type="submit" value="<%= isEdit ? "Update" : "Create" %>">
        </div>
      </form>
    </section>
  </main>
</body>
</html>
