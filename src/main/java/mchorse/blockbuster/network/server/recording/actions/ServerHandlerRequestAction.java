package mchorse.blockbuster.network.server.recording.actions;

import mchorse.blockbuster.common.CommonProxy;
import mchorse.blockbuster.network.Dispatcher;
import mchorse.blockbuster.network.common.recording.actions.PacketActions;
import mchorse.blockbuster.network.common.recording.actions.PacketRequestAction;
import mchorse.blockbuster.network.server.ServerMessageHandler;
import mchorse.blockbuster.recording.data.Record;
import net.minecraft.entity.player.EntityPlayerMP;

public class ServerHandlerRequestAction extends ServerMessageHandler<PacketRequestAction>
{
    @Override
    public void run(EntityPlayerMP player, PacketRequestAction message)
    {
        Record record = null;

        try
        {
            record = CommonProxy.manager.getRecord(message.filename);
        }
        catch (Exception e)
        {}

        if (record != null)
        {
            Dispatcher.sendTo(new PacketActions(message.filename, record.actions), player);
        }
    }
}