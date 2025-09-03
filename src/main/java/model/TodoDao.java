package model;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//DB接続
public class TodoDao {
	public List<Todo> findAll() {
		List<Todo> list = new ArrayList<>();
		String sql = "SELECT * FROM todos";

		try (Connection conn = DBUtil.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				Todo todo = new Todo();
				todo.setId(rs.getInt("id"));
				todo.setTitle(rs.getString("title"));
				todo.setDescription(rs.getString("description"));
				todo.setDueDate(rs.getDate("due_date").toLocalDate());
				todo.setCompleted(rs.getBoolean("is_completed"));
				list.add(todo);
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
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, todo.getTitle());
			ps.setString(2, todo.getDescription());
			ps.setDate(3, Date.valueOf(todo.getDueDate()));
			ps.setBoolean(4, todo.isCompleted());

			int rows = ps.executeUpdate();
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
			ps.setDate(3, Date.valueOf(todo.getDueDate()));
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

}
