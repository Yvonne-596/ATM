import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 经济阶段指标系统诊断程序
 * 基于天涯帖子"七剑战歌之风月"的逻辑：
 * 一切结论仅供参考用途，请勿用于除了测试、学习天涯帖子指标以外的用途
 * 对于代码结论不承担一切不良影响、后果
 */
public class EconomicIndicatorAnalyzer {

    static class DataRow implements Comparable<DataRow> {
        YearMonth date;
        double cnYield;          // 中国10年期国债收益率 (%)
        double usYield;          // 美国10年期国债收益率 (%)
        double porkPrice;        // 猪肉价格 (元/公斤)

        // 计算字段
        double m2YoY;            // M2同比增速 (%)
        double porkMonthlyChg;   // 猪肉月环比 (%)
        double porkCumulChg;     // 猪肉累计跌幅 (从首个数据点算起, %)

        @Override
        public int compareTo(DataRow o) {
            return this.date.compareTo(o.date);
        }
    }

    // ========== 工具方法 ==========
    private static double parsePercent(String s) {
        if (s == null || s.trim().isEmpty()) return Double.NaN;
        return Double.parseDouble(s.replace("%", "").trim());
    }

    private static double parseNumber(String s) {
        if (s == null || s.trim().isEmpty()) return Double.NaN;
        return Double.parseDouble(s.trim().replace(",", ""));
    }

    // ========== 读取宏观经济数据 (国债、猪肉) ==========
    private static List<DataRow> readMacroData(String filename) throws IOException {
        List<DataRow> list = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (!headerSkipped && line.contains("日期") && line.contains("国债收益率")) {
                    headerSkipped = true;
                    continue;
                }
                if (line.trim().startsWith("|") && line.contains("---")) continue; // 分隔线
                if (!headerSkipped) continue;
                if (!line.trim().startsWith("|")) continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length < 5) continue;
                String dateStr = parts[1].trim();
                String cnYieldStr = parts[2].trim();
                String usYieldStr = parts[3].trim();
                String porkStr = parts[4].trim();
                if (dateStr.isEmpty() || dateStr.equals("日期") || dateStr.startsWith("—")) continue;

