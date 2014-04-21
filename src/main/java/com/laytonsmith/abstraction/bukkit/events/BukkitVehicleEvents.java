package com.laytonsmith.abstraction.bukkit.events;

import com.laytonsmith.abstraction.Implementation;
import com.laytonsmith.abstraction.MCEntity;
import com.laytonsmith.abstraction.MCLocation;
import com.laytonsmith.abstraction.MCVehicle;
import com.laytonsmith.abstraction.blocks.MCBlock;
import com.laytonsmith.abstraction.bukkit.BukkitConvertor;
import com.laytonsmith.abstraction.bukkit.BukkitMCLocation;
import com.laytonsmith.abstraction.bukkit.BukkitMCVehicle;
import com.laytonsmith.abstraction.bukkit.blocks.BukkitMCBlock;
import com.laytonsmith.abstraction.enums.MCCollisionType;
import com.laytonsmith.abstraction.events.MCVehicleBlockCollideEvent;
import com.laytonsmith.abstraction.events.MCVehicleEnitityCollideEvent;
import com.laytonsmith.abstraction.events.MCVehicleEnterExitEvent;
import com.laytonsmith.abstraction.events.MCVehicleEvent;
import com.laytonsmith.abstraction.events.MCVehicleMoveEvent;
import com.laytonsmith.annotations.abstraction;
import org.bukkit.Location;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleCollisionEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

/**
 * 
 * @author jb_aero
 */
public class BukkitVehicleEvents {
	
	@abstraction(type = Implementation.Type.BUKKIT)
	public static class BukkitMCVehicleEntityCollideEvent extends BukkitMCVehicleEvent
			implements MCVehicleEnitityCollideEvent {

		VehicleEntityCollisionEvent vec;
		public BukkitMCVehicleEntityCollideEvent(VehicleCollisionEvent event) {
			super(event);
			vec = (VehicleEntityCollisionEvent) event;
		}
		
		@Override
		public MCEntity getEntity() {
			if (vec.getEntity() == null) {
				return null;
			}
			return BukkitConvertor.BukkitGetCorrectEntity(vec.getEntity());
		}

		@Override
		public boolean isCollisionCancelled() {
			return vec.isCollisionCancelled();
		}

		@Override
		public boolean isPickupCancelled() {
			return vec.isPickupCancelled();
		}

		@Override
		public void setCollisionCancelled(boolean cancel) {
			vec.setCollisionCancelled(cancel);
		}

		@Override
		public void setPickupCancelled(boolean cancel) {
			vec.setPickupCancelled(cancel);
		}

		@Override
		public MCCollisionType getCollisionType() {
			return MCCollisionType.ENTITY;
		}
	}
	
	@abstraction(type = Implementation.Type.BUKKIT)
	public static class BukkitMCVehicleBlockCollideEvent extends BukkitMCVehicleEvent
			implements MCVehicleBlockCollideEvent {

		VehicleBlockCollisionEvent vbc;
		public BukkitMCVehicleBlockCollideEvent(VehicleCollisionEvent event) {
			super(event);
			vbc = (VehicleBlockCollisionEvent) event;
		}
		
		@Override
		public MCBlock getBlock() {
			return new BukkitMCBlock(vbc.getBlock());
		}

		@Override
		public MCCollisionType getCollisionType() {
			return MCCollisionType.BLOCK;
		}
	}
	
	@abstraction(type = Implementation.Type.BUKKIT)
	public static class BukkitMCVehicleEnterEvent extends BukkitMCVehicleEvent
			implements MCVehicleEnterExitEvent {

		VehicleEnterEvent vee;
		public BukkitMCVehicleEnterEvent(VehicleEnterEvent event) {
			super(event);
			vee = event;
		}
		
		@Override
		public MCEntity getEntity() {
			if (vee.getEntered() == null) {
				return null;
			}
			return BukkitConvertor.BukkitGetCorrectEntity(vee.getEntered());
		}
	}
	
	@abstraction(type = Implementation.Type.BUKKIT)
	public static class BukkitMCVehicleExitEvent extends BukkitMCVehicleEvent
			implements MCVehicleEnterExitEvent {

		VehicleExitEvent vee;
		public BukkitMCVehicleExitEvent(VehicleExitEvent event) {
			super(event);
			vee = event;
		}
		
		@Override
		public MCEntity getEntity() {
			if (vee.getExited() == null) {
				return null;
			}
			return BukkitConvertor.BukkitGetCorrectEntity(vee.getExited());
		}
	}

	public static class BukkitMCVehicleEvent implements MCVehicleEvent {

		VehicleEvent ve;
		public BukkitMCVehicleEvent(VehicleEvent event) {
			ve = event;
		}

		@Override
		public MCVehicle getVehicle() {
			if (ve.getVehicle() instanceof org.bukkit.entity.Vehicle) {
				return (MCVehicle) BukkitConvertor.BukkitGetCorrectEntity(ve.getVehicle());
			}
			return null;
		}

		@Override
		public Object _GetObject() {
			return ve;
		}
	}

	@abstraction(type = Implementation.Type.BUKKIT)
	public static class BukkitMCVehicleMoveEvent implements MCVehicleMoveEvent {

		BukkitMCLocation from;
		BukkitMCLocation to;
		BukkitMCVehicle vehicle;
		boolean cancelled = false;

		public BukkitMCVehicleMoveEvent(Vehicle vehicle, Location from, Location to) {
			this.vehicle = new BukkitMCVehicle(vehicle);
			this.from = new BukkitMCLocation(from);
			this.to = new BukkitMCLocation(to);
		}

		@Override
		public MCLocation getFrom() {
			return from;
		}

		@Override
		public MCLocation getTo() {
			return to;
		}

		@Override
		public MCVehicle getVehicle() {
			return vehicle;
		}

		@Override
		public Object _GetObject() {
			return null;
		}

		@Override
		public void setCancelled(boolean state) {
			cancelled = state;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}
	}
}
