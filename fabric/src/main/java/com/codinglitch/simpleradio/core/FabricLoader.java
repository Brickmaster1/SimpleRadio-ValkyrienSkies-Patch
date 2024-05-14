package com.codinglitch.simpleradio.core;

import com.codinglitch.simpleradio.CommonSimpleRadio;
import com.codinglitch.simpleradio.core.central.ItemHolder;
import com.codinglitch.simpleradio.core.networking.packets.ClientboundRadioPacket;
import com.codinglitch.simpleradio.core.networking.packets.ServerboundRadioUpdatePacket;
import com.codinglitch.simpleradio.core.registry.SimpleRadioBlockEntities;
import com.codinglitch.simpleradio.core.registry.SimpleRadioBlocks;
import com.codinglitch.simpleradio.core.registry.SimpleRadioItems;
import com.codinglitch.simpleradio.core.registry.SimpleRadioMenus;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.Consumer;
import java.util.function.Function;

public class FabricLoader {
    public static void loadItems() {
        SimpleRadioItems.ITEMS.forEach(((location, item) -> {
            ResourceLocation conditionLocation = location.withSuffix("_enabled");

            ResourceConditions.register(conditionLocation, object -> {
                return item.enabled; // i have no idea if this is how youre supposed to do it
            });

            Registry.register(BuiltInRegistries.ITEM, location, item.get());
        }));
    }

    public static void loadBlocks() {
        SimpleRadioBlocks.BLOCKS.forEach(((location, block) -> Registry.register(BuiltInRegistries.BLOCK, location, block)));
    }

    public static void loadPackets() {
        ServerPlayNetworking.registerGlobalReceiver(ServerboundRadioUpdatePacket.ID,
                serverbound(ServerboundRadioUpdatePacket::decode, ServerboundRadioUpdatePacket::handle));
    }

    public static void loadClientPackets() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundRadioPacket.ID,
                clientbound(ClientboundRadioPacket::decode, ClientboundRadioPacket::handle));
    }

    public static <P> ServerPlayNetworking.PlayChannelHandler serverbound(Function<FriendlyByteBuf, P> decoder, TriConsumer<P, MinecraftServer, ServerPlayer> consumer) {
        return (server, player, _handler, buf, _responseSender) -> consumer.accept(decoder.apply(buf), server, player);
    }
    public static <P> ClientPlayNetworking.PlayChannelHandler clientbound(Function<FriendlyByteBuf, P> decoder, Consumer<P> consumer) {
        return (client, listener, buffer, sender) -> consumer.accept(decoder.apply(buffer));
    }

    private static final ResourceLocation ITEMS_ENABLED = CommonSimpleRadio.id("items_enabled");

    public static ConditionJsonProvider itemsEnabled(String... items) {
        return new ConditionJsonProvider() {
            @Override
            public void writeParameters(JsonObject object) {
                JsonArray array = new JsonArray();
                for (var item : items) {
                    array.add(item);
                }
                object.add("values", array);
            }

            @Override
            public ResourceLocation getConditionId() {
                return ITEMS_ENABLED;
            }
        };
    }

    static {
        ResourceConditions.register(ITEMS_ENABLED, object -> {
            JsonArray array = GsonHelper.getAsJsonArray(object, "values");

            for (JsonElement element : array) {
                if (element.isJsonPrimitive()) {
                    ItemHolder<Item> holder = SimpleRadioItems.getByName(element.getAsString());
                    if (holder != null) return holder.enabled;
                } else {
                    throw new JsonParseException("Invalid item entry: " + element);
                }
            }

            return true;
        });
    }

    public static void load() {
        loadItems();
        loadBlocks();
        loadPackets();

        SimpleRadioBlockEntities.load();
        SimpleRadioMenus.load();
    }
}
