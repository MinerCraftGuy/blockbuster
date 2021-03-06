package mchorse.blockbuster.recording.actions;

import io.netty.buffer.ByteBuf;
import mchorse.blockbuster.recording.WorldEventListener;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * Breaking block animation
 *
 * This action is responsible for animating blocks which are about to be
 * broken. This action is recorded in the {@link WorldEventListener}.
 */
public class BreakBlockAnimation extends InteractBlockAction
{
    public int progress;

    public BreakBlockAnimation()
    {}

    public BreakBlockAnimation(BlockPos pos, int progress)
    {
        super(pos);
        this.progress = progress;
    }

    @Override
    public byte getType()
    {
        return Action.BREAK_ANIMATION;
    }

    @Override
    public void apply(EntityLivingBase actor)
    {
        if (this.progress >= 10)
        {
            actor.worldObj.destroyBlock(this.pos, true);
        }
        else
        {
            actor.worldObj.sendBlockBreakProgress(actor.getEntityId(), this.pos, this.progress);
        }
    }

    @Override
    public void fromBuf(ByteBuf buf)
    {
        super.fromBuf(buf);
        this.progress = buf.readInt();
    }

    @Override
    public void toBuf(ByteBuf buf)
    {
        super.toBuf(buf);
        buf.writeInt(this.progress);
    }

    @Override
    public void fromNBT(NBTTagCompound tag)
    {
        super.fromNBT(tag);

        this.progress = tag.getInteger("Progress");
    }

    @Override
    public void toNBT(NBTTagCompound tag)
    {
        super.toNBT(tag);

        tag.setInteger("Progress", this.progress);
    }
}