package brightspark.sparkstools.item

data class CustomToolData(
	val type: String,
	val name: String?,
	val material: String,
	val textureColour: String?,
	val effectSize: Int?,
	val harvestLevel: Int?,
	val durability: Int?,
	val efficiency: Float?,
	val attackDamage: Float?,
	val attackSpeed: Float?,
	val enchantability: Int?
)