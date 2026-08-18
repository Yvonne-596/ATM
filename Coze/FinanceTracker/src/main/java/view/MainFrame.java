package view;

import dao.DurableItemDAO;
import entity.DurableItem;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MainFrame extends JFrame {

    private DurableItemDAO dao = new DurableItemDAO();
    private JTable table;
    private DefaultTableModel tableModel;
    private JTabbedPane tabbedPane;

    public MainFrame() {
        setTitle("📊 我的智能物品账本（含图表分析）");
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
        loadData();
        refreshCharts();
    }

    // ---------- 初始化UI（表格 + 图表双标签页） ----------
    private void initUI() {
        tabbedPane = new JTabbedPane();

        // ----- 标签页1：数据列表 -----
        JPanel listPanel = new JPanel(new BorderLayout());

        String[] columns = {"ID", "名称", "品类", "购买日期", "总价(元)", "日均成本(元/天)", "性价比评级", "状态"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.getColumnModel().getColumn(6).setPreferredWidth(180);

        // 双击表格行触发编辑
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editItem();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);

        // 按钮面板
        JPanel btnPanel = new JPanel();
        JButton btnAdd = new JButton("➕ 添加物品");
        JButton btnEdit = new JButton("✏️ 编辑物品");
        JButton btnMarkUsed = new JButton("✅ 标记用完");
        JButton btnDelete = new JButton("🗑️ 删除");
        JButton btnRefresh = new JButton("🔄 刷新");

        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnMarkUsed);
        btnPanel.add(btnDelete);
        btnPanel.add(btnRefresh);

        btnAdd.addActionListener(this::addItem);
        btnEdit.addActionListener(e -> editItem());
        btnMarkUsed.addActionListener(this::markAsUsed);
        btnDelete.addActionListener(this::deleteItem);
        btnRefresh.addActionListener(e -> { loadData(); refreshCharts(); });

        listPanel.add(scrollPane, BorderLayout.CENTER);
        listPanel.add(btnPanel, BorderLayout.SOUTH);

        // ----- 标签页2：图表看板 -----
        JPanel chartPanel = createChartPanel();

        tabbedPane.addTab("📋 数据列表", listPanel);
        tabbedPane.addTab("📈 图表分析", chartPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ---------- 创建图表面板（左右布局） ----------
    private JPanel createChartPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(new ChartPanel(createPieChart()));
        panel.add(new ChartPanel(createBarChart()));
        return panel;
    }

    // ---------- 生成饼图（所有中文均已设置字体） ----------
    private JFreeChart createPieChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        List<DurableItem> items = dao.findAll();
        items.stream()
                .filter(DurableItem::isActive)
                .collect(Collectors.groupingBy(DurableItem::getCategory, Collectors.summingDouble(DurableItem::getTotalPrice)))
                .forEach((category, sum) -> dataset.setValue(category, sum));

        if (dataset.getKeys().isEmpty()) {
            dataset.setValue("暂无数据", 1.0);
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "各品类持有成本占比",
                dataset,
                true,
                true,
                false
        );

        // ★★★ 统一中文字体（标题、图例、扇区标签） ★★★
        Font titleFont = new Font("微软雅黑", Font.BOLD, 14);
        Font textFont = new Font("微软雅黑", Font.PLAIN, 12);

        chart.getTitle().setFont(titleFont);
        chart.getLegend().setItemFont(textFont);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelFont(textFont);      // ← 扇区标签（就是你说的连线标注）
        plot.setLabelBackgroundPaint(null); // 可选：让标签背景透明，更清爽

        return chart;
    }

    // ---------- 生成柱状图（所有中文均已设置字体） ----------
    private JFreeChart createBarChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<DurableItem> items = dao.findAll().stream()
                .filter(DurableItem::isActive)
                .sorted(Comparator.comparingDouble(DurableItem::calcDailyCost).reversed())
                .limit(10)
                .collect(Collectors.toList());

        for (DurableItem item : items) {
            dataset.addValue(item.calcDailyCost(), "日均成本(元)", item.getName());
        }

        if (dataset.getRowCount() == 0) {
            dataset.addValue(0, "暂无数据", "请添加物品");
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "💰 日均成本排行榜（使用中）",
                "物品名称",
                "元/天",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // ★★★ 统一中文字体（标题、图例、X轴、Y轴） ★★★
        Font titleFont = new Font("微软雅黑", Font.BOLD, 14);
        Font textFont = new Font("微软雅黑", Font.PLAIN, 12);

        chart.getTitle().setFont(titleFont);
        chart.getLegend().setItemFont(textFont);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.getDomainAxis().setTickLabelFont(textFont);   // X轴物品名称
        plot.getDomainAxis().setLabelFont(textFont);       // X轴标题"物品名称"
        plot.getRangeAxis().setTickLabelFont(textFont);    // Y轴刻度数字
        plot.getRangeAxis().setLabelFont(textFont);        // Y轴标题"元/天"

        return chart;
    }

    // ---------- 刷新图表 ----------
    private void refreshCharts() {
        int chartIndex = tabbedPane.indexOfTab("📈 图表分析");
        if (chartIndex != -1) {
            tabbedPane.remove(chartIndex);
            tabbedPane.addTab("📈 图表分析", createChartPanel());
        }
    }

    // ---------- 性价比评级算法 ----------
    private String getRating(double dailyCost, String category) {
        double healthyLine = 2.0;
        double warningLine = 5.0;

        if (category.contains("洗面") || category.contains("洁面") || category.contains("个护清洁")) {
            healthyLine = 0.6;
            warningLine = 1.0;
        } else if (category.contains("面霜") || category.contains("乳液") || category.contains("护肤")) {
            healthyLine = 3.0;
            warningLine = 5.0;
        } else if (category.contains("防晒")) {
            healthyLine = 2.5;
            warningLine = 4.0;
        } else if (category.contains("手机") || category.contains("智能手机")) {
            healthyLine = 5.0;
            warningLine = 8.0;
        } else if (category.contains("笔记本") || category.contains("电脑")) {
            healthyLine = 5.0;
            warningLine = 10.0;
        } else if (category.contains("耳机") || category.contains("音响")) {
            healthyLine = 0.5;
            warningLine = 1.0;
        } else if (category.contains("鞋") || category.contains("运动")) {
            healthyLine = 1.0;
            warningLine = 2.0;
        } else if (category.contains("羽绒服") || category.contains("外套")) {
            healthyLine = 0.8;
            warningLine = 1.5;
        } else if (category.contains("牙刷") || category.contains("口腔")) {
            healthyLine = 0.8;
            warningLine = 1.5;
        } else if (category.contains("床垫") || category.contains("寝具")) {
            healthyLine = 1.2;
            warningLine = 2.0;
        }

        if (dailyCost <= healthyLine) {
            return "🟢 性价比极高 (优于均值)";
        } else if (dailyCost <= warningLine) {
            return "🟡 市场正常水平 (可接受)";
        } else {
            return "🔴 日均偏高 (建议考虑平替)";
        }
    }

    // ---------- 加载表格数据 ----------
    private void loadData() {
        tableModel.setRowCount(0);
        List<DurableItem> items = dao.findAll();

        if (items.isEmpty()) {
            tableModel.addRow(new Object[]{"", "暂无数据，请添加物品", "", "", "", "", "", ""});
            return;
        }

        for (DurableItem item : items) {
            double dailyCost = item.calcDailyCost();
            String status = item.isActive() ? "使用中" : "已用完";
            String rating = getRating(dailyCost, item.getCategory());

            tableModel.addRow(new Object[]{
                    item.getId(),
                    item.getName(),
                    item.getCategory(),
                    item.getPurchaseDate(),
                    String.format("%.2f", item.getTotalPrice()),
                    String.format("%.2f", dailyCost),
                    rating,
                    status
            });
        }
    }

    // ---------- 通用输入对话框 ----------
    private Object[] showItemDialog(String title, String name, String category, String priceStr, String dateStr) {
        JTextField nameField = new JTextField(name, 15);
        JTextField categoryField = new JTextField(category, 15);
        JTextField priceField = new JTextField(priceStr, 15);
        JTextField dateField = new JTextField(dateStr, 15);

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.add(new JLabel("名称："));
        panel.add(nameField);
        panel.add(new JLabel("品类："));
        panel.add(categoryField);
        panel.add(new JLabel("价格（元）："));
        panel.add(priceField);
        panel.add(new JLabel("购买日期（格式 yyyy-MM-dd）："));
        panel.add(dateField);

        int result = JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            return new Object[]{nameField.getText().trim(), categoryField.getText().trim(), priceField.getText().trim(), dateField.getText().trim()};
        }
        return null;
    }

    // ---------- 添加物品 ----------
    private void addItem(ActionEvent e) {
        String today = LocalDate.now().toString();
        Object[] inputs = showItemDialog("➕ 添加新物品", "", "", "", today);
        if (inputs == null) return;

        String name = (String) inputs[0];
        String category = (String) inputs[1];
        String priceStr = (String) inputs[2];
        String dateStr = (String) inputs[3];

        if (name.isEmpty() || category.isEmpty() || priceStr.isEmpty() || dateStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "所有字段都不能为空！");
            return;
        }

        double price;
        LocalDate date;
        try {
            price = Double.parseDouble(priceStr);
            date = LocalDate.parse(dateStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "价格格式错误，请输入数字");
            return;
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "日期格式错误，请使用 yyyy-MM-dd 格式（如 2026-08-18）");
            return;
        }

        DurableItem newItem = new DurableItem(name, category, date, price);
        dao.add(newItem);
        loadData();
        refreshCharts();
        JOptionPane.showMessageDialog(this, "✅ 添加成功！");
    }

    // ---------- 编辑物品 ----------
    private void editItem() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先在表格中选中一行！");
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        DurableItem item = dao.findById(id);
        if (item == null) {
            JOptionPane.showMessageDialog(this, "未找到该物品数据");
            return;
        }

        Object[] inputs = showItemDialog(
                "✏️ 编辑物品",
                item.getName(),
                item.getCategory(),
                String.valueOf(item.getTotalPrice()),
                item.getPurchaseDate().toString()
        );
        if (inputs == null) return;

        String name = (String) inputs[0];
        String category = (String) inputs[1];
        String priceStr = (String) inputs[2];
        String dateStr = (String) inputs[3];

        if (name.isEmpty() || category.isEmpty() || priceStr.isEmpty() || dateStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "所有字段都不能为空！");
            return;
        }

        try {
            item.setName(name);
            item.setCategory(category);
            item.setTotalPrice(Double.parseDouble(priceStr));
            item.setPurchaseDate(LocalDate.parse(dateStr));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "价格格式错误，请输入数字");
            return;
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "日期格式错误，请使用 yyyy-MM-dd 格式");
            return;
        }

        dao.update(item);
        loadData();
        refreshCharts();
        JOptionPane.showMessageDialog(this, "✅ 更新成功！");
    }

    // ---------- 标记为已用完 ----------
    private void markAsUsed(ActionEvent e) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先在表格中选中一行！");
            return;
        }
        int id = (int) tableModel.getValueAt(selectedRow, 0);
        dao.markAsUsedUp(id);
        loadData();
        refreshCharts();
        JOptionPane.showMessageDialog(this, "✅ 已标记为用完！");
    }

    // ---------- 删除物品 ----------
    private void deleteItem(ActionEvent e) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "请先在表格中选中一行！");
            return;
        }
        int id = (int) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "确定要删除吗？", "确认删除", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dao.delete(id);
            loadData();
            refreshCharts();
            JOptionPane.showMessageDialog(this, "🗑️ 已删除！");
        }
    }

    // ---------- 程序入口 ----------
    public static void main(String[] args) {
        db.DBHelper.initDatabase();
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}