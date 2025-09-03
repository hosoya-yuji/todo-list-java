<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page import="model.Todo,java.util.List"%>
<%
List<Todo> todos = (List<Todo>) request.getAttribute("todos");
if (todos == null) {
	// 直接 /index.jsp を開かれたら、一覧サーブレットへ回す
	response.sendRedirect(request.getContextPath() + "/list");
	return;
}
%>
<html>
<head>
<title>TODO一覧</title>
</head>
<body>
	<h2>TODO一覧</h2>
	<table border="1">
		<tr>
			<th>ID</th>
			<th>タイトル</th>
			<th>説明</th>
			<th>期限</th>
			<th>完了</th>
		</tr>
		<%
		for (Todo t : todos) {
		%>
		<tr>
			<td><%=t.getId()%></td>
			<td><%=t.getTitle()%></td>
			<td><%=t.getDescription()%></td>
			<td><%=t.getDueDate()%></td>
			<td><%=t.isCompleted() ? "✔" : ""%></td>
		</tr>
		<%
		}
		%>
	</table>
	<p>
		<a href="<%=request.getContextPath()%>/form">新規登録</a>
	</p>
</body>
</html>
