package com.codinglitch.simpleradio.core;

import com.codinglitch.simpleradio.CommonSimpleRadio;
import com.codinglitch.simpleradio.core.networking.packets.ClientboundRadioPacket;
import com.codinglitch.simpleradio.core.networking.packets.ServerboundRadioUpdatePacket;
import com.codinglitch.simpleradio.core.registry.SimpleRadioBlockEntities;
import com.codinglitch.simpleradio.core.registry.SimpleRadioBlocks;
import com.codinglitch.simpleradio.core.registry.SimpleRadioItems;
import com.codinglitch.simpleradio.core.registry.SimpleRadioMenus;
import com.codinglitch.simpleradio.datagen.SimpleRadioRecipeProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = CommonSimpleRadio.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeLoader {
    public static final SimpleChannel CHANNEL = ChannelBuilder.named(new ResourceLocation(CommonSimpleRadio.ID,"channel"))
            .optional()
            .networkProtocolVersion(0)
            .simpleChannel();

    @SubscribeEvent
    public void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();

        generator.addProvider(
                event.includeServer(),
                new SimpleRadioRecipeProvider(generator.getPackOutput())
        );
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ITEMS, helper -> SimpleRadioItems.ITEMS.forEach((location, itemHolder) -> helper.register(location, itemHolder.get())));
        event.register(ForgeRegistries.Keys.BLOCKS, helper -> SimpleRadioBlocks.BLOCKS.forEach((helper::register)));
        event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, helper -> SimpleRadioBlockEntities.BLOCK_ENTITIES.forEach((helper::register)));
        event.register(ForgeRegistries.Keys.MENU_TYPES, helper -> SimpleRadioMenus.MENUS.forEach(helper::register));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> SimpleRadioMenus.CREATIVE_TABS.forEach(helper::register));

        event.register(ForgeRegistries.Keys.CONDITION_SERIALIZERS, helper -> {
            helper.register(CommonSimpleRadio.id("items_enabled"), ItemsEnabledCondition.CODEC);
        });
    }

    public static void loadPackets() {
        CHANNEL.messageBuilder(ServerboundRadioUpdatePacket.class).decoder(ServerboundRadioUpdatePacket::decode).encoder(ServerboundRadioUpdatePacket::encode)
                .consumerMainThread(serverbound(ServerboundRadioUpdatePacket::handle)).add();

        CHANNEL.messageBuilder(ClientboundRadioPacket.class).decoder(ClientboundRadioPacket::decode).encoder(ClientboundRadioPacket::encode)
                .consumerMainThread(clientbound(ClientboundRadioPacket::handle)).add();
    }

    private static <P> BiConsumer<P, CustomPayloadEvent.Context> serverbound(TriConsumer<P, MinecraftServer, ServerPlayer> consumer) {
        return (packet, context) -> {
            consumer.accept(packet, context.getSender().getServer(), context.getSender());
            context.setPacketHandled(true);
        };
    }
    public static <P> BiConsumer<P, CustomPayloadEvent.Context> clientbound(Consumer<P> consumer) {
        return (packet, context) -> {
            consumer.accept(packet);
            context.setPacketHandled(true);
        };
    }

    public static void loadItems() {

    }

    public static void load() {
        loadItems();
        loadPackets();
    }

    public static void loadClient() {
    }
}
