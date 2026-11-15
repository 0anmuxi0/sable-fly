package dev.ryanhcode.sable.fabric.client;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.SableClientConfig;
import dev.ryanhcode.sable.fabric.network.FabricSablePacketContext;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.network.tcp.SableTCPPackets;
import dev.ryanhcode.sable.physics.config.FloatingBlockMaterialDataHandler;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderer;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents.Loading;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents.Reloading;
import fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.PreparableReloadListener.SharedState;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

public final class SableFabricClient implements ClientModInitializer {
   public void onInitializeClient() {
      SableClient.init();
      SableTCPPackets.entries().stream().filter(SableTCPPackets.Entry::clientbound).forEach(SableFabricClient::registerClientboundReceiver);
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> FloatingBlockMaterialDataHandler.clearMaterials());
      ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
         .registerReloadListener(
            new IdentifiableResourceReloadListener() {
               {
                  Objects.requireNonNull(SableFabricClient.this);
               }

               public Identifier getFabricId() {
                  return Sable.sablePath("sub_level_renderer");
               }

               public CompletableFuture<Void> reload(
                  SharedState sharedState, Executor preparationExecutor, PreparationBarrier preparationBarrier, Executor applicationExecutor
               ) {
                  return preparationBarrier.wait(null)
                     .thenRunAsync(() -> SubLevelRenderer.getDispatcher().onResourceManagerReload(sharedState.resourceManager()), applicationExecutor);
               }
            }
         );
      ModConfigEvents.loading("sable").register((Loading)config -> {
         if (config.getSpec().equals(SableClientConfig.SPEC)) {
            SableClientConfig.onUpdate(false);
         }
      });
      ModConfigEvents.reloading("sable").register((Reloading)config -> {
         if (config.getSpec().equals(SableClientConfig.SPEC)) {
            SableClientConfig.onUpdate(true);
         }
      });
      ConfigRegistry.INSTANCE.register("sable", Type.CLIENT, SableClientConfig.SPEC);
      ConfigScreenFactoryRegistry.INSTANCE.register("sable", ConfigurationScreen::new);
   }

   private static <T extends SableTCPPacket> void registerClientboundReceiver(SableTCPPackets.Entry<T> entry) {
      ClientPlayNetworking.registerGlobalReceiver(entry.type(), (payload, context) -> payload.handle(new FabricSablePacketContext(context.player())));
   }
}
