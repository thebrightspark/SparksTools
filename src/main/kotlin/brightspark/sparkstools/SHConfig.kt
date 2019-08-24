@file:Config(modid = SparksTools.MOD_ID, name = SparksTools.MOD_ID + "/config")

package brightspark.sparkstools

import net.minecraftforge.common.config.Config

@JvmField
@Config.Comment("The max number of wood log blocks a Lumber Axe can break at once")
@Config.RangeInt(min = 1)
var lumberAxeMaxBlocks = 100
