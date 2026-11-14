package dev.ryanhcode.sable.mixin.punching;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.network.client.ClientSubLevelPunchHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={MultiPlayerGameMode.class})
public class MultiPlayerGameModeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method={"startDestroyBlock"}, at={@At(value="HEAD")})
    private void onBlockBreakStart(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        assert (this.minecraft.player != null);
        HitResult hitResult = this.minecraft.hitResult;
        if (hitResult instanceof BlockHitResult) {
            BlockHitResult blockHitResult = (BlockHitResult)hitResult;
            ClientSubLevelPunchHelper.clientTryPunch(blockHitResult, (Level)this.minecraft.level, true);
        }
    }

    @Inject(method={"useItemOn"}, at={@At(value="RETURN")})
    private void sable$swingOnSubLevelUseOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult.SwingSource swing;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (Sable.HELPER.getContaining((Level)level, (Vec3i)hitResult.getBlockPos()) == null) {
            return;
        }
        InteractionResult result = (InteractionResult)cir.getReturnValue();
        InteractionResult.SwingSource v0;
        if (result instanceof InteractionResult.Success) {
            InteractionResult.Success success = (InteractionResult.Success)result;
            v0 = success.swingSource();
        } else {
            v0 = swing = InteractionResult.SwingSource.NONE;
        }
        swing = v0;
        if (swing != InteractionResult.SwingSource.NONE) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        Item item = stack.getItem();
        if (!(item instanceof BlockItem)) {
            return;
        }
        BlockItem blockItem = (BlockItem)item;
        if (!new BlockPlaceContext((Player)player, hand, stack, hitResult).canPlace()) {
            return;
        }
        BlockPos placedPos = hitResult.getBlockPos().relative(hitResult.getDirection());
        AABB placedBox = new AABB((double)placedPos.getX(), (double)placedPos.getY(), (double)placedPos.getZ(), (double)placedPos.getX() + 1.0, (double)placedPos.getY() + 1.0, (double)placedPos.getZ() + 1.0);
        if (!level.getEntities((Entity)null, placedBox).isEmpty()) {
            return;
        }
        // 26.2 has no ItemStack.getInteractAnimation() (26.3 API returning SwingAnimation);
        // 26.2's swing only has (hand) / (hand, boolean) overloads - use the plain swing.
        player.swing(hand, false);
    }
}

