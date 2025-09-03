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
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String title       = request.getParameter("title");
        String description = request.getParameter("description");
        String due         = request.getParameter("dueDate");
        String completed   = request.getParameter("isCompleted"); // チェックされていれば "on"

        Todo t = new Todo();
        t.setTitle(title);
        t.setDescription(description);
        t.setDueDate(due == null || due.isBlank() ? null : LocalDate.parse(due));
        t.setCompleted(completed != null);

        new TodoDao().insert(t);

        // Jakarta/ Tomcat10でも同じ
        response.sendRedirect(request.getContextPath() + "/list");
    }
}
