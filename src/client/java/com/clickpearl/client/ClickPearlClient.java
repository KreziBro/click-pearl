package com.clickpearl.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class ClickPearlClient implements ClientModInitializer {
	private static KeyMapping throwKey;

	@Override
	public void onInitializeClient() {
		// Регистрируем кейбинд (по умолчанию: средняя кнопка мыши / СКМ)
		throwKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.clickpearl.throw",
			InputConstants.Type.MOUSE,
			GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
			"category.clickpearl.general"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;

			if (throwKey.consumeClick()) {
				// Подавляем конфликтующие биндинги на той же кнопке (например, Pick Block)
				for (KeyMapping mapping : client.options.keyMappings) {
					if (mapping != throwKey && throwKey.same(mapping)) {
						while (mapping.consumeClick()) {
							// очищаем очередь кликов
						}
					}
				}
				throwPearl(client);
			}
		});
	}

	private void throwPearl(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.gameMode == null || client.getConnection() == null) return;

		// Не бросаем если открыт инвентарь/контейнер
		if (client.screen != null) return;

		// Не бросаем если жемчуг на кулдауне
		if (player.getCooldowns().isOnCooldown(Items.ENDER_PEARL.getDefaultInstance())) return;

		// Проверяем вторую руку
		if (player.getOffhandItem().is(Items.ENDER_PEARL)) {
			client.gameMode.useItem(player, InteractionHand.OFF_HAND);
			player.swing(InteractionHand.OFF_HAND);
			return;
		}

		// Ищем жемчуг в хотбаре (слоты 0-8)
		int pearlSlot = -1;
		for (int i = 0; i < 9; i++) {
			if (player.getInventory().getItem(i).is(Items.ENDER_PEARL)) {
				pearlSlot = i;
				break;
			}
		}

		if (pearlSlot == -1) {
			// Сообщение в action bar если жемчуга нет
			player.displayClientMessage(
				Component.translatable("chat.clickpearl.no_pearls").withStyle(ChatFormatting.RED),
				true
			);
			return;
		}

		int originalSlot = player.getInventory().selected;

		// 1. Переключаемся на слот с жемчугом
		player.getInventory().selected = pearlSlot;
		client.getConnection().send(new ServerboundSetCarriedItemPacket(pearlSlot));

		// 2. Используем предмет (бросок жемчуга)
		client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
		player.swing(InteractionHand.MAIN_HAND);

		// 3. Возвращаем оригинальный слот
		player.getInventory().selected = originalSlot;
		client.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
	}
}
