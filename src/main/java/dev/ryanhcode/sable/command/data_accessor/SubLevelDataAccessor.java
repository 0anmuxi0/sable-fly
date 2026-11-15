package dev.ryanhcode.sable.command.data_accessor;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtPathArgument.NbtPath;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands.DataProvider;

public class SubLevelDataAccessor implements DataAccessor {
   public static final Function<String, DataProvider> PROVIDER = var0 -> new DataProvider() {
      @Override
      public DataAccessor access(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException {
         return new SubLevelDataAccessor(SubLevelArgumentType.getSingleSubLevel(var1, var0));
      }

      @Override
      public ArgumentBuilder<CommandSourceStack, ?> wrap(
         ArgumentBuilder<CommandSourceStack, ?> var1, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> var2
      ) {
         return var1.then(Commands.literal("sub_level").then((ArgumentBuilder)var2.apply(Commands.argument(var0, SubLevelArgumentType.singleSubLevel()))));
      }
   };
   private final ServerSubLevel subLevel;

   public SubLevelDataAccessor(ServerSubLevel var1) {
      this.subLevel = var1;
   }

   @Override
   public void setData(CompoundTag var1) {
      this.subLevel.setUserDataTag(var1);
   }

   @Override
   public CompoundTag getData() {
      CompoundTag var1 = this.subLevel.getUserDataTag();
      return var1 != null ? var1 : new CompoundTag();
   }

   @Override
   public Component getModifiedSuccess() {
      return Component.translatable("commands.data.sub_level.modified", this.subLevel.toString());
   }

   @Override
   public Component getPrintSuccess(Tag var1) {
      return Component.translatable("commands.data.sub_level.query", this.subLevel.toString(), NbtUtils.toPrettyComponent(var1));
   }

   @Override
   public Component getPrintSuccess(NbtPath var1, double var2, int var4) {
      return Component.translatable("commands.data.sub_level.get", var1.asString(), this.subLevel.toString(), String.format(Locale.ROOT, "%.2f", var2), var4);
   }
}
