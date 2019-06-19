package brightspark.sparkstools.item

data class SHToolMaterial(
	val type: ToolType,
	val harvestLevel: Int,
	val durability: Int,
	val efficiency: Float
)