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
      LiteralArgumentBuilder instance, ArgumentBuilder argumentBuilder, Operation<ArgumentBuilder> original, @Local LiteralCommandNode<CommandSourceStack> literalCommandNode
   ) {
      return instance.then(argumentBuilder).then(Commands.literal("in_sub_level").then(Commands.argument("sub_levels", SubLevelArgumentType.subLevels()).fork(literalCommandNode, commandContext -> {
         ArrayList list = Lists.newArrayList();
         ServerSubLevelContainer container = SubLevelContainer.getContainer(((CommandSourceStack)commandContext.getSource()).getLevel());

         for (SubLevel subLevel : SubLevelArgumentType.getSubLevels(commandContext, "sub_levels")) {
            Pose3d pose = subLevel.logicalPose();
            Vec3 localPos = pose.transformPositionInverse(((CommandSourceStack)commandContext.getSource()).getPosition());
            BlockPos blockPos = BlockPos.containing(localPos);
            if (container.getPlot(new ChunkPos(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()))) != subLevel.getPlot()) {
               throw SableCommandHelper.ERROR_NOT_INSIDE_SUB_LEVEL.create();
            }

            list.add(((CommandSourceStack)commandContext.getSource()).withLevel((ServerLevel)subLevel.getLevel()).withPosition(localPos).withRotation(new Vec2(0.0F, 0.0F)));
         }

         return list;
      }))).then(Commands.literal("out_sub_level").then(Commands.argument("sub_levels", SubLevelArgumentType.subLevels()).fork(literalCommandNode, commandContext -> {
         ArrayList list = Lists.newArrayList();
         ServerSubLevelContainer container = SubLevelContainer.getContainer(((CommandSourceStack)commandContext.getSource()).getLevel());

         for (SubLevel subLevel : SubLevelArgumentType.getSubLevels(commandContext, "sub_levels")) {
            Pose3d pose = subLevel.logicalPose();
            Vec3 sourcePosition = ((CommandSourceStack)commandContext.getSource()).getPosition();
            Vec3 globalPos = pose.transformPosition(sourcePosition);
            BlockPos sourceBlockPos = BlockPos.containing(sourcePosition);
            if (container.getPlot(new ChunkPos(SectionPos.blockToSectionCoord(sourceBlockPos.getX()), SectionPos.blockToSectionCoord(sourceBlockPos.getZ()))) != subLevel.getPlot()) {
               throw SableCommandHelper.ERROR_NOT_INSIDE_SUB_LEVEL.create();
            }

            list.add(((CommandSourceStack)commandContext.getSource()).withLevel((ServerLevel)subLevel.getLevel()).withPosition(globalPos).withRotation(new Vec2(0.0F, 0.0F)));
         }

         return list;
      }))).then(Commands.literal("centered_in_sub_level").then(Commands.argument("sub_levels", SubLevelArgumentType.subLevels()).fork(literalCommandNode, commandContext -> {
         ArrayList list = Lists.newArrayList();

         for (SubLevel subLevel : SubLevelArgumentType.getSubLevels(commandContext, "sub_levels")) {
            LevelPlot plot = subLevel.getPlot();
            Vec3 center = Vec3.atCenterOf(plot.getCenterBlock());
            list.add(((CommandSourceStack)commandContext.getSource()).withLevel((ServerLevel)subLevel.getLevel()).withPosition(center).withRotation(new Vec2(0.0F, 0.0F)));
         }

         return list;
      })));
   }
}
