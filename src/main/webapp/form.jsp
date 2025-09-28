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

 // 今日の日付文字列（yyyy-MM-dd）
 String today = java.time.LocalDate.now().toString();
%>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>TODO登録フォーム</title>
  <style>
    .error { color: red; }
    .field-error { color: red; font-size: 0.9em; margin-left: 6px; }
  </style>
</head>
<body>
  <h2>TODO登録フォーム</h2>

  <% if (error != null) { %>
    <div class="error"><%= error %></div>
  <% } %>

  <form action="<%= request.getContextPath() %>/add" method="post" novalidate>
    <div>
      タイトル：
      <input type="text" name="title" required maxlength="100"
             value="<%= titleVal != null ? titleVal : "" %>">
      <% if (errorTitle != null) { %>
        <span class="field-error"><%= errorTitle %></span>
      <% } %>
    </div>
    <br>

    <div>
      説明：<br>
      <textarea name="description" rows="3" cols="30"><%= descVal != null ? descVal : "" %></textarea>
    </div>
    <br>

    <div>
      期限：
      <input type="date" name="dueDate" required
             min="<%= today %>"
             value="<%= dueVal != null ? dueVal : "" %>">
      <% if (errorDue != null) { %>
        <span class="field-error"><%= errorDue %></span>
      <% } %>
    </div>
    <br>

    <div>
      完了：<input type="checkbox" name="isCompleted" <%= checked ? "checked" : "" %>>
    </div>
    <br>

    <input type="submit" value="登録">
  </form>

  <br>
  <a href="<%= request.getContextPath() %>/list">一覧に戻る</a>
</body>
</html>
