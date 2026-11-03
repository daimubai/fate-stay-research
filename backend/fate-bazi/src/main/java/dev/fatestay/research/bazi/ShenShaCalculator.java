package dev.fatestay.research.bazi;

import dev.fatestay.research.calendar.TymeCalendar.Analysis;
import java.util.ArrayList;
import java.util.List;

/** Explicit natal rules over Tyme-derived pillars; not Tyme's almanac God API. */
public final class ShenShaCalculator {
    private ShenShaCalculator() {}
    public record Rule(String id, String name, String reference) {}
    public record Basis(String reference, String sourceValue, List<String> targetValues) {
        public Basis { targetValues = List.copyOf(targetValues); }
    }
    public record Hit(String id, String name, String pillarKey, String pillarLabel,
                      String targetBranch, List<Basis> bases) {
        public Hit { bases = List.copyOf(bases); }
    }
    public record Result(String provider, String ruleVersion, List<Rule> rules, List<Hit> hits) {
        public Result { rules = List.copyOf(rules); hits = List.copyOf(hits); }
    }
    private static final List<Rule> RULES = List.of(
        new Rule("TIAN_YI", "天乙贵人", "日干"), new Rule("LU_SHEN", "禄神", "日干"),
        new Rule("YI_MA", "驿马", "年支、日支"), new Rule("TAO_HUA", "桃花", "年支、日支"),
        new Rule("HUA_GAI", "华盖", "年支、日支"), new Rule("JIANG_XING", "将星", "年支、日支"),
        new Rule("XUN_KONG", "旬空", "日柱"));
    private static List<String> branches(String value) {
        return value.codePoints().mapToObj(c -> new String(Character.toChars(c))).toList();
    }
    static String noble(String stem) {
        return switch (stem) {
            case "甲", "戊", "庚" -> "丑未";
            case "乙", "己" -> "子申";
            case "丙", "丁" -> "亥酉";
            case "辛" -> "寅午";
            case "壬", "癸" -> "卯巳";
            default -> throw new IllegalArgumentException("无效天干");
        };
    }
    static String groupTarget(String branch, int ruleIndex) {
        String[] groups = {"申子辰", "寅午戌", "亥卯未", "巳酉丑"};
        String[] targets = {"寅酉辰子", "申卯戌午", "巳子未卯", "亥午丑酉"};
        for (int i = 0; i < groups.length; i++)
            if (groups[i].contains(branch)) return branches(targets[i]).get(ruleIndex);
        throw new IllegalArgumentException("无效地支");
    }
    public static Result calculate(Analysis analysis) {
        var hits = new ArrayList<Hit>();
        var day = analysis.dayMaster().name();
        for (int i = 0; i < RULES.size(); i++) {
            var rule = RULES.get(i);
            var bases = new ArrayList<Basis>();
            if (i == 0) bases.add(new Basis("日干", day, branches(noble(day))));
            else if (i == 1) bases.add(new Basis("日干", day, List.of(analysis.dayLuBranch())));
            else if (i == 6) bases.add(new Basis("日柱", analysis.pillars().day(), analysis.dayVoidBranches()));
            else {
                for (var source : analysis.pillarDetails()) {
                    if (source.key().equals("year") || source.key().equals("day"))
                        bases.add(new Basis(source.key().equals("year") ? "年支" : "日支",
                            source.branch().name(), List.of(groupTarget(source.branch().name(), i - 2))));
                }
            }
            // Include the reference pillar itself; merge year/day evidence into one tag per pillar/rule.
            for (var pillar : analysis.pillarDetails()) {
                var matched = bases.stream().filter(b -> b.targetValues().contains(pillar.branch().name())).toList();
                if (!matched.isEmpty()) hits.add(new Hit(rule.id(), rule.name(), pillar.key(), pillar.label(),
                    pillar.branch().name(), matched));
            }
        }
        return new Result("PROJECT_RULES_ON_TYME4J", "common-seven-day-year-v1", RULES, hits);
    }
}
