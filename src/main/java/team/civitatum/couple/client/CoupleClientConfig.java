package team.civitatum.couple.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CoupleClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CARRY_REQUESTS, KISS_REQUESTS, SOUND, PARTICLES;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        CARRY_REQUESTS = b.define("allowCarryRequests", true);
        KISS_REQUESTS = b.define("allowKissRequests", true);
        SOUND = b.define("kissSound", true);
        PARTICLES = b.define("kissParticles", true);
        SPEC = b.build();
    }
    private CoupleClientConfig() {}
}
