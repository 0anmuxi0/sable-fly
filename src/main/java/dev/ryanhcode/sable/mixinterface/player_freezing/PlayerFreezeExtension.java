package dev.ryanhcode.sable.mixinterface.player_freezing;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3dc;

public interface PlayerFreezeExtension {
   @Nullable
   UUID sable$getFrozenToSubLevel();

   @Nullable
   Vector3dc sable$getFrozenToSubLevelAnchor();

   void sable$tickStopFreezing();

   void sable$freezeTo(UUID subLevelID, Vector3dc localPosition);

   void sable$teleport();
}
