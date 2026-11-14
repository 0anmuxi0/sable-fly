package dev.ryanhcode.sable.mixin.command;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.command.data_accessor.SubLevelDataAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.commands.data.DataCommands.DataProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Restores sub-level support for the {@code /data} command on 26.2.
 *
 * The 26.3 original wrapped {@code List.of(e1, e2, e3)} inside {@code DataCommands.<clinit>} and
 * appended {@code SubLevelDataAccessor.PROVIDER}. {@code SubLevelDataAccessor} is rebuilt against
 * 26.2's {@code Function<String, DataCommands.DataProvider>} shape (26.3's {@code ArgProvider.Factory}
 * does not exist there), so this mixin appends the provider exactly like the 26.3 original.
 */
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
       Object e1, Object e2, Object e3, Operation<ImmutableList<Function<String, DataProvider>>> original
    ) {
       List<Function<String, DataProvider>> providers = (List<Function<String, DataProvider>>)original.call(new Object[]{e1, e2, e3});
       ObjectArrayList<Function<String, DataProvider>> mutableList = new ObjectArrayList<Function<String, DataProvider>>((Collection)providers);
       mutableList.add(SubLevelDataAccessor.PROVIDER);
       return ImmutableList.copyOf((Collection)mutableList);
    }
}
