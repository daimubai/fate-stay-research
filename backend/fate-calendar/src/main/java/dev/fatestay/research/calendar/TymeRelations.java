package dev.fatestay.research.calendar;

import com.tyme.sixtycycle.SixtyCycle;

/** Native pair relations from the pinned Tyme4j API; elements describe correspondence, not proven transformation. */
public final class TymeRelations {
    private TymeRelations() {}
    public record Pair(String stemCombineElement, String branchCombineElement, boolean clash, boolean harm) {}
    public static Pair between(String first, String second) {
        var a = SixtyCycle.fromName(first);
        var b = SixtyCycle.fromName(second);
        var stem = a.getHeavenStem().combine(b.getHeavenStem());
        var branch = a.getEarthBranch().combine(b.getEarthBranch());
        return new Pair(stem == null ? null : stem.getName(), branch == null ? null : branch.getName(),
            a.getEarthBranch().getOpposite().equals(b.getEarthBranch()),
            a.getEarthBranch().getHarm().equals(b.getEarthBranch()));
    }
}
