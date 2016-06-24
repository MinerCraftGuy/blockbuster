package noname.blockbuster.recording.actions;

import net.minecraft.util.math.BlockPos;
import noname.blockbuster.entity.EntityActor;

/**
 * Breaking block action
 *
 * Actor breaks the block
 */
public class BreakBlockAction extends InteractBlockAction
{
    public BreakBlockAction()
    {}

    public BreakBlockAction(BlockPos pos)
    {
        super(pos);
    }

    @Override
    public byte getType()
    {
        return Action.BREAK_BLOCK;
    }

    @Override
    public void apply(EntityActor actor)
    {
        actor.worldObj.destroyBlock(this.pos, false);
    }
}