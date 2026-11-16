package dev.ryanhcode.sable;

import dev.ryanhcode.sable.sublevel.render.SubLevelRenderer;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class SableClientConfig {
   public static final ModConfigSpec SPEC;
   public static final BooleanValue ATTEMPT_UDP_NETWORKING;
   public static final BooleanValue DEBUG_DRAW_LOADED_CHUNKS;
   public static final BooleanValue SUB_LEVEL_OCCLUSION_CULLING;
   public static final BooleanValue SUB_LEVEL_CULL_ENCLOSED_BLOCKS;
   public static final BooleanValue SUB_LEVEL_BATCHED_SECTIONS;
   public static final DoubleValue SUB_LEVEL_RENDER_DISTANCE;
   public static final DoubleValue INTERPOLATION_DELAY;
   public static final EnumValue<SubLevelRenderer.SelectedRenderer> SELECTED_RENDERER;
   public static final DoubleValue ZOOM_SENSITIVITY;

   @Internal
   public static void onUpdate(boolean notify) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft != null) {
         minecraft.execute(() -> SubLevelRenderer.setImpl(SELECTED_RENDERER.get()));
      }
   }

   static {
      Builder builder = new Builder();
      DEBUG_DRAW_LOADED_CHUNKS = builder.comment("Whether to draw loaded chunks on the client in the chunk debug renderer")
         .define("debug_draw_loaded_chunks", false);
      SUB_LEVEL_OCCLUSION_CULLING = builder.comment(
            "Whether to cull sub-level sections that are hidden behind other sections",
            "Sections are traversed from the camera like vanilla's section occlusion graph;",
            "a section is only drawn if it is reachable through non-opaque section faces."
         )
         .define("sub_level_occlusion_culling", true);
      SUB_LEVEL_CULL_ENCLOSED_BLOCKS = builder.comment(
            "Whether to skip sub-level blocks that are completely enclosed by opaque neighbours",
            "(invisible from the outside, typically the interior of solid structures)"
         )
         .define("sub_level_cull_enclosed_blocks", true);
      SUB_LEVEL_BATCHED_SECTIONS = builder.comment(
            "Whether to draw sub-level sections in one batch per sub-level through the terrain pipeline.",
            "This is the fast path: compiled section meshes are submitted once instead of one moving",
            "block per block each frame. Turn off to fall back to per-block submission."
         )
         .define("sub_level_batched_sections", true);
      SUB_LEVEL_RENDER_DISTANCE = builder.comment("Maximum render distance (in blocks) for sub-levels.", "Set to -1 to disable distance culling.")
         .defineInRange("sub_level_render_distance", -1.0, -1.0, Double.MAX_VALUE);
      INTERPOLATION_DELAY = builder.comment("The distance back in game-ticks that the snapshot interpolation should operate")
         .defineInRange("sub_level_snapshot_interpolation_delay_ticks", 1.5, 0.0, 100.0);
      SELECTED_RENDERER = builder.comment("The renderer to use for sub-levels")
         .defineEnum(
            "sub_level_renderer",
            SubLevelRenderer.DEFAULT,
            Arrays.stream(SubLevelRenderer.SelectedRenderer.values())
               .filter(SubLevelRenderer.SelectedRenderer::isSupported)
               .toArray(SubLevelRenderer.SelectedRenderer[]::new)
         );
      ZOOM_SENSITIVITY = builder.comment("The zoom sensitivity for sub-level camera types").defineInRange("sub_level_zoom_sensitivity", 0.2, 0.0, 100.0);
      ATTEMPT_UDP_NETWORKING = builder.comment(
            "If Sable should attempt to establish a UDP connection with the server, to receive sub-level movement data over a UDP channel"
         )
         .define("attempt_udp_networking", true);
      SPEC = builder.build();
   }
}
