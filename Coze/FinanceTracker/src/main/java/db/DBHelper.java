package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBHelper {

    // 数据库文件存放路径（就在项目根目录下，文件名为 finance.db）
    private static final String DB_URL = "jdbc:sqlite:finance.db";

    // 获取数据库连接的方法
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // 初始化数据库：创建两张核心表（如果不存在的话）
    public static void initDatabase() {
        // 创建"日常流水表" - 记录买菜、吃饭等即时消费
        String sqlTransactions =
                "CREATE TABLE IF NOT EXISTS transactions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "date TEXT NOT NULL, " +
                        "category TEXT NOT NULL, " +
                        "amount REAL NOT NULL, " +
                        "note TEXT" +
                        ");";

        // 创建"长期物品表" - 记录洗面奶、手机等耐用品
        String sqlDurableItems =
                "CREATE TABLE IF NOT EXISTS durable_items (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, " +
                        "category TEXT NOT NULL, " +
                        "purchase_date TEXT NOT NULL, " +
                        "total_price REAL NOT NULL, " +
                        "end_date TEXT, " +
                        "daily_usage REAL, " +
                        "remaining_qty REAL, " +
                        "note TEXT" +
                        ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            // 执行建表语句
            stmt.execute(sqlTransactions);
            stmt.execute(sqlDurableItems);
            System.out.println("✅ 数据库初始化成功！表已创建。");
            System.out.println("📁 数据库文件位置：" + System.getProperty("user.dir") + "/finance.db");
        } catch (SQLException e) {
            System.out.println("❌ 数据库初始化失败：");
            e.printStackTrace();
        }
    }

    // 测试入口：运行这个 main 方法，检查数据库是否能正常建立
    public static void main(String[] args) {
        initDatabase();
    }
}