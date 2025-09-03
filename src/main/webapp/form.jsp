<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>TODO登録フォーム</title>
</head>
<body>
    <h2>TODO登録フォーム</h2>
    <form action="add" method="post">
        タイトル：<input type="text" name="title" required><br><br>
        説明：<textarea name="description" rows="3" cols="30"></textarea><br><br>
        期限：<input type="date" name="dueDate" required><br><br>
        完了：<input type="checkbox" name="isCompleted"><br><br>
        <input type="submit" value="登録">
    </form>
    <br>
    <a href="list">一覧に戻る</a>
</body>
</html>
