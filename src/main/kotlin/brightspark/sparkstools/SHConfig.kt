package brightspark.sparkstools

import net.minecraftforge.common.config.Config

@Config(modid = SparksTools.MOD_ID, name = "${SparksTools.MOD_ID}/config")
object SHConfig {
	@JvmField
	@Config.Comment("The max number of wood log blocks a Lumber Axe can break at once")
	@Config.RangeInt(min = 1)
	var lumberAxeMaxBlocks = 100
}