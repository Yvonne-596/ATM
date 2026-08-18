package dao;

import db.DBHelper;
import entity.DurableItem;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DurableItemDAO {

    /**
     * 新增一件长期物品
     */
    public void add(DurableItem item) {
        String sql = "INSERT INTO durable_items (name, category, purchase_date, total_price, note) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getCategory());
            pstmt.setString(3, item.getPurchaseDate().toString());
            pstmt.setDouble(4, item.getTotalPrice());
            pstmt.setString(5, item.getNote());
            pstmt.executeUpdate();
            System.out.println("✅ 添加成功：" + item.getName());
        } catch (SQLException e) {
            System.out.println("❌ 添加失败：" + e.getMessage());
        }
    }

    /**
     * 查询所有长期物品（按ID正序）
     */
    public List<DurableItem> findAll() {
        List<DurableItem> list = new ArrayList<>();
        String sql = "SELECT * FROM durable_items ORDER BY id";
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                DurableItem item = new DurableItem();
                item.setId(rs.getInt("id"));
                item.setName(rs.getString("name"));
                item.setCategory(rs.getString("category"));
                item.setPurchaseDate(LocalDate.parse(rs.getString("purchase_date")));
                item.setTotalPrice(rs.getDouble("total_price"));
                String endDateStr = rs.getString("end_date");
                if (endDateStr != null) {
                    item.setEndDate(LocalDate.parse(endDateStr));
                }
                item.setNote(rs.getString("note"));
                list.add(item);
            }
        } catch (SQLException e) {
            System.out.println("❌ 查询失败：" + e.getMessage());
        }
        return list;
    }

    /**
     * 根据ID查找单件物品
     */
    public DurableItem findById(int id) {
        String sql = "SELECT * FROM durable_items WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                DurableItem item = new DurableItem();
                item.setId(rs.getInt("id"));
                item.setName(rs.getString("name"));
                item.setCategory(rs.getString("category"));
                item.setPurchaseDate(LocalDate.parse(rs.getString("purchase_date")));
                item.setTotalPrice(rs.getDouble("total_price"));
                String endDateStr = rs.getString("end_date");
                if (endDateStr != null) {
                    item.setEndDate(LocalDate.parse(endDateStr));
                }
                item.setNote(rs.getString("note"));
                return item;
            }
        } catch (SQLException e) {
            System.out.println("❌ 查询失败：" + e.getMessage());
        }
        return null;
    }

    /**
     * 将物品标记为已用完（更新 end_date）
     */
    public void markAsUsedUp(int id) {
        String sql = "UPDATE durable_items SET end_date = ? WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, LocalDate.now().toString());
            pstmt.setInt(2, id);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ 已标记为用完！");
            } else {
                System.out.println("⚠️ 未找到该物品");
            }
        } catch (SQLException e) {
            System.out.println("❌ 更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除物品
     */
    public void delete(int id) {
        String sql = "DELETE FROM durable_items WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ 已删除！");
            } else {
                System.out.println("⚠️ 未找到该物品");
            }
        } catch (SQLException e) {
            System.out.println("❌ 删除失败：" + e.getMessage());
        }
    }
    /**
     * 更新物品信息（名称、品类、购买日期、总价、备注）
     */
    public void update(DurableItem item) {
        String sql = "UPDATE durable_items SET name = ?, category = ?, purchase_date = ?, total_price = ?, note = ? WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getCategory());
            pstmt.setString(3, item.getPurchaseDate().toString());
            pstmt.setDouble(4, item.getTotalPrice());
            pstmt.setString(5, item.getNote());
            pstmt.setInt(6, item.getId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ 更新成功：" + item.getName());
            } else {
                System.out.println("⚠️ 未找到该物品，更新失败");
            }
        } catch (SQLException e) {
            System.out.println("❌ 更新失败：" + e.getMessage());
        }
    }
}