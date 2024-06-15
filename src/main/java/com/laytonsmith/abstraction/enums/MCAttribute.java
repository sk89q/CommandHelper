package com.laytonsmith.abstraction.enums;

import com.laytonsmith.annotations.MEnum;

@MEnum("com.commandhelper.Attribute")
public enum MCAttribute {
	GENERIC_ARMOR,
	GENERIC_ARMOR_TOUGHNESS,
	GENERIC_ATTACK_DAMAGE,
	GENERIC_ATTACK_KNOCKBACK,
	GENERIC_ATTACK_SPEED,
	GENERIC_BURNING_TIME,
	GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE,
	GENERIC_FALL_DAMAGE_MULTIPLIER,
	GENERIC_FLYING_SPEED,
	GENERIC_FOLLOW_RANGE,
	GENERIC_GRAVITY,
	GENERIC_JUMP_STRENGTH,
	GENERIC_KNOCKBACK_RESISTANCE,
	GENERIC_LUCK,
	GENERIC_MAX_ABSORPTION,
	GENERIC_MAX_HEALTH,
	GENERIC_MOVEMENT_EFFICIENCY,
	GENERIC_MOVEMENT_SPEED,
	GENERIC_OXYGEN_BONUS,
	GENERIC_SAFE_FALL_DISTANCE,
	GENERIC_SCALE,
	GENERIC_STEP_HEIGHT,
	GENERIC_WATER_MOVEMENT_EFFICIENCY,
	HORSE_JUMP_STRENGTH, // changed to GENERIC_JUMP_STRENGTH in 1.20.6
	PLAYER_BLOCK_BREAK_SPEED,
	PLAYER_BLOCK_INTERACTION_RANGE,
	PLAYER_ENTITY_INTERACTION_RANGE,
	PLAYER_MINING_EFFICIENCY,
	PLAYER_SNEAKING_SPEED,
	PLAYER_SUBMERGED_MINING_SPEED,
	PLAYER_SWEEPING_DAMAGE_RATIO,
	ZOMBIE_SPAWN_REINFORCEMENTS
}
