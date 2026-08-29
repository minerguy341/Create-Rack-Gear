package com.minerguy341.rackgear.registry;

import com.minerguy341.rackgear.CreateRackGear;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's creative tab. Contents are filled in by Registrate for every entry built after
 * {@code CreateRegistrate#setCreativeTab} is called, so the display item list stays empty here.
 */
public class RackGearCreativeTab {

	private static final DeferredRegister<CreativeModeTab> TABS =
		DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateRackGear.ID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
		() -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup." + CreateRackGear.ID))
			.icon(() -> RackGearBlocks.RACK_PINION.asStack())
			.displayItems((parameters, output) -> {
			})
			.build());

	public static void register(IEventBus modEventBus) {
		TABS.register(modEventBus);
	}
}
