package dev.ryanhcode.sable.mixin.plot.lighting;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ClientPacketListener.class})
public class ClientPacketListenerMixin {
   @WrapOperation(
      method = {"handleLevelChunkWithLight"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/ClientLevel;queueLightUpdate(Ljava/lang/Runnable;)V"
      )}
   )
   private void sable$queueLightData(ClientLevel var1, Runnable var2, Operation<Void> var3, @Local(argsOnly = true) ClientboundLevelChunkWithLightPacket var4) {
      ClientSubLevelContainer var5 = SubLevelContainer.getContainer(var1);
      if (var5.inBounds(var4.getX(), var4.getZ())) {
         var2.run();
         var1.getLightEngine().runLightUpdates();
      } else {
         var3.call(var1, var2);
      }
   }
}
