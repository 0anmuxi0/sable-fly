package dev.ryanhcode.sable.mixin.command;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ryanhcode.sable.api.command.SableCommandHelper;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import java.util.ArrayList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ExecuteCommand.class})
public class ExecuteCommandMixin {
   @WrapOperation(
      method = {"register"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;then(Lcom/mojang/brigadier/builder/ArgumentBuilder;)Lcom/mojang/brigadier/builder/ArgumentBuilder;",
         ordinal = 31,
         remap = false
      )}
   )
   private static ArgumentBuilder sable$then(
      LiteralArgumentBuilder var0, ArgumentBuilder var1, Operation<ArgumentBuilder> var2, @Local LiteralCommandNode<CommandSourceStack> var3
   ) {
      return var0.then(var1).then(Commands.literal("in_sub_level").then(Commands.argument("sub_levels", SubLevelArgumentType.subLevels()).fork(var3, var0x -> {
         ArrayList var1x = Lists.newArrayList();
         ServerSubLevelContainer var2x = SubLevelContainer.getContainer(((CommandSourceStack)var0x.getSource()).getLevel());

         for (SubLevel var4 : SubLevelArgumentType.getSubLevels(var0x, "sub_levels")) {
            Pose3d var5 = var4.logicalPose();
            Vec3 var6 = var5.transformPositionInverse(((CommandSourceStack)var0x.getSource()).getPosition());
            BlockPos var7 = BlockPos.containing(var6);
            if (var2x.getPlot(new ChunkPos(SectionPos.blockToSectionCoord(var7.getX()), SectionPos.blockToSectionCoord(var7.getZ()))) != var4.getPlot()) {
               throw SableCommandHelper.ERROR_NOT_INSIDE_SUB_LEVEL.create();
            }

            var1x.add(((CommandSourceStack)var0x.getSource()).withLevel((ServerLevel)var4.getLevel()).withPosition(var6).withRotation(new Vec2(0.0F, 0.0F)));
         }

         return var1x;
      }))).then(Commands.literal("out_sub_level").then(Commands.argument("sub_levels", SubLevelArgumentType.subLevels()).fork(var3, var0x -> {
         ArrayList var1x = Lists.newArrayList();
         ServerSubLevelContainer var2x = SubLevelContainer.getContainer(((CommandSourceStack)var0x.getSource()).getLevel());

         for (SubLevel var4 : SubLevelArgumentType.getSubLevels(var0x, "sub_levels")) {
            Pose3d var5 = var4.logicalPose();
            Vec3 var6 = ((CommandSourceStack)var0x.getSource()).getPosition();
            Vec3 var7 = var5.transformPosition(var6);
            BlockPos var8 = BlockPos.containing(var6);
            if (var2x.getPlot(new ChunkPos(SectionPos.blockToSectionCoord(var8.getX()), SectionPos.blockToSectionCoord(var8.getZ()))) != var4.getPlot()) {
               throw SableCommandHelper.ERROR_NOT_INSIDE_SUB_LEVEL.create();
            }

            var1x.add(((CommandSourceStack)var0x.getSource()).withLevel((ServerLevel)var4.getLevel()).withPosition(var7).withRotation(new Vec2(0.0F, 0.0F)));
         }

         return var1x;
      }))).then(Commands.literal("centered_in_sub_level").then(Commands.argument("sub_levels", SubLevelArgumentType.subLevels()).fork(var3, var0x -> {
         ArrayList var1x = Lists.newArrayList();

         for (SubLevel var3x : SubLevelArgumentType.getSubLevels(var0x, "sub_levels")) {
            LevelPlot var4 = var3x.getPlot();
            Vec3 var5 = Vec3.atCenterOf(var4.getCenterBlock());
            var1x.add(((CommandSourceStack)var0x.getSource()).withLevel((ServerLevel)var3x.getLevel()).withPosition(var5).withRotation(new Vec2(0.0F, 0.0F)));
         }

         return var1x;
      })));
   }
}