                try {
                    YearMonth ym = YearMonth.parse(dateStr, formatter);
                    DataRow row = new DataRow();
                    row.date = ym;
                    row.cnYield = parsePercent(cnYieldStr);
                    row.usYield = parsePercent(usYieldStr);
                    row.porkPrice = parseNumber(porkStr);
                    list.add(row);
                } catch (Exception e) {
                    System.err.println("⚠️ 跳过无效宏观行: " + line.trim());
                }
            }
        }
        Collections.sort(list);
        return list;
    }

    // ========== 读取M2数据 ==========
    private static Map<YearMonth, Double> readM2Data(String filename) throws IOException {
        Map<YearMonth, Double> map = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (!headerSkipped && line.contains("日期") && line.contains("M2")) {
                    headerSkipped = true;
                    continue;
                }
                if (line.trim().startsWith("|") && line.contains("---")) continue;
                if (!headerSkipped) continue;
                if (!line.trim().startsWith("|")) continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length < 3) continue;
                String dateStr = parts[1].trim();
                String m2Str = parts[2].trim();
                if (dateStr.isEmpty() || dateStr.equals("日期") || dateStr.startsWith("—")) continue;

                try {
                    YearMonth ym = YearMonth.parse(dateStr, formatter);
                    double m2 = parseNumber(m2Str);
                    map.put(ym, m2);
                } catch (Exception e) {
                    System.err.println("⚠️ 跳过无效M2行: " + line.trim());
                }
            }
        }
        return map;
    }

    // ========== 合并数据，计算M2同比、猪肉环比/累计 ==========
    private static List<DataRow> mergeAndCalculate(List<DataRow> macroList,
                                                   Map<YearMonth, Double> m2Map) {
        // 只保留宏观数据中存在于M2映射中的日期
        List<DataRow> filtered = new ArrayList<>();
        for (DataRow row : macroList) {
            if (m2Map.containsKey(row.date)) {
                filtered.add(row);
            } else {
                System.err.println("⚠️ 日期 " + row.date + " 在M2数据中不存在，已跳过");
            }
        }
        if (filtered.isEmpty()) {
            throw new RuntimeException("没有匹配到任何日期");
        }
        Collections.sort(filtered);

        // 计算M2同比 (直接使用m2Map获取去年同月)
        for (DataRow row : filtered) {
            YearMonth prevYM = row.date.minusYears(1);
            Double currM2 = m2Map.get(row.date);
            Double prevM2 = m2Map.get(prevYM);
            if (currM2 != null && prevM2 != null) {
                row.m2YoY = (currM2 / prevM2 - 1) * 100.0;
            } else {
                row.m2YoY = Double.NaN;
            }
        }

        // 计算猪肉月环比、累计跌幅
        for (int i = 0; i < filtered.size(); i++) {
            DataRow curr = filtered.get(i);
            if (i == 0) {
                curr.porkMonthlyChg = 0.0;
                curr.porkCumulChg = 0.0;
            } else {
                DataRow prev = filtered.get(i - 1);
                curr.porkMonthlyChg = (curr.porkPrice / prev.porkPrice - 1) * 100.0;
                // 累计跌幅：从第一个数据点开始
                DataRow first = filtered.get(0);
                curr.porkCumulChg = (curr.porkPrice / first.porkPrice - 1) * 100.0;
            }
        }

        return filtered;
    }

    // ========== 分析诊断 ==========
    static class PhaseAnalysis {
        String m2Status;
        String yieldStatus;
        String foodStatus;
        String overallPhase;
    }

    private static PhaseAnalysis analyze(DataRow latest, List<DataRow> allData) {
        PhaseAnalysis result = new PhaseAnalysis();

        // 1. M2同比状态
        double m2 = latest.m2YoY;
        if (Double.isNaN(m2)) {
            result.m2Status = "⚠️ 数据缺失，无法判断";
        } else if (m2 > 13.0) {
            result.m2Status = "宽松（M2同比 > 13%）";
        } else if (m2 >= 11.0) {
            result.m2Status = "中性（11% ≤ M2同比 ≤ 13%）";
        } else if (m2 >= 10.0) {
            result.m2Status = "紧缩（10% ≤ M2同比 < 11%）";
        } else {
            result.m2Status = String.format("⚠️ 超级大紧缩（M2同比 %.2f%% < 10%%，资产负债表衰退风险）", m2);
        }

        // 2. 国债收益率传导
        double spread = latest.usYield - latest.cnYield;
        if (spread > 1.0) {
            result.yieldStatus = String.format("美债高企（利差%.2f%%），强烈传导", spread);
        } else if (spread > 0.5) {
            result.yieldStatus = String.format("利差正常（%.2f%%），有一定传导", spread);
        } else {
            result.yieldStatus = String.format("利差收窄（%.2f%%），传导压力较小", spread);
        }

        // 3. 猪肉价格信号（结合月环比和累计跌幅）
        double monthly = latest.porkMonthlyChg;
        double cumul = latest.porkCumulChg;
        // 统计连续暴跌月数（月环比 < -3%）
        int consecutiveDropMonths = 0;
        for (int i = allData.size() - 1; i >= 0; i--) {
            if (allData.get(i).porkMonthlyChg < -3.0) {
                consecutiveDropMonths++;
            } else {
                break;
            }
        }
        // 判断
        if (cumul < -15.0) {
            result.foodStatus = String.format("⚠️ 猪肉价格累计暴跌 %.1f%%，通缩先行信号强烈", cumul);
        } else if (cumul < -10.0 || consecutiveDropMonths >= 2) {
            result.foodStatus = String.format("⚠️ 猪肉价格持续下跌（累计%.1f%%，连续%d个月环比跌幅>3%%），关注通缩风险", cumul, consecutiveDropMonths);
        } else if (monthly < -3.0) {
            result.foodStatus = "⚠️ 猪肉价格月环比明显下跌，需警惕";
        } else {
            result.foodStatus = "猪肉价格相对平稳，暂未出现明显通缩信号";
        }

        // 4. 综合判断
        boolean m2SuperTight = m2 < 10.0;
        boolean porkSevere = cumul < -10.0 || consecutiveDropMonths >= 2;
        if (m2SuperTight && porkSevere) {
            result.overallPhase = "🔴 恶性通缩预警 —— 超级紧缩+猪肉持续暴跌，大萧条风险上升";
        } else if (m2SuperTight || porkSevere) {
            result.overallPhase = "🟡 通缩压力期 —— 部分指标接近临界，需持续观察";
        } else {
            result.overallPhase = "🟢 相对平稳 —— 各项指标尚未触发警报";
        }

        // 补充M2连续低于10%月数
        int m2LowCount = 0;
        for (int i = allData.size() - 1; i >= 0; i--) {
            if (!Double.isNaN(allData.get(i).m2YoY) && allData.get(i).m2YoY < 10.0) {
                m2LowCount++;
            } else {
                break;
            }
        }
        if (m2LowCount >= 3) {
            result.overallPhase += String.format("（⚠️ M2同比已连续%d个月低于10%%，信用收缩风险加剧）", m2LowCount);
        }

        return result;
    }

    // ========== 生成报告 ==========
    private static String generateReport(DataRow latest, PhaseAnalysis analysis, List<DataRow> allData) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 经济阶段指标系统报告（基于作者观点：同比优先，综合趋势）\n\n");
        sb.append("**数据日期：** ").append(latest.date).append("\n\n");

        sb.append("## 一、核心指标快照\n\n");
        sb.append("| 指标 | 当前值 | 阈值判断 |\n");
        sb.append("|------|--------|----------|\n");
        sb.append(String.format("| M2增速（同比） | %.2f%% | %s |\n", latest.m2YoY, analysis.m2Status));
        sb.append(String.format("| 中国10年期国债收益率 | %.2f%% | — |\n", latest.cnYield));
        sb.append(String.format("| 美国10年期国债收益率 | %.2f%% | 中美利差 %.2f%% |\n", latest.usYield, latest.usYield - latest.cnYield));
        sb.append(String.format("| 猪肉价格月环比 | %.2f%% | %s |\n", latest.porkMonthlyChg,
                latest.porkMonthlyChg < -3.0 ? "下跌显著" : "相对平稳"));
        sb.append(String.format("| 猪肉价格累计跌幅 | %.2f%% | %s |\n", latest.porkCumulChg,
                latest.porkCumulChg < -10.0 ? "通缩预警" : "相对平稳"));

        sb.append("\n## 二、阶段分析\n\n");

        sb.append("### M2增速\n");
        sb.append("- ").append(analysis.m2Status).append("\n");
        sb.append("  - 注：根据作者观点，M2同比增速低于10%即进入超级紧缩区间，表明新增信贷难以覆盖债务利息，资产负债表衰退风险激增。\n\n");

        sb.append("### 国债收益率传导\n");
        sb.append("- ").append(analysis.yieldStatus).append("\n");
        sb.append("  - 美债收益率是全球利率的锚，其走高将通过资本流动和心理预期推升国内长端利率，进而抬高房贷和企业融资成本。\n\n");

        sb.append("### 食品价格信号（猪肉）\n");
        sb.append("- ").append(analysis.foodStatus).append("\n");
        sb.append("  - 猪肉因储存期短、消费高频，其价格暴跌（特别是累计跌幅较大或连续数月暴跌）往往是货币紧缩与需求萎缩的先行指标。\n\n");

        sb.append("## 三、综合判断\n\n");
        sb.append("> **").append(analysis.overallPhase).append("**\n\n");

        sb.append("### 建议关注\n");
        // 动态建议
        if (!Double.isNaN(latest.m2YoY) && latest.m2YoY < 10.0) {
            int lowCount = 0;
            for (int i = allData.size() - 1; i >= 0; i--) {
                if (!Double.isNaN(allData.get(i).m2YoY) && allData.get(i).m2YoY < 10.0) {
                    lowCount++;
                } else break;
            }
            if (lowCount >= 3) {
                sb.append("- 若M2同比连续三个月低于10%（当前已满足），需警惕信用收缩连锁反应\n");
            } else {
                sb.append("- 关注M2同比是否持续低于10%，若连续三个月则信用收缩风险骤增\n");
            }
        }
        if ((latest.usYield - latest.cnYield) > 1.5) {
            sb.append("- 若中美利差持续扩大，关注央行是否跟随加息\n");
        }
        if (latest.porkCumulChg < -10.0) {
            sb.append("- 若食品价格持续深跌，通缩预期将自我强化，现金为王策略占优\n");
        }
        sb.append("- 当前策略建议：降低杠杆，增加现金储备，避免新增债务。\n");

        return sb.toString();
    }

    // ========== 主程序 ==========
    public static void main(String[] args) {
        String macroFile = "宏观经济数据.txt";
        String m2File = "M2.txt";
        String outFile = "经济阶段的“指标系统”.txt";

        try {
            // 1. 读取数据
            List<DataRow> macroList = readMacroData(macroFile);
            Map<YearMonth, Double> m2Map = readM2Data(m2File);

            // 2. 合并计算
            List<DataRow> allData = mergeAndCalculate(macroList, m2Map);
            if (allData.isEmpty()) {
                System.err.println("❌ 没有有效数据");
                return;
            }

            // 3. 分析最新数据
            DataRow latest = allData.get(allData.size() - 1);
            PhaseAnalysis analysis = analyze(latest, allData);

            // 4. 生成报告
            String report = generateReport(latest, analysis, allData);

            // 5. 输出
            try (FileWriter fw = new FileWriter(outFile, StandardCharsets.UTF_8)) {
                fw.write(report);
            }
            System.out.println("✅ 报告已生成：" + outFile);
            System.out.println("📄 内容预览：\n" + report);

        } catch (IOException e) {
            System.err.println("❌ 错误：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
