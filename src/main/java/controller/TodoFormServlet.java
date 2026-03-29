package controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Todo;
import model.TodoDao;

@WebServlet("/form")
public class TodoFormServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idStr = request.getParameter("id");
        if (idStr != null && !idStr.isBlank()) {
            try {
                int id = Integer.parseInt(idStr);
                Todo todo = new TodoDao().findById(id);
                if (todo != null) {
                    request.setAttribute("id", todo.getId());
                    request.setAttribute("title", todo.getTitle());
                    request.setAttribute("description", todo.getDescription());
                    if (todo.getDueDate() != null) {
                        request.setAttribute("dueDate", todo.getDueDate().toString());
                    }
                    request.setAttribute("isCompleted", todo.isCompleted());
                    request.setAttribute("isEdit", true);
                }
            } catch (NumberFormatException e) {
                // ignore invalid id
            }
        }
        request.getRequestDispatcher("/form.jsp").forward(request, response);
    }
}
