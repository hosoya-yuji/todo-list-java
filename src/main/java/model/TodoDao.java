package model;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

//DB接続
public class TodoDao {
	public List<Todo> findAll() {
	    String sql = "SELECT id, title, description, due_date, is_completed FROM todos ORDER BY id";
	    List<Todo> list = new ArrayList<>();

	    try (Connection conn = DBUtil.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {

	        while (rs.next()) {
	            java.sql.Date d = rs.getDate("due_date");
	            java.time.LocalDate due = (d != null) ? d.toLocalDate() : null;

	            list.add(new Todo(
	                rs.getInt("id"),
	                rs.getString("title"),
	                rs.getString("description"),
	                due,
	                rs.getBoolean("is_completed")
	            ));
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return list;
	}

	//DB登録
	public boolean insert(Todo todo) {
	    String sql = "INSERT INTO todos (title, description, due_date, is_completed) VALUES (?, ?, ?, ?)";

	    try (Connection conn = DBUtil.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

	        ps.setString(1, todo.getTitle());
	        ps.setString(2, todo.getDescription());

	        // ★ dueDate が null のときは setNull
	        if (todo.getDueDate() != null) {
	            ps.setDate(3, java.sql.Date.valueOf(todo.getDueDate()));
	        } else {
	            ps.setNull(3, java.sql.Types.DATE);
	        }

	        ps.setBoolean(4, todo.isCompleted());

	        int rows = ps.executeUpdate();

	        // 生成IDを戻したい場合（任意）
	        if (rows > 0) {
	            try (ResultSet rs = ps.getGeneratedKeys()) {
	                if (rs.next()) todo.setId(rs.getInt(1));
	            }
	        }
	        return rows > 0;

	    } catch (SQLException e) {
	        e.printStackTrace();
	        return false;
	    }
	}


	//DB更新
	public boolean update(Todo todo) {
		String sql = "UPDATE todos SET title = ?, description = ?, due_date = ?, is_completed = ? WHERE id = ?";

		try (Connection conn = DBUtil.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, todo.getTitle());
			ps.setString(2, todo.getDescription());
			if (todo.getDueDate() != null) {
				ps.setDate(3, Date.valueOf(todo.getDueDate()));
			} else {
				ps.setNull(3, java.sql.Types.DATE);
			}
			ps.setBoolean(4, todo.isCompleted());
			ps.setInt(5, todo.getId());

			int rows = ps.executeUpdate();
			return rows > 0;

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	//DB削除
	public boolean delete(int id) {
		String sql = "DELETE FROM todos WHERE id = ?";

		try (Connection conn = DBUtil.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, id);
			int rows = ps.executeUpdate();
			return rows > 0;

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public Todo findById(int id) {
		String sql = "SELECT id, title, description, due_date, is_completed FROM todos WHERE id = ?";

		try (Connection conn = DBUtil.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					java.sql.Date d = rs.getDate("due_date");
					java.time.LocalDate due = (d != null) ? d.toLocalDate() : null;
					return new Todo(
							rs.getInt("id"),
							rs.getString("title"),
							rs.getString("description"),
							due,
							rs.getBoolean("is_completed")
					);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

}
