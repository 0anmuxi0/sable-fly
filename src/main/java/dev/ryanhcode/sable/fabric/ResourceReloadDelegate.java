package dev.ryanhcode.sable.fabric;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.PreparableReloadListener.SharedState;

public class ResourceReloadDelegate implements IdentifiableResourceReloadListener {
   private final Identifier id;
   private final SimpleJsonResourceReloadListener delegate;

   public ResourceReloadDelegate(Identifier id, SimpleJsonResourceReloadListener delegate) {
      this.id = id;
      this.delegate = delegate;
   }

   public Identifier getFabricId() {
      return this.id;
   }

   public CompletableFuture<Void> reload(
      SharedState sharedState, Executor preparationExecutor, PreparationBarrier preparationBarrier, Executor applicationExecutor
   ) {
      return this.delegate.reload(sharedState, preparationExecutor, preparationBarrier, applicationExecutor);
   }
}
