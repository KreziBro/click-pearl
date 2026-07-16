package com.clickpearl.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ClickPearlClient implements ClientModInitializer {
	private static KeyMapping throwKey;

	@Override
	public void onInitializeClient() {
		// Register keybinding (default is Middle Mouse Button, GLFW_MOUSE_BUTTON_MIDDLE is 2)
		throwKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.clickpearl.throw",
			InputConstants.Type.MOUSE,
			GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
			"category.clickpearl.general"
		));

		// Register client tick event to check for key press
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;

			try {
				// Detect if our key was clicked
				if (throwKey.consumeClick()) {
					// Consume conflicting clicks (like Pick Block or Use Item) sharing the same key
					Object options = Class.forName("net.minecraft.class_310").getField("field_1690").get(client);
					Field keysAllField = options.getClass().getField("field_1839");
					Object[] keyMappings = (Object[]) keysAllField.get(options);
					Class<?> keyBindingClass = Class.forName("net.minecraft.class_304");
					Method sameMethod = keyBindingClass.getMethod("method_1436", keyBindingClass);
					Method consumeClickMethod = keyBindingClass.getMethod("method_1431");

					for (Object mapping : keyMappings) {
						if (mapping != throwKey && (boolean) sameMethod.invoke(mapping, throwKey)) {
							while ((boolean) consumeClickMethod.invoke(mapping)) {
								// Consume clicks to clear queue
							}
						}
					}

					// Perform throwing
					throwPearl(client);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private static void throwPearl(Object clientObj) {
		try {
			Class<?> minecraftClass = Class.forName("net.minecraft.class_310");
			Class<?> playerInventoryClass = Class.forName("net.minecraft.class_1661");
			Class<?> itemStackClass = Class.forName("net.minecraft.class_1799");
			Class<?> itemsClass = Class.forName("net.minecraft.class_1802");
			Class<?> handClass = Class.forName("net.minecraft.class_1268");
			Class<?> componentClass = Class.forName("net.minecraft.class_2561");
			Class<?> chatFormattingClass = Class.forName("net.minecraft.class_124");

			Object player = minecraftClass.getField("field_1724").get(clientObj);
			if (player == null) return;

			// Don't throw if player is in a screen (like container, chest, chat, etc.)
			Object currentScreen = minecraftClass.getField("field_1755").get(clientObj);
			if (currentScreen != null) return;

			// Get Ender Pearl item
			Object enderPearlItem = itemsClass.getField("field_8231").get(null);

			// Check cooldown
			Object cooldownManager = player.getClass().getMethod("method_7357").invoke(player);
			Object enderPearlStack = itemStackClass.getConstructor(Class.forName("net.minecraft.class_1935")).newInstance(enderPearlItem);
			
			Method isCoolingDownMethod = null;
			for (Method m : cooldownManager.getClass().getMethods()) {
				if (m.getName().equals("method_7904")) {
					isCoolingDownMethod = m;
					break;
				}
			}

			if (isCoolingDownMethod != null) {
				Class<?> paramType = isCoolingDownMethod.getParameterTypes()[0];
				boolean coolingDown;
				if (paramType.getName().endsWith("class_1799")) {
					coolingDown = (boolean) isCoolingDownMethod.invoke(cooldownManager, enderPearlStack);
				} else {
					coolingDown = (boolean) isCoolingDownMethod.invoke(cooldownManager, enderPearlItem);
				}
				if (coolingDown) return;
			}

			Object offHandEnum = handClass.getField("field_5810").get(null);
			Object mainHandEnum = handClass.getField("field_5808").get(null);

			// Check if holding in offhand (can be used directly)
			Object offHandStack = player.getClass().getMethod("method_6121").invoke(player);
			boolean offHandHasPearl = false;
			if (offHandStack != null && !(boolean) itemStackClass.getMethod("method_7960").invoke(offHandStack)) {
				Object offHandItem = itemStackClass.getMethod("method_7909").invoke(offHandStack);
				if (offHandItem == enderPearlItem) {
					offHandHasPearl = true;
				}
			}

			if (offHandHasPearl) {
				// Send Use Item Packet (offhand)
				sendUseItemPacket(clientObj, offHandEnum);
				// Swing hand
				player.getClass().getMethod("method_6075", handClass).invoke(player, offHandEnum);
				return;
			}

			// Find Ender Pearl in hotbar (slots 0-8)
			Object inventory = player.getClass().getField("field_7514").get(player);
			java.util.List<?> mainInventory = (java.util.List<?>) playerInventoryClass.getField("field_7547").get(inventory);

			int pearlSlot = -1;
			for (int i = 0; i < 9; i++) {
				Object stack = mainInventory.get(i);
				if (stack != null && !(boolean) itemStackClass.getMethod("method_7960").invoke(stack)) {
					Object item = itemStackClass.getMethod("method_7909").invoke(stack);
					if (item == enderPearlItem) {
						pearlSlot = i;
						break;
					}
				}
			}

			if (pearlSlot == -1) {
				// Resolve and format warning message
				Object chatComponent;
				try {
					chatComponent = componentClass.getMethod("method_4347", String.class).invoke(null, "chat.clickpearl.no_pearls");
				} catch (NoSuchMethodException e) {
					chatComponent = Class.forName("net.minecraft.class_2588").getConstructor(String.class).newInstance("chat.clickpearl.no_pearls");
				}

				Object redFormat = chatFormattingClass.getField("field_1061").get(null);
				chatComponent = chatComponent.getClass().getMethod("method_1086", chatFormattingClass).invoke(chatComponent, redFormat);

				// Send message to action bar
				Method sendMessageMethod = null;
				for (Method m : player.getClass().getMethods()) {
					Class<?>[] pTypes = m.getParameterTypes();
					if (pTypes.length == 2 && pTypes[0].isAssignableFrom(componentClass) && pTypes[1] == boolean.class) {
						sendMessageMethod = m;
						break;
					}
				}
				if (sendMessageMethod != null) {
					sendMessageMethod.invoke(player, chatComponent, true);
				}
				return;
			}

			// Fast Swap -> Throw -> Swap Back
			Field selectedSlotField = playerInventoryClass.getField("field_7545");
			int originalSlot = (int) selectedSlotField.get(inventory);

			// Swap client slot
			selectedSlotField.set(inventory, pearlSlot);

			// Send packet to swap on server
			Object swapPacket = Class.forName("net.minecraft.class_2868").getConstructor(int.class).newInstance(pearlSlot);
			sendPacket(clientObj, swapPacket);

			// Send packet to use item (throws the pearl)
			sendUseItemPacket(clientObj, mainHandEnum);

			// Swing hand
			player.getClass().getMethod("method_6075", handClass).invoke(player, mainHandEnum);

			// Restore client slot
			selectedSlotField.set(inventory, originalSlot);

			// Send packet to restore on server
			Object restorePacket = Class.forName("net.minecraft.class_2868").getConstructor(int.class).newInstance(originalSlot);
			sendPacket(clientObj, restorePacket);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void sendPacket(Object clientObj, Object packet) throws Exception {
		Class<?> minecraftClass = Class.forName("net.minecraft.class_310");
		Class<?> packetClass = Class.forName("net.minecraft.class_2596");
		Object networkHandler = minecraftClass.getMethod("method_1562").invoke(clientObj);
		if (networkHandler != null) {
			networkHandler.getClass().getMethod("method_2883", packetClass).invoke(networkHandler, packet);
		}
	}

	private static void sendUseItemPacket(Object clientObj, Object handEnum) throws Exception {
		Class<?> handClass = Class.forName("net.minecraft.class_1268");
		Class<?> useItemPacketClass = Class.forName("net.minecraft.class_2886");
		Object useItemPacket;
		try {
			// 1.19+ constructor: (Hand, int)
			Constructor<?> constr = useItemPacketClass.getConstructor(handClass, int.class);
			useItemPacket = constr.newInstance(handEnum, 0);
		} catch (NoSuchMethodException e) {
			// pre-1.19 constructor: (Hand)
			Constructor<?> constr = useItemPacketClass.getConstructor(handClass);
			useItemPacket = constr.newInstance(handEnum);
		}
		sendPacket(clientObj, useItemPacket);
	}
}
