package dev.ryanhcode.sable.command.data_accessor;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;

/**
 * 26.2 adaptation of the 26.3 original.
 *
 * The 26.3 class declared {@code PROVIDER} as an {@code ArgProvider.Factory<DataAccessor>} against
 * {@code net.minecraft.server.commands.ArgProvider}, a symbol that does not exist on 26.2 (the whole
 * {@code ArgProvider} abstraction was introduced in 26.3). 26.2's equivalent shape is
 * {@code Function<String, DataCommands.DataProvider>} (see {@code DataCommands.ALL_PROVIDERS}), which
 * carries exactly the same {@code access} / {@code wrap} contract, so the factory is rebuilt against
 * it. Behaviour (the {@code /data ... sub_level <sublevel>} target with translated feedback keys) is
 * identical to the 26.3 original.
 */
public class SubLevelDataAccessor implements DataAccessor {
    public static final Function<String, DataCommands.DataProvider> PROVIDER = arg -> new DataCommands.DataProvider() {

        @Override
        public DataAccessor access(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
            return new SubLevelDataAccessor(SubLevelArgumentType.getSingleSubLevel(commandContext, arg));
        }

        @Override
        public ArgumentBuilder<CommandSourceStack, ?> wrap(
            ArgumentBuilder<CommandSourceStack, ?> argumentBuilder,
            Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> function
        ) {
            return argumentBuilder.then(
                Commands.literal("sub_level").then(function.apply(Commands.argument(arg, (ArgumentType)SubLevelArgumentType.singleSubLevel())))
            );
        }
    };
    private final ServerSubLevel subLevel;

    public SubLevelDataAccessor(ServerSubLevel subLevel) {
        this.subLevel = subLevel;
    }

    @Override
    public void setData(CompoundTag compoundTag) {
        this.subLevel.setUserDataTag(compoundTag);
    }

    @Override
    public CompoundTag getData() {
        CompoundTag userTag = this.subLevel.getUserDataTag();
        return userTag != null ? userTag : new CompoundTag();
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
    public Component getPrintSuccess(NbtPathArgument.NbtPath nbtPath, double d, int i) {
        return Component.translatable(
            "commands.data.sub_level.get", nbtPath.asString(), this.subLevel.toString(), String.format(Locale.ROOT, "%.2f", d), i
        );
    }
}
