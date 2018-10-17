package com.laytonsmith.abstraction.events;

import com.laytonsmith.abstraction.MCEntity;
import com.laytonsmith.abstraction.entities.MCPigZombie;
import com.laytonsmith.core.constructs.CInt;
import com.laytonsmith.core.events.BindableEvent;

public interface MCPigZombieAngerEvent extends BindableEvent {

	MCPigZombie getEntity();

	CInt getNewAnger();

	MCEntity getTarget();

	boolean isCancelled();

	void setCancelled(boolean cancelled);

	void setNewAnger(int newAnger);

}
