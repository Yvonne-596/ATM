package entity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DurableItem {
    private int id;
    private String name;
    private String category;
    private LocalDate purchaseDate;
    private double totalPrice;
    private LocalDate endDate;      // 用完日期，如果还在用则为null
    private Double dailyUsage;      // 每天用量（克/毫升），可为null
    private Double remainingQty;    // 剩余量，可为null
    private String note;

    // 构造方法（无参）
    public DurableItem() {}

    // 构造方法（带核心字段，方便快速创建）
    public DurableItem(String name, String category, LocalDate purchaseDate, double totalPrice) {
        this.name = name;
        this.category = category;
        this.purchaseDate = purchaseDate;
        this.totalPrice = totalPrice;
        this.endDate = null;
    }

    // ========== Getter 和 Setter ==========
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Double getDailyUsage() { return dailyUsage; }
    public void setDailyUsage(Double dailyUsage) { this.dailyUsage = dailyUsage; }

    public Double getRemainingQty() { return remainingQty; }
    public void setRemainingQty(Double remainingQty) { this.remainingQty = remainingQty; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    // ========== 核心业务方法 ==========

    /**
     * 计算截至今天的日均持有成本
     * 如果 endDate 不为空，则计算从购买到用完的日均成本
     * 如果 endDate 为空，则计算从购买到今天的天数
     */
    public double calcDailyCost() {
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();
        long days = ChronoUnit.DAYS.between(purchaseDate, end) + 1; // +1防止当天买当天算0天
        if (days <= 0) days = 1; // 容错处理
        return totalPrice / days;
    }

    /**
     * 判断物品是否还在使用中（未标记为用完）
     */
    public boolean isActive() {
        return endDate == null;
    }

    /**
     * 将物品标记为已用完
     */
    public void markAsUsedUp() {
        this.endDate = LocalDate.now();
    }

    @Override
    public String toString() {
        return name + " (" + category + ") 日均: " + String.format("%.2f", calcDailyCost()) + "元/天";
    }
}