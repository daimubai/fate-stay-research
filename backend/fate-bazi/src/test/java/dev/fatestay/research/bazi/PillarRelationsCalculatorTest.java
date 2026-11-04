package dev.fatestay.research.bazi;

import dev.fatestay.research.calendar.TymeCalendar.Pillars;
import dev.fatestay.research.calendar.TymeRelations;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static dev.fatestay.research.bazi.PillarRelationsCalculator.Kind.*;
import static org.junit.jupiter.api.Assertions.*;

class PillarRelationsCalculatorTest {
    private static final String STEMS="甲乙丙丁戊己庚辛壬癸", BRANCHES="子丑寅卯辰巳午未申酉戌亥";
    private static String branchCycle(String branch) { return (BRANCHES.indexOf(branch)%2==0?"甲":"乙")+branch; }
    private static String stemCycle(String stem) { return stem+(STEMS.indexOf(stem)%2==0?"子":"丑"); }
    private static PillarRelationsCalculator.Result branches(String a,String b,String c,String d) {
        return PillarRelationsCalculator.calculate(new Pillars(branchCycle(a),branchCycle(b),branchCycle(c),branchCycle(d)));
    }
    private static List<PillarRelationsCalculator.Hit> kind(PillarRelationsCalculator.Result r,PillarRelationsCalculator.Kind kind) {
        return r.hits().stream().filter(h -> h.kind()==kind).toList();
    }
    private static List<PillarRelationsCalculator.Hit> yearMonth(PillarRelationsCalculator.Result r,PillarRelationsCalculator.Kind kind) {
        return kind(r,kind).stream().filter(h -> h.participants().stream().map(PillarRelationsCalculator.Participant::pillarKey).collect(Collectors.toSet()).equals(Set.of("year","month"))).toList();
    }
    @Test void nativeStemMappingIsExhaustiveAndSymmetricForAllHundredPairs() {
        var elements=Map.of("甲己","土","乙庚","金","丙辛","水","丁壬","木","戊癸","火");
        for(int i=0;i<10;i++) for(int j=0;j<10;j++) {
            String a=STEMS.substring(i,i+1), b=STEMS.substring(j,j+1);
            String expected=elements.get(i<j?a+b:b+a);
            var pair=TymeRelations.between(stemCycle(a),stemCycle(b));
            assertEquals(expected,pair.stemCombineElement(),a+b);
            var r=PillarRelationsCalculator.calculate(new Pillars(stemCycle(a),stemCycle(b),"丙寅","戊辰"));
            assertEquals(expected==null?0:1,yearMonth(r,STEM_COMBINATION).size(),a+b);
            if(expected!=null) assertEquals(expected,yearMonth(r,STEM_COMBINATION).getFirst().associatedElement());
        }
    }
    @Test void branchPairRulesCoverAll144PairsAndDoNotInventRelations() {
        var combinations=Map.of("子丑","土","寅亥","木","卯戌","火","辰酉","金","巳申","水","午未","土");
        var clashes=Set.of("子午","丑未","寅申","卯酉","辰戌","巳亥");
        var harms=Set.of("子未","丑午","寅巳","卯辰","申亥","酉戌");
        var breaks=Set.of("子酉","丑辰","寅亥","卯午","巳申","未戌");
        for(int i=0;i<12;i++) for(int j=0;j<12;j++) {
            String a=BRANCHES.substring(i,i+1), b=BRANCHES.substring(j,j+1), key=i<j?a+b:b+a;
            var p=TymeRelations.between(branchCycle(a),branchCycle(b));
            assertEquals(combinations.get(key),p.branchCombineElement(),a+b);
            assertEquals(clashes.contains(key),p.clash(),a+b);
            assertEquals(harms.contains(key),p.harm(),a+b);
            var r=branches(a,b,"辰","巳");
            assertEquals(combinations.containsKey(key)?1:0,yearMonth(r,BRANCH_SIX_COMBINATION).size(),a+b);
            assertEquals(clashes.contains(key)?1:0,yearMonth(r,BRANCH_CLASH).size(),a+b);
            assertEquals(harms.contains(key)?1:0,yearMonth(r,BRANCH_HARM).size(),a+b);
            assertEquals(breaks.contains(key)?1:0,yearMonth(r,BRANCH_BREAK).size(),a+b);
        }
    }
    @ParameterizedTest @CsvSource({"申,子,辰,水", "亥,卯,未,木", "寅,午,戌,火", "巳,酉,丑,金"})
    void trinesRequireThreeDistinctBranchesAcrossEveryPermutation(String a,String b,String c,String element) {
        String other=BRANCHES.codePoints().mapToObj(v -> String.valueOf((char)v)).filter(v -> !List.of(a,b,c).contains(v)).findFirst().orElseThrow();
        for(var values:List.of(List.of(a,b,c),List.of(a,c,b),List.of(b,a,c),List.of(b,c,a),List.of(c,a,b),List.of(c,b,a))) {
            var hits=kind(branches(values.get(0),values.get(1),values.get(2),other),BRANCH_THREE_COMBINATION);
            assertEquals(1,hits.size()); assertEquals("TRIPLE",hits.getFirst().form());
            assertEquals(element,hits.getFirst().associatedElement());
            assertEquals(List.of("year","month","day"),hits.getFirst().participants().stream().map(PillarRelationsCalculator.Participant::pillarKey).toList());
        }
        assertTrue(kind(branches(a,b,a,b),BRANCH_THREE_COMBINATION).isEmpty()); // no half-combination
    }
    @ParameterizedTest @CsvSource({"寅,巳,申", "丑,戌,未"})
    void twoBranchPunishmentsPreserveDirectionInBothColumnOrders(String a,String b,String c) {
        for(var edge:List.of(List.of(a,b),List.of(b,c),List.of(c,a))) for(boolean reverse:List.of(false,true)) {
            var r=reverse?branches(edge.get(1),edge.get(0),"子","酉"):branches(edge.get(0),edge.get(1),"子","酉");
            var hit=yearMonth(r,BRANCH_PUNISHMENT).getFirst();
            assertEquals("PAIR",hit.form());assertTrue(hit.directed());
            assertEquals(edge,hit.participants().stream().map(PillarRelationsCalculator.Participant::value).toList());
        }
    }
    @ParameterizedTest @CsvSource({"寅,巳,申", "丑,戌,未"})
    void fullThreePunishmentDoesNotRepeatItsConstituentPairRows(String a,String b,String c) {
        var hits=kind(branches(a,b,c,c),BRANCH_PUNISHMENT);
        assertEquals(2,hits.size());
        assertTrue(hits.stream().allMatch(h -> h.form().equals("TRIPLE") && h.participants().size()==3));
        assertTrue(hits.stream().allMatch(h -> h.basis().contains("三刑齐见")));
    }
    @ParameterizedTest @CsvSource({"辰", "午", "酉", "亥"})
    void selfPunishmentRequiresSeparatePillarsAndKeepsAllSixPairs(String branch) {
        assertEquals(6,kind(branches(branch,branch,branch,branch),BRANCH_PUNISHMENT).size());
        assertTrue(kind(branches(branch,branch,branch,branch),BRANCH_PUNISHMENT).stream().allMatch(h -> h.form().equals("SELF_PAIR") && !h.directed()));
        assertFalse(kind(branches(branch,"子","丑","卯"),BRANCH_PUNISHMENT).stream().anyMatch(h -> h.form().equals("SELF_PAIR")));
    }
    @Test void repeatedBranchesKeepColumnIdentitiesRatherThanCollapsingToNames() {
        var r=branches("申","子","辰","辰");
        var hits=kind(r,BRANCH_THREE_COMBINATION);
        assertEquals(2,hits.size());assertNotEquals(hits.get(0).id(),hits.get(1).id());
        assertEquals(1,kind(r,BRANCH_PUNISHMENT).size());
        assertEquals(3,r.hits().size());
        assertEquals(r.hits().size(),r.hits().stream().map(PillarRelationsCalculator.Hit::id).distinct().count());
    }
    @Test void mutualPunishmentAndCombinationBreakCanCoexistWithoutCancellation() {
        var mutual=yearMonth(branches("子","卯","子","卯"),BRANCH_PUNISHMENT);
        assertEquals(1,mutual.size());assertFalse(mutual.getFirst().directed());
        var r=branches("寅","亥","辰","酉");
        assertEquals(1,yearMonth(r,BRANCH_SIX_COMBINATION).size());
        assertEquals(1,yearMonth(r,BRANCH_BREAK).size());
        assertEquals("木",yearMonth(r,BRANCH_SIX_COMBINATION).getFirst().associatedElement());
    }
    @Test void matchesAcrossNonAdjacentPillarsAndAllowsEmptyResult() {
        var r=PillarRelationsCalculator.calculate(new Pillars("甲子","丙寅","戊辰","己巳"));
        assertTrue(kind(r,STEM_COMBINATION).stream().anyMatch(h -> h.id().equals("STEM_COMBINATION:year-hour")));
        assertTrue(branches("子","子","子","子").hits().isEmpty());
    }
    @Test void knownBirthHasTwoTraceableRelationsAndImmutableCollections() {
        var r=new BaziCalculator().calculate(LocalDateTime.of(1986,5,29,13,37)).relations();
        assertEquals("natal-pillar-relations-v1",r.ruleVersion());assertEquals("NATAL_FOUR_PILLARS",r.scope());assertEquals(7,r.rules().size());
        assertEquals(List.of(BRANCH_PUNISHMENT,BRANCH_HARM),r.hits().stream().map(PillarRelationsCalculator.Hit::kind).toList());
        var hit=r.hits().getFirst();assertEquals("寅巳申",hit.pattern());assertTrue(hit.directed());
        assertEquals(List.of("year","month"),hit.participants().stream().map(PillarRelationsCalculator.Participant::pillarKey).toList());
        assertThrows(UnsupportedOperationException.class,() -> r.hits().clear());
        assertThrows(UnsupportedOperationException.class,() -> r.rules().clear());
        assertThrows(UnsupportedOperationException.class,() -> hit.participants().clear());
    }
    @Test void knownBirthRetainsNativeCombinationAndBreakAlongsideDirectedPunishment() {
        var r=new BaziCalculator().calculate(LocalDateTime.of(1994,12,6,2,0)).relations();
        assertEquals(4,r.hits().size());
        assertEquals("土",kind(r,STEM_COMBINATION).getFirst().associatedElement());
        assertEquals("甲己",kind(r,STEM_COMBINATION).getFirst().pattern());
        assertEquals("木",kind(r,BRANCH_SIX_COMBINATION).getFirst().associatedElement());
        assertEquals(List.of("hour","year"),kind(r,BRANCH_PUNISHMENT).getFirst().participants().stream().map(PillarRelationsCalculator.Participant::pillarKey).toList());
        assertEquals(1,kind(r,BRANCH_BREAK).size());
    }
    @Test void solarLunarGenderAndZiBoundaryUseTheSamePillars() {
        var calc=new BaziCalculator();var time=LocalDateTime.of(1986,5,29,13,37);
        var male=calc.calculate(time,BaziCalculator.BirthGender.MALE);
        assertEquals(male.relations(),calc.calculate(time,BaziCalculator.BirthGender.FEMALE).relations());
        assertEquals(male.relations(),calc.calculateLunar(1986,4,21,false,LocalTime.of(13,37)).relations());
        var before=calc.calculate(LocalDateTime.of(1986,5,29,22,59)).relations();
        var after=calc.calculate(LocalDateTime.of(1986,5,29,23,0)).relations();
        assertNotEquals(before,after);assertEquals(after,calc.calculate(LocalDateTime.of(1986,5,30,0,0)).relations());
        assertEquals(4,male.pillarDetails().size());assertEquals(8,male.elementCounts().values().stream().mapToInt(Integer::intValue).sum());
    }
}
