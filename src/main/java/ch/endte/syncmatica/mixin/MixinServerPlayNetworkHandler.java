package ch.endte.syncmatica.mixin;

import java.util.function.Consumer;
import ch.endte.syncmatica.Context;
import ch.endte.syncmatica.Reference;
import ch.endte.syncmatica.Syncmatica;
import ch.endte.syncmatica.communication.ExchangeTarget;
import ch.endte.syncmatica.communication.ServerCommunicationManager;
import ch.endte.syncmatica.network.ChannelManager;
import ch.endte.syncmatica.network.PacketType;
import ch.endte.syncmatica.network.actor.IServerPlay;
import ch.endte.syncmatica.network.handler.ServerPlayHandler;
import ch.endte.syncmatica.network.SyncmaticaPacket;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.listener.TickablePacketListener;
import net.minecraft.server.network.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 1001)
public abstract class MixinServerPlayNetworkHandler extends ServerCommonNetworkHandler implements IServerPlay, ServerPlayPacketListener, PlayerAssociatedNetworkHandler, TickablePacketListener
{
    @Shadow public abstract ServerPlayerEntity getPlayer();
    @Shadow
    public ServerPlayerEntity player;

    @Unique
    private ExchangeTarget exTarget = null;

    @Unique
    private ServerCommunicationManager comManager = null;

    public MixinServerPlayNetworkHandler(MinecraftServer server, ClientConnection connection, ConnectedClientData clientData) {
        super(server, connection, clientData);
    }

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    public void onDisconnected(DisconnectionInfo info, CallbackInfo ci) {
        ChannelManager.onServerPlayerDisconnected(syncmatica$getExchangeTarget());
        syncmatica$operateComms(sm -> sm.onPlayerLeave(syncmatica$getExchangeTarget()));
    }

    @Override
    public void onCustomPayload(CustomPayloadC2SPacket packet) {
        if (packet.payload() instanceof SyncmaticaPacket payload) {
            ChannelManager.onChannelRegisterHandle(syncmatica$getExchangeTarget(), payload.getChannel(), payload.getPacket());
            if (PacketType.containsIdentifier(payload.getChannel())) {
                NetworkThreadUtils.forceMainThread(packet, this, player.getServerWorld());
                syncmatica$operateComms(sm -> sm.onPacket(syncmatica$getExchangeTarget(), payload.getType(), payload.getPacket()));
            }
        }
    }


    @Inject(method = "<init>", at = @At("TAIL"))
    public void syncmatica$onConnect(MinecraftServer server, ClientConnection clientConnection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci)
    {
        syncmatica$operateComms(sm -> sm.onPlayerJoin(syncmatica$getExchangeTarget(), player));
    }

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    public void syncmatica$onDisconnected(DisconnectionInfo info, CallbackInfo ci)
    {
        syncmatica$operateComms(sm -> sm.onPlayerLeave(syncmatica$getExchangeTarget()));
    }

    @Inject(method = "onCustomPayload", at = @At("HEAD"), cancellable = true)
    private void syncmatica$onCustomPayload(CustomPayloadC2SPacket packet, CallbackInfo ci)
    {
        CustomPayload thisPayload = packet.payload();


        if (thisPayload.getId().id().getNamespace().equals(Reference.MOD_ID) || thisPayload.getId().id().equals(ChannelManager.MINECRAFT_REGISTER)) if (thisPayload.getId().id().getNamespace().equals(Reference.MOD_ID) || thisPayload.getId().id().equals(ChannelManager.MINECRAFT_REGISTER))
        {
            SyncmaticaPacket.Payload payload = (SyncmaticaPacket.Payload) thisPayload;
            ServerPlayHandler.decodeSyncData(payload.data(), this);

            // Cancel unnecessary processing if a PacketType we own is caught
            if (ci.isCancellable())
                ci.cancel();

        }
    }

    @Unique
    public void syncmatica$operateComms(final Consumer<ServerCommunicationManager> operation)
    {
        if (comManager == null)
        {
            final Context con = Syncmatica.getContext(Syncmatica.SERVER_CONTEXT);
            if (con != null)
            {
                comManager = (ServerCommunicationManager) con.getCommunicationManager();
            }
        }
        if (comManager != null)
        {
            operation.accept(comManager);
        }
    }

    @Unique
    public ExchangeTarget syncmatica$getExchangeTarget()
    {
        if (exTarget == null)
        {
            exTarget = new ExchangeTarget((ServerPlayNetworkHandler) (Object) this);
        }
        return exTarget;
    }
}
