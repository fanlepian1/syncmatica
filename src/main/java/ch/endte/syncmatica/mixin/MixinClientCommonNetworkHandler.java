package ch.endte.syncmatica.mixin;

import ch.endte.syncmatica.Reference;
import ch.endte.syncmatica.network.ChannelManager;
import ch.endte.syncmatica.network.SyncmaticaPacket;
import ch.endte.syncmatica.network.handler.ClientPlayHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;

@Mixin(ClientCommonNetworkHandler.class)
public class MixinClientCommonNetworkHandler
{
    @Inject(method = "onCustomPayload", at = @At("HEAD"), cancellable = true)
    private void syncmatica$handlePacket(CustomPayloadS2CPacket packet, CallbackInfo ci)
    {
        if (packet.payload().getId().id().getNamespace().equals(Reference.MOD_ID)||packet.getPacketId().id().equals(ChannelManager.MINECRAFT_REGISTER))
        {
            SyncmaticaPacket.Payload payload = (SyncmaticaPacket.Payload) packet.payload();
            Object thiss = this;

            if (thiss instanceof ClientPlayNetworkHandler handler)
            {
                ClientPlayHandler.decodeSyncData(payload.data(), handler);
            }
            else
            {
                ClientPlayHandler.receiveSyncPayload(payload.data());
            }

            // Cancel unnecessary processing if a PacketType we own is caught
            if  (ci.isCancellable())
                ci.cancel();

        }
    }
}
