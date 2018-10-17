package com.laytonsmith.abstraction.events;

import com.laytonsmith.abstraction.MCInventory;
import com.laytonsmith.abstraction.blocks.MCBlock;
import com.laytonsmith.core.constructs.CInt;
import com.laytonsmith.core.events.BindableEvent;

public interface MCBrewEvent extends BindableEvent {

	MCInventory getContents();

	CInt getFuelLevel();

	MCBlock getBlock();

	boolean isCancelled();

	void setCancelled(boolean cancelled);

}
