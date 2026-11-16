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
   public static final Function<String, DataProvider> PROVIDER = argumentName -> new DataProvider() {
      @Override
      public DataAccessor access(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
         return new SubLevelDataAccessor(SubLevelArgumentType.getSingleSubLevel(context, argumentName));
      }

      @Override
      public ArgumentBuilder<CommandSourceStack, ?> wrap(
         ArgumentBuilder<CommandSourceStack, ?> builder, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> action
      ) {
         return builder.then(Commands.literal("sub_level").then((ArgumentBuilder)action.apply(Commands.argument(argumentName, SubLevelArgumentType.singleSubLevel()))));
      }
   };
   private final ServerSubLevel subLevel;

   public SubLevelDataAccessor(ServerSubLevel subLevel) {
      this.subLevel = subLevel;
   }

   @Override
   public void setData(CompoundTag tag) {
      this.subLevel.setUserDataTag(tag);
   }

   @Override
   public CompoundTag getData() {
      CompoundTag tag = this.subLevel.getUserDataTag();
      return tag != null ? tag : new CompoundTag();
   }

   @Override
   public Component getModifiedSuccess() {
      return Component.translatable("commands.data.sub_level.modified", this.subLevel.toString());
   }

   @Override
   public Component getPrintSuccess(Tag tag) {
      return Component.translatable("commands.data.sub_level.query", this.subLevel.toString(), NbtUtils.toPrettyComponent(tag));
   }

   @Override
   public Component getPrintSuccess(NbtPath path, double scale, int integer) {
      return Component.translatable("commands.data.sub_level.get", path.asString(), this.subLevel.toString(), String.format(Locale.ROOT, "%.2f", scale), integer);
   }
}
