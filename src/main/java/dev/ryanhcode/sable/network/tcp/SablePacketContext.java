package dev.ryanhcode.sable.network.tcp;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface SablePacketContext {
   Level level();

   Player player();
}
