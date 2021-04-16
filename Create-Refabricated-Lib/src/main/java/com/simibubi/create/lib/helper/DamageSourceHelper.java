package com.simibubi.create.lib.helper;

import com.simibubi.create.lib.mixin.accessor.DamageSourceAccessor;
import com.simibubi.create.lib.utility.MixinHelper;

import net.minecraft.util.DamageSource;

public final class DamageSourceHelper {
	public static DamageSource create$createDamageSource(String string) {
		return DamageSourceAccessor.create$init(string);
	}

	// this is probably going to crash and burn.
	public static DamageSource create$createDamageSourceWhichBypassesArmor(String string) {
		return MixinHelper.<DamageSourceAccessor>cast(create$createDamageSource(string)).create$setDamageBypassesArmor();
	}

	public static DamageSource create$createFireDamageSource(String string) {
		return MixinHelper.<DamageSourceAccessor>cast(create$createDamageSource(string)).create$setFireDamage();
	}

	private DamageSourceHelper() {}
}
