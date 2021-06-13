package com.laytonsmith.abstraction.entities;

import com.laytonsmith.abstraction.enums.MCAxolotlType;

public interface MCAxolotl extends MCTameable {
	boolean isPlayingDead();
	void setPlayingDead(boolean playingDead);
	MCAxolotlType getAxolotlType();
	void setAxolotlType(MCAxolotlType type);
}
