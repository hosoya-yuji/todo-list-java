package controller;

import java.io.IOException;
import java.time.LocalDate;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Todo;
import model.TodoDao;

@WebServlet("/add")
public class TodoAddServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/form.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");

        String title       = req.getParameter("title");
        String description = req.getParameter("description");
        String dueStr      = req.getParameter("dueDate");
        String completed   = req.getParameter("isCompleted"); // チェック時 "on"

        String normalizedTitle = (title == null) ? null : title.trim();
        StringBuilder errAll = new StringBuilder();
        String errTitle = null;
        String errDue   = null;

        // タイトル：必須・空白のみ不可・100文字以内
        if (normalizedTitle == null || normalizedTitle.isEmpty()) {
            errTitle = "タイトルは必須です。";
        } else if (normalizedTitle.length() > 100) {
            errTitle = "タイトルは100文字以内で入力してください。";
        }

        // 期限：必須＋過去日不可（今日以降のみ可）
        LocalDate due = null;
        if (dueStr == null || dueStr.isBlank()) {
            errDue = "期限は必須です。";
        } else {
            try {
                due = LocalDate.parse(dueStr);
                if (due.isBefore(LocalDate.now())) {
                    errDue = "期限に過去日は指定できません（本日以降）。";
                }
            } catch (java.time.format.DateTimeParseException e) {
                errDue = "期限の日付形式が不正です（yyyy-MM-dd）。";
            }
        }

        if (errTitle != null) {
            errAll.append(errTitle);
        }
        if (errDue != null) {
            if (errAll.length() > 0) errAll.append("<br>");
            errAll.append(errDue);
        }

        boolean isCompleted = (completed != null);

        if (errAll.length() > 0) {
            // エラー時：入力値と個別エラーを戻す
            req.setAttribute("error", errAll.toString());
            req.setAttribute("errorTitle", errTitle);
            req.setAttribute("errorDue", errDue);

            req.setAttribute("title", title);
            req.setAttribute("description", description);
            req.setAttribute("dueDate", dueStr);
            req.setAttribute("isCompleted", isCompleted);

            // 任意：HTTP 400 にしておく
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            req.getRequestDispatcher("/form.jsp").forward(req, resp);
            return;
        }

        // 正常登録
        Todo t = new Todo(normalizedTitle, description, due, isCompleted);
        boolean ok = new TodoDao().insert(t);

        if (ok) {
            resp.sendRedirect(req.getContextPath() + "/list");
        } else {
            req.setAttribute("error", "登録に失敗しました。しばらくしてからお試しください。");
            req.setAttribute("title", title);
            req.setAttribute("description", description);
            req.setAttribute("dueDate", dueStr);
            req.setAttribute("isCompleted", isCompleted);
            req.getRequestDispatcher("/form.jsp").forward(req, resp);
        }
    }
}
