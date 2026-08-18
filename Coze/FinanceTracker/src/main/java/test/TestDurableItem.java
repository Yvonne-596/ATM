package test;

import dao.DurableItemDAO;
import entity.DurableItem;

import java.time.LocalDate;
import java.util.List;

public class TestDurableItem {
    public static void main(String[] args) {
        DurableItemDAO dao = new DurableItemDAO();

        // 1. 添加一条测试数据：一瓶洗面奶，买成45元，今天刚买
        DurableItem item = new DurableItem(
                "氨基酸洗面奶",
                "个护清洁",
                LocalDate.of(2026, 8, 1),  // 假设8月1号买的
                45.0
        );
        item.setNote("超市购入");
        dao.add(item);

        // 2. 查询所有物品并打印日均成本
        System.out.println("\n📊 当前持有的所有物品：");
        List<DurableItem> items = dao.findAll();
        for (DurableItem i : items) {
            System.out.println("  " + i.getName() +
                    " | 日均: " + String.format("%.2f", i.calcDailyCost()) + "元/天" +
                    " | 状态: " + (i.isActive() ? "使用中" : "已用完"));
        }

        // 3. 测试标记为用完
        if (!items.isEmpty()) {
            int firstId = items.get(0).getId();
            System.out.println("\n🔄 将 " + items.get(0).getName() + " 标记为已用完...");
            dao.markAsUsedUp(firstId);
        }

        // 4. 再次查询确认更新后的状态
        System.out.println("\n📊 更新后的列表：");
        List<DurableItem> updatedList = dao.findAll();
        for (DurableItem i : updatedList) {
            System.out.println("  " + i.getName() +
                    " | 日均: " + String.format("%.2f", i.calcDailyCost()) + "元/天" +
                    " | 状态: " + (i.isActive() ? "使用中" : "已用完"));
        }
    }
}