package dev.ryanhcode.sable.mixin.command;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.command.data_accessor.SubLevelDataAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.server.commands.ArgProvider.Factory;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({DataCommands.class})
public class DataCommandsMixin {
   @WrapOperation(require = 0, method = {"<clinit>"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/List;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;"
      )}
   )
   private static List<Factory<DataAccessor>> sable$allProviders(Object e1, Object e2, Object e3, Operation<List<Factory<DataAccessor>>> original) {
      List<Factory<DataAccessor>> providers = original.call(e1, e2, e3);
      ObjectArrayList<Factory<DataAccessor>> mutableList = new ObjectArrayList(providers);
      mutableList.add(SubLevelDataAccessor.PROVIDER);
      return List.copyOf(mutableList);
   }
}
