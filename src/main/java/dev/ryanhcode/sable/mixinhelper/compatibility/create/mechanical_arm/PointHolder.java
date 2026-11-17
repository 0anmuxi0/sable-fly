package dev.ryanhcode.sable.mixinhelper.compatibility.create.mechanical_arm;

import com.zurrtum.create.catnip.nbt.NBTHelper;
import com.zurrtum.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.lang3.mutable.MutableBoolean;

public record PointHolder(BlockPos pos, ArmInteractionPoint.Mode interactionMode, MutableBoolean covered) {
    public CompoundTag serialize(BlockPos anchor) {
        CompoundTag tag = new CompoundTag();
        BlockPos offset = this.pos.subtract(anchor);
        tag.putIntArray("pos", new int[]{offset.getX(), offset.getY(), offset.getZ()});
        NBTHelper.writeEnum(tag, "mode", this.interactionMode);
        return tag;
    }

    public static PointHolder deserialize(CompoundTag tag, BlockPos anchor) {
        int[] offset = tag.getIntArray("pos").orElse(null);
        if (offset == null || offset.length != 3) {
            return null;
        }
        BlockPos localPos = new BlockPos(offset[0], offset[1], offset[2]);
        return new PointHolder(localPos.offset(anchor), NBTHelper.readEnum(tag, "mode", ArmInteractionPoint.Mode.class), new MutableBoolean(false));
    }
}
