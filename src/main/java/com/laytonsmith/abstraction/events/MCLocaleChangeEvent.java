package com.laytonsmith.abstraction.events;

import com.laytonsmith.abstraction.MCPlayer;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.events.BindableEvent;

public interface MCLocaleChangeEvent extends BindableEvent {

	CString getLocale();

	MCPlayer getPlayer();

}
