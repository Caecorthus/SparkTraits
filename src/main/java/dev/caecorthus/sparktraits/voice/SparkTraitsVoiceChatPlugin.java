package dev.caecorthus.sparktraits.voice;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.packets.Packet;
import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionTraitService;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Simple Voice Chat bridge for Depression psycho silence.
 * 抑郁疯魔期间双向静音。
 */
public class SparkTraitsVoiceChatPlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return SparkTraits.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        // Cancel before Wathe can relay walkie-talkie audio manually.
        // 必须先于 Wathe 取消事件，避免对讲机绕过抑郁疯魔的静音。
        registration.registerEvent(
                MicrophonePacketEvent.class,
                this::blockDepressionSpeaker,
                Integer.MAX_VALUE
        );
        registration.registerEvent(EntitySoundPacketEvent.class, this::blockDepressionListener);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::blockDepressionListener);
        VoicechatPlugin.super.registerEvents(registration);
    }

    private void blockDepressionSpeaker(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null
                || event.getSenderConnection().getPlayer() == null
                || event.getSenderConnection().getPlayer().getPlayer() == null) {
            return;
        }
        ServerPlayerEntity speaker = (ServerPlayerEntity) event.getSenderConnection().getPlayer().getPlayer();
        if (DepressionTraitService.shouldMuteVoice(speaker)) {
            event.cancel();
        }
    }

    private <T extends Packet> void blockDepressionListener(SoundPacketEvent<T> event) {
        if (event.getReceiverConnection() == null
                || event.getReceiverConnection().getPlayer() == null
                || event.getReceiverConnection().getPlayer().getPlayer() == null) {
            return;
        }
        ServerPlayerEntity listener = (ServerPlayerEntity) event.getReceiverConnection().getPlayer().getPlayer();
        if (DepressionTraitService.shouldMuteVoice(listener)) {
            event.cancel();
        }
    }
}
