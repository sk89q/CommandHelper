package com.laytonsmith.abstraction.bukkit.events.drivers;

import com.laytonsmith.abstraction.MCLocation;
import com.laytonsmith.abstraction.bukkit.BukkitMCLocation;
import com.laytonsmith.abstraction.bukkit.events.BukkitServerEvents;
import com.laytonsmith.abstraction.events.MCRedstoneChangedEvent;
import com.laytonsmith.commandhelper.CommandHelperPlugin;
import com.laytonsmith.core.events.Driver;
import com.laytonsmith.core.events.EventUtils;
import com.laytonsmith.core.events.drivers.ServerEvents;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.server.BroadcastMessageEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.Map;

public class BukkitServerListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPing(ServerListPingEvent event) {
		BukkitServerEvents.BukkitMCServerPingEvent pe = new BukkitServerEvents.BukkitMCServerPingEvent(event);
		EventUtils.TriggerListener(Driver.SERVER_PING, "server_ping", pe, CommandHelperPlugin.getCore().getLastLoadedEnv());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		Map<MCLocation, Boolean> locations = ServerEvents.getRedstoneMonitors();
		if(locations.isEmpty()) {
			// Bail as quickly as we can if this isn't being used.
			return;
		}
		final MCLocation blockLocation = new BukkitMCLocation(event.getBlock().getLocation());
		if(locations.containsKey(blockLocation)) {
			// This is a monitored location, so we will be triggering the event.
			boolean wasPowered = locations.get(blockLocation);
			final boolean isPowered = blockLocation.getBlock().isBlockPowered();
			if(wasPowered != isPowered) {
				// It was changed, so set the state appropriately now.
				locations.put(blockLocation, isPowered);
				EventUtils.TriggerListener(Driver.REDSTONE_CHANGED, "redstone_changed", new MCRedstoneChangedEvent() {

					@Override
					public boolean isActive() {
						return isPowered;
					}

					@Override
					public MCLocation getLocation() {
						return blockLocation;
					}

					@Override
					public Object _GetObject() {
						return null;
					}
				}, CommandHelperPlugin.getCore().getLastLoadedEnv());
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBroadcast(BroadcastMessageEvent event) {
		EventUtils.TriggerListener(Driver.BROADCAST_MESSAGE, "broadcast_message",
				new BukkitServerEvents.BukkitMCBroadcastMessageEvent(event), CommandHelperPlugin.getCore().getLastLoadedEnv());
	}
}
