package mchorse.blockbuster.recording.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mchorse.blockbuster.Blockbuster;
import mchorse.blockbuster.common.entity.EntityActor;
import mchorse.blockbuster.recording.actions.Action;
import mchorse.blockbuster.recording.actions.MountingAction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * This class stores actions and frames states for a recording (to be played
 * back or while recording).
 *
 * There's two list arrays in this class, index in both of these arrays
 * represents the frame position (0 is first frame). Frames list is always
 * populated, but actions list will contain some nulls.
 */
public class Record
{
    /**
     * Signature of the recording. If the first short of the record file isn't
     * this file, then the
     */
    public static final short SIGNATURE = 148;

    /**
     * Filename of this record
     */
    public String filename = "";

    /**
     * Version of this record
     */
    public short version = SIGNATURE;

    /**
     * Delay between recording frames
     */
    public int delay = 1;

    /**
     * Pre-delay same thing as post-delay but less useful
     */
    public int preDelay = 0;

    /**
     * Post-delay allows actors to stay longer on the screen before 
     * whoosing into void
     */
    public int postDelay = 0;

    /**
     * Recorded actions
     */
    public List<List<Action>> actions = new ArrayList<List<Action>>();

    /**
     * Recorded frames
     */
    public List<Frame> frames = new ArrayList<Frame>();

    /**
     * Player data which was recorded when player started recording 
     */
    public NBTTagCompound playerData;

    /**
     * Unload timer. Used only on server side.
     */
    public int unload;

    /**
     * Whether this record has changed elements
     */
    public boolean dirty;

    public Record(String filename)
    {
        this.filename = filename;
        this.delay = Blockbuster.proxy.config.recording_delay;
        this.resetUnload();
    }

    /**
     * Get the length of this record in frames/ticks
     */
    public int getLength()
    {
        return Math.max(this.actions.size(), this.frames.size());
    }

    /**
     * Get an action by given tick and index 
     */
    public Action getAction(int tick, int index)
    {
        if (tick >= 0 && tick < this.actions.size())
        {
            List<Action> actions = this.actions.get(tick);

            if (actions != null && index >= 0 && index < actions.size())
            {
                return actions.get(index);
            }
        }

        return null;
    }

    /**
     * Reset unloading timer
     */
    public void resetUnload()
    {
        this.unload = Blockbuster.proxy.config.record_unload_time;
    }

    /**
     * Apply a frame at given tick on the given actor. Don't pass tick value
     * less than 0, otherwise you might experience <s>tranquility</s> game
     * crash.
     */
    public void applyFrame(int tick, EntityLivingBase actor, boolean force)
    {
        if (tick >= this.frames.size() || tick < 0)
        {
            return;
        }

        this.frames.get(tick).apply(actor, force);

        if (tick != 0)
        {
            Frame prev = this.frames.get(tick - 1);

            /* Override fall distance, apparently fallDistance gets reset
             * faster than RecordRecorder can record both onGround and
             * fallDistance being correct for player, so we just hack */
            actor.fallDistance = prev.fallDistance;
        }
    }

    /**
     * Apply an action at the given tick on the given actor. Don't pass tick
     * value less than 0, otherwise you might experience game crash.
     */
    public void applyAction(int tick, EntityLivingBase actor)
    {
        if (tick >= this.actions.size() || tick < 0)
        {
            return;
        }

        List<Action> actions = this.actions.get(tick);

        if (actions != null)
        {
            for (Action action : actions)
            {
                action.apply(actor);
            }
        }
    }

