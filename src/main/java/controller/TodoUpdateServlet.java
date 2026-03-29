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

@WebServlet("/update")
public class TodoUpdateServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");

        String idStr = req.getParameter("id");
        String title = req.getParameter("title");
        String description = req.getParameter("description");
        String dueStr = req.getParameter("dueDate");
        String completed = req.getParameter("isCompleted");

        String normalizedTitle = (title == null) ? null : title.trim();
        StringBuilder errAll = new StringBuilder();
        String errTitle = null;
        String errDue = null;

        if (idStr == null || idStr.isBlank()) {
            errAll.append("IDが不正です。");
        }

        if (normalizedTitle == null || normalizedTitle.isEmpty()) {
            errTitle = "タイトルは必須です。";
        } else if (normalizedTitle.length() > 100) {
            errTitle = "タイトルは100文字以内で入力してください。";
        }

        // dueDate is optional. Validate only when provided.
        LocalDate due = null;
        if (dueStr != null && !dueStr.isBlank()) {
            try {
                due = LocalDate.parse(dueStr);
                if (due.isBefore(LocalDate.now())) {
                    errDue = "期限日は今日以降を指定してください。";
                }
            } catch (java.time.format.DateTimeParseException e) {
                errDue = "期限日の形式が不正です。yyyy-MM-dd 形式で入力してください。";
            }
        }

        if (errTitle != null) {
            if (errAll.length() > 0) {
                errAll.append("<br>");
            }
            errAll.append(errTitle);
        }
        if (errDue != null) {
            if (errAll.length() > 0) {
                errAll.append("<br>");
            }
            errAll.append(errDue);
        }

        boolean isCompleted = (completed != null);

        if (errAll.length() > 0) {
            req.setAttribute("error", errAll.toString());
            req.setAttribute("errorTitle", errTitle);
            req.setAttribute("errorDue", errDue);

            req.setAttribute("id", idStr);
            req.setAttribute("title", title);
            req.setAttribute("description", description);
            req.setAttribute("dueDate", dueStr);
            req.setAttribute("isCompleted", isCompleted);
            req.setAttribute("isEdit", true);

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            req.getRequestDispatcher("/form.jsp").forward(req, resp);
            return;
        }

        int id = Integer.parseInt(idStr);
        Todo t = new Todo(id, normalizedTitle, description, due, isCompleted);
        boolean ok = new TodoDao().update(t);

        if (ok) {
            resp.sendRedirect(req.getContextPath() + "/list");
        } else {
            req.setAttribute("error", "更新に失敗しました。もう一度お試しください。");
            req.setAttribute("id", idStr);
            req.setAttribute("title", title);
            req.setAttribute("description", description);
            req.setAttribute("dueDate", dueStr);
            req.setAttribute("isCompleted", isCompleted);
            req.setAttribute("isEdit", true);
            req.getRequestDispatcher("/form.jsp").forward(req, resp);
        }
    }
}
