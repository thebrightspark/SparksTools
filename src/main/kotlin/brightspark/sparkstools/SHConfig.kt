package brightspark.sparkstools

import net.minecraftforge.common.ForgeConfigSpec

object SHConfig {
	val COMMON: SHCommonConfig
	val COMMON_SPEC: ForgeConfigSpec

	var lumberAxeMaxBlocks = 0

	init {
		ForgeConfigSpec.Builder().configure { SHCommonConfig(it) }.apply {
			COMMON = left
			COMMON_SPEC = right
		}
	}

	fun bake() {
		lumberAxeMaxBlocks = COMMON.lumberAxeMaxBlocks.get()
	}
}

class SHCommonConfig(builder: ForgeConfigSpec.Builder) {
	val lumberAxeMaxBlocks = builder
		.comment("The max number of wood log blocks a Lumber Axe can break at once")
		.defineInRange("lumberAxeMaxBlocks", 100, 1, Int.MAX_VALUE)
}
