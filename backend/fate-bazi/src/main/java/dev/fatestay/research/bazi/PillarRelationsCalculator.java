package dev.fatestay.research.bazi;

import dev.fatestay.research.calendar.TymeCalendar.Pillars;
import dev.fatestay.research.calendar.TymeRelations;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Natal four-pillar structural matches only; does not infer strength, cancellation or successful transformation. */
public final class PillarRelationsCalculator {
    private PillarRelationsCalculator() {}
    public enum Kind {
        STEM_COMBINATION, BRANCH_SIX_COMBINATION, BRANCH_THREE_COMBINATION,
        BRANCH_PUNISHMENT, BRANCH_CLASH, BRANCH_BREAK, BRANCH_HARM
    }
    public record Rule(Kind kind, String label, String provider) {}
    public record Participant(String pillarKey, String pillarLabel, String ganZhi, String value) {}
    public record Hit(String id, Kind kind, String pattern, String form, boolean directed,
                      List<Participant> participants, String associatedElement, String basis) {
        public Hit { participants = List.copyOf(participants); }
    }
    public record Result(String ruleVersion, String scope, List<Rule> rules, List<Hit> hits) {
        public Result { rules = List.copyOf(rules); hits = List.copyOf(hits); }
    }
    private static final List<Rule> RULES = List.of(
        new Rule(Kind.STEM_COMBINATION,"天干五合","TYME4J"),
        new Rule(Kind.BRANCH_SIX_COMBINATION,"地支六合","TYME4J"),
        new Rule(Kind.BRANCH_THREE_COMBINATION,"地支三合","PROJECT_RULES_ON_TYME4J"),
        new Rule(Kind.BRANCH_PUNISHMENT,"地支刑","PROJECT_RULES_ON_TYME4J"),
        new Rule(Kind.BRANCH_CLASH,"地支冲","TYME4J"),
        new Rule(Kind.BRANCH_BREAK,"地支破","PROJECT_RULES_ON_TYME4J"),
        new Rule(Kind.BRANCH_HARM,"地支害","TYME4J"));
    private static final List<String> KEYS = List.of("year","month","day","hour");
    private static final String[] LABELS = {"年柱","月柱","日柱","时柱"};
    private static final String[] TRINES = {"申子辰","亥卯未","寅午戌","巳酉丑"};
    private static final String[] TRINE_ELEMENTS = {"水","木","火","金"};
    private static final String[] PUNISHMENTS = {"寅巳申","丑戌未"};
    private static final String[] BREAKS = {"子酉","卯午","辰丑","未戌","寅亥","巳申"};

