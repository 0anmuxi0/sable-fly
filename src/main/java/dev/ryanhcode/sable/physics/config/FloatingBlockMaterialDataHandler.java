package dev.ryanhcode.sable.physics.config;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.physics.floating_block.FloatingBlockMaterial;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class FloatingBlockMaterialDataHandler {
   public static HashMap<Identifier, FloatingBlockMaterial> allMaterials = new HashMap<>();

   public static void addMaterial(Identifier id, FloatingBlockMaterial material) {
      allMaterials.put(id, material);
   }

   public static void clearMaterials() {
      allMaterials.clear();
   }

   public static class ReloadListener extends SimpleJsonResourceReloadListener<FloatingBlockMaterial> {
      public static final String NAME = "floating_block_material";
      public static final Identifier ID = Sable.sablePath("floating_block_material");
      public static final FloatingBlockMaterialDataHandler.ReloadListener INSTANCE = new FloatingBlockMaterialDataHandler.ReloadListener();

      protected ReloadListener() {
         super(FloatingBlockMaterial.CODEC, FileToIdConverter.json("floating_materials"));
      }

      protected void apply(Map<Identifier, FloatingBlockMaterial> map, ResourceManager resourceManager, ProfilerFiller profiler) {
         FloatingBlockMaterialDataHandler.allMaterials.clear();

         for (Entry<Identifier, FloatingBlockMaterial> entry : map.entrySet()) {
            FloatingBlockMaterialDataHandler.addMaterial(entry.getKey(), entry.getValue());
         }
      }
   }
}
