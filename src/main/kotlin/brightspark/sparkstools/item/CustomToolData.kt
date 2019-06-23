package brightspark.sparkstools.item

data class CustomToolData(
	val type: String,
	val name: String?,
	val material: String,
	val textureColours: List<String>?,
	val harvestLevel: Int,
	val durability: Int,
	val efficiency: Float
)