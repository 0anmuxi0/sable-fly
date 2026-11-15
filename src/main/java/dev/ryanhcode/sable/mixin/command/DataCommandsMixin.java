package dev.ryanhcode.sable.mixin.command;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.command.data_accessor.SubLevelDataAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.commands.data.DataCommands.DataProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({DataCommands.class})
public class DataCommandsMixin {
   @WrapOperation(
      method = {"<clinit>"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/google/common/collect/ImmutableList;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList;"
      )}
   )
   private static ImmutableList<Function<String, DataProvider>> sable$allProviders(
      Object var0, Object var1, Object var2, Operation<ImmutableList<Function<String, DataProvider>>> var3
   ) {
      List var4 = (List)var3.call(var0, var1, var2);
      ObjectArrayList var5 = new ObjectArrayList(var4);
      var5.add(SubLevelDataAccessor.PROVIDER);
      return ImmutableList.copyOf(var5);
   }
}