    public static Result calculate(Pillars pillars) {
        var cycles = List.of(pillars.year(),pillars.month(),pillars.day(),pillars.hour());
        var hits = new ArrayList<Hit>();
        for (int i=0;i<4;i++) for (int j=i+1;j<4;j++) {
            // Parsing both full Gan-Zhi through Tyme also validates the synthetic/domain inputs.
            var nativePair = TymeRelations.between(cycles.get(i),cycles.get(j));
            var stems = List.of(participant(cycles,i,true),participant(cycles,j,true));
            var branches = List.of(participant(cycles,i,false),participant(cycles,j,false));
            String a = branches.get(0).value(), b = branches.get(1).value();
            if (nativePair.stemCombineElement()!=null) add(hits,Kind.STEM_COMBINATION,canonical(stems),"PAIR",false,stems,nativePair.stemCombineElement(),"Tyme4j 天干五合；配属五行，不判合化");
            if (nativePair.branchCombineElement()!=null) add(hits,Kind.BRANCH_SIX_COMBINATION,canonical(branches),"PAIR",false,branches,nativePair.branchCombineElement(),"Tyme4j 地支六合；配属五行，不判合化");
            if (nativePair.clash()) add(hits,Kind.BRANCH_CLASH,canonical(branches),"PAIR",false,branches,null,"Tyme4j 六冲");
            if (nativePair.harm()) add(hits,Kind.BRANCH_HARM,canonical(branches),"PAIR",false,branches,null,"Tyme4j 六害");
            for (String pattern : BREAKS) if (pairMatches(pattern,a,b)) add(hits,Kind.BRANCH_BREAK,pattern,"PAIR",false,branches,null,"六破固定查表");
            if (pairMatches("子卯",a,b)) add(hits,Kind.BRANCH_PUNISHMENT,"子卯","PAIR",false,branches,null,"子卯互刑");
            if (a.equals(b) && "辰午酉亥".contains(a)) add(hits,Kind.BRANCH_PUNISHMENT,a+a,"SELF_PAIR",false,branches,null,"不同柱位同支自刑");
            for (String group : PUNISHMENTS) {
                if (a.equals(b) || !group.contains(a) || !group.contains(b) || completeGroup(cycles,group)) continue;
                int first = group.indexOf(a), second = group.indexOf(b);
                var ordered = (first+1)%3 == second ? branches : List.of(branches.get(1),branches.get(0));
                add(hits,Kind.BRANCH_PUNISHMENT,group,"PAIR",true,ordered,null,
                    ordered.get(0).value()+"刑"+ordered.get(1).value()+"；三刑组仅见两支");
            }
        }
        // Every distinct three-column combination is preserved, including repeated branches in a fourth column.
        for (int i=0;i<4;i++) for (int j=i+1;j<4;j++) for (int k=j+1;k<4;k++) {
            var participants = List.of(participant(cycles,i,false),participant(cycles,j,false),participant(cycles,k,false));
            var values = participants.stream().map(Participant::value).collect(Collectors.toSet());
            for (int n=0;n<TRINES.length;n++) if (matchesGroup(values,TRINES[n]))
                add(hits,Kind.BRANCH_THREE_COMBINATION,TRINES[n],"TRIPLE",false,participants,TRINE_ELEMENTS[n],"三支齐见；只标记三合组合，不判成化");
            for (String group : PUNISHMENTS) if (matchesGroup(values,group))
                add(hits,Kind.BRANCH_PUNISHMENT,group,"TRIPLE",false,participants,null,
                    group.charAt(0)+"刑"+group.charAt(1)+"、"+group.charAt(1)+"刑"+group.charAt(2)+"、"+group.charAt(2)+"刑"+group.charAt(0)+"；三刑齐见");
        }
        hits.sort(Comparator.comparing(Hit::kind).thenComparing(Hit::id));
        return new Result("natal-pillar-relations-v1","NATAL_FOUR_PILLARS",RULES,hits);
    }
    private static Participant participant(List<String> cycles,int index,boolean stem) {
        return new Participant(KEYS.get(index),LABELS[index],cycles.get(index),cycles.get(index).substring(stem?0:1,stem?1:2));
    }
    private static boolean pairMatches(String pattern,String a,String b) {
        return !a.equals(b) && pattern.contains(a) && pattern.contains(b);
    }
    private static boolean matchesGroup(Set<String> values,String pattern) {
        return values.size()==3 && values.stream().allMatch(pattern::contains);
    }
    private static boolean completeGroup(List<String> cycles,String group) {
        return group.codePoints().allMatch(c -> cycles.stream().anyMatch(v -> v.endsWith(new String(Character.toChars(c)))));
    }
    private static String canonical(List<Participant> participants) {
        return participants.stream().map(Participant::value).sorted(Comparator.comparingInt("甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午未申酉戌亥"::indexOf)).collect(Collectors.joining());
    }
    private static void add(List<Hit> hits,Kind kind,String pattern,String form,boolean directed,List<Participant> participants,String element,String basis) {
        String id=kind+":"+participants.stream().map(Participant::pillarKey).sorted(Comparator.comparingInt(KEYS::indexOf)).collect(Collectors.joining("-"));
        hits.add(new Hit(id,kind,pattern,form,directed,participants,element,basis));
    }
}
