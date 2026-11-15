package dev.ryanhcode.sable;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class SableServerConfig {
   public static final ModConfigSpec SPEC;
   public static final IntValue SUB_LEVEL_SUBSTEPS_PER_TICK;

   static {
      Builder builder = new Builder();
      SUB_LEVEL_SUBSTEPS_PER_TICK = builder.comment(
            "How many times the physics simulation is stepped in every second. Higher values will be significantly more performance intensive, but will have higher accuracy."
         )
         .defineInRange("sub_level_substeps_per_tick", 2, 1, 10);
      SPEC = builder.build();
   }
}