    /**
     * Reset the actor based on this record
     */
    public void reset(EntityLivingBase actor)
    {
        if (actor.isRiding())
        {
            this.resetMount(actor);
        }

        if (actor.getHealth() > 0.0F)
        {
            this.applyFrame(0, actor, true);

            /* Reseting actor's state */
            actor.setSneaking(false);
            actor.setSprinting(false);
            actor.setItemStackToSlot(EntityEquipmentSlot.HEAD, null);
            actor.setItemStackToSlot(EntityEquipmentSlot.CHEST, null);
            actor.setItemStackToSlot(EntityEquipmentSlot.LEGS, null);
            actor.setItemStackToSlot(EntityEquipmentSlot.FEET, null);
            actor.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, null);
            actor.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, null);
        }
    }

    /**
     * Reset actor's mount
     */
    protected void resetMount(EntityLivingBase actor)
    {
        int index = -1;

        /* Find at which tick player has mounted a vehicle */
        for (int i = 0, c = this.actions.size(); i < c; i++)
        {
            List<Action> actions = this.actions.get(i);

            if (actions == null)
            {
                continue;
            }

            for (Action action : actions)
            {
                if (action instanceof MountingAction)
                {
                    MountingAction act = (MountingAction) action;

                    if (act.isMounting)
                    {
                        index = i + 1;
                        break;
                    }
                }
            }
        }

        actor.dismountRidingEntity();

        if (index != -1)
        {
            Frame frame = this.frames.get(index);

            if (frame != null)
            {
                Entity mount = actor.getRidingEntity();

                if (mount != null && !(mount instanceof EntityActor))
                {
                    mount.setPositionAndRotation(frame.x, frame.y, frame.z, frame.yaw, frame.pitch);
                }
            }
        }
    }

    /**
     * Add an action to the record
     */
    public void addAction(int tick, Action action)
    {
        List<Action> actions = this.actions.get(tick);

        if (actions != null)
        {
            actions.add(action);
        }
        else
        {
            actions = new ArrayList<Action>();
            actions.add(action);

            this.actions.set(tick, actions);
        }
    }

    /**
     * Add an action to the record
     */
    public void addAction(int tick, int index, Action action)
    {
        List<Action> actions = this.actions.get(tick);

        if (actions != null)
        {
            if (index == -1)
            {
                actions.add(action);
            }
            else
            {
                actions.add(index, action);
            }
        }
        else
        {
            actions = new ArrayList<Action>();
            actions.add(action);

            this.actions.set(tick, actions);
        }
    }

    /**
     * Remove an action at given tick and index 
     */
    public void removeAction(int tick, int index)
    {
        if (index == -1)
        {
            this.actions.set(tick, null);
        }
        else
        {
            List<Action> actions = this.actions.get(tick);

            if (index >= 0 && index < actions.size())
            {
                actions.remove(index);

                if (actions.isEmpty())
                {
                    this.actions.set(tick, null);
                }
            }
        }
    }

    /**
     * Create a copy of this record
     */
    public Record clone()
    {
        Record record = new Record(this.filename);

        record.version = this.version;
        record.delay = this.delay;
        record.preDelay = this.preDelay;
        record.postDelay = this.postDelay;

        for (Frame frame : this.frames)
        {
            record.frames.add(frame.clone());
        }

        for (List<Action> actions : this.actions)
        {
            if (actions == null || actions.isEmpty())
            {
                record.actions.add(null);
            }
            else
            {
                List<Action> newActions = new ArrayList<Action>();

                for (Action action : actions)
                {
                    try
                    {
                        NBTTagCompound tag = new NBTTagCompound();

                        action.toNBT(tag);
                        tag.setByte("Type", action.getType());
                        newActions.add(this.actionFromNBT(tag));
                    }
                    catch (Exception e)
                    {
                        System.out.println("Failed to clone an action!");
                        e.printStackTrace();
                    }
                }

                record.actions.add(newActions);
            }
        }

        return record;
    }

    /**
     * Save a recording to given file.
     *
     * This method basically writes the signature of the current version,
     * and then saves all available frames and actions.
     */
    public void save(File file) throws IOException
    {
        NBTTagCompound compound = new NBTTagCompound();
        NBTTagList frames = new NBTTagList();

        /* Version of the recording */
        compound.setShort("Version", SIGNATURE);
        compound.setByte("Delay", (byte) this.delay);
        compound.setInteger("PreDelay", this.preDelay);
        compound.setInteger("PostDelay", this.postDelay);

        if (this.playerData != null)
        {
            compound.setTag("PlayerData", this.playerData);
        }

        int c = this.frames.size();
        int d = this.actions.size() - this.frames.size();

        if (d < 0) d = 0;

        for (int i = 0; i < c; i++)
        {
            NBTTagCompound frameTag = new NBTTagCompound();

            Frame frame = this.frames.get(i);
            List<Action> actions = null;

            if (d + i <= this.actions.size() - 1)
            {
                actions = this.actions.get(d + i);
            }

            frame.toNBT(frameTag);

            if (actions != null)
            {
                NBTTagList actionsTag = new NBTTagList();

                for (Action action : actions)
                {
                    NBTTagCompound actionTag = new NBTTagCompound();

                    action.toNBT(actionTag);
                    actionTag.setByte("Type", action.getType());
                    actionsTag.appendTag(actionTag);
                }

                frameTag.setTag("Action", actionsTag);
            }

            frames.appendTag(frameTag);
        }

        compound.setTag("Frames", frames);

        CompressedStreamTools.writeCompressed(compound, new FileOutputStream(file));
    }

    /**
     * Read a recording from given file.
     *
     * This method basically checks if the given file has appropriate short
     * signature, and reads all frames and actions from the file.
     */
    public void load(File file) throws IOException
    {
        NBTTagCompound compound = CompressedStreamTools.readCompressed(new FileInputStream(file));

        this.version = compound.getShort("Version");
        this.delay = compound.getByte("Delay");
        this.preDelay = compound.getInteger("PreDelay");
        this.postDelay = compound.getInteger("PostDelay");

        if (compound.hasKey("PlayerData", 10))
        {
            this.playerData = compound.getCompoundTag("PlayerData");
        }

        NBTTagList frames = (NBTTagList) compound.getTag("Frames");

        for (int i = 0, c = frames.tagCount(); i < c; i++)
        {
            NBTTagCompound frameTag = frames.getCompoundTagAt(i);
            NBTBase actionTag = frameTag.getTag("Action");
            Frame frame = new Frame();

            frame.fromNBT(frameTag);

            if (actionTag != null)
            {
                try
                {
                    List<Action> actions = new ArrayList<Action>();

                    if (actionTag instanceof NBTTagCompound)
                    {
                        actions.add(this.actionFromNBT((NBTTagCompound) actionTag));
                    }
                    else if (actionTag instanceof NBTTagList)
                    {
                        NBTTagList list = (NBTTagList) actionTag;

                        for (int ii = 0, cc = list.tagCount(); ii < cc; ii++)
                        {
                            actions.add(this.actionFromNBT(list.getCompoundTagAt(ii)));
                        }
                    }

                    this.actions.add(actions);
                }
                catch (Exception e)
                {
                    System.out.println("Failed to load an action at frame " + i);
                    e.printStackTrace();
                }
            }
            else
            {
                this.actions.add(null);
            }

            this.frames.add(frame);
        }
    }

    private Action actionFromNBT(NBTTagCompound tag) throws Exception
    {
        byte type = tag.getByte("Type");
        Action action = null;

        action = Action.fromType(type);
        action.fromNBT(tag);

        return action;
    }
}