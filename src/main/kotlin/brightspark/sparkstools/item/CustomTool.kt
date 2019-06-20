package brightspark.sparkstools.item

import brightspark.sparkstools.SparksTools
import com.google.common.base.MoreObjects
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraftforge.oredict.OreDictionary

class CustomTool(
	type: String,
	name: String?,
	val material: String,
	textureColours: List<String>?,
	val harvestLevel: Int,
	val durability: Int,
	val efficiency: Float
) {
	/**
	 * The type of this tool
	 */
	@Suppress("USELESS_ELVIS")
	@delegate:Transient
	val type by lazy { ToolType.valueOf(type.toUpperCase()) ?: throw RuntimeException("The tool type '$type' does not exist!") }

	/**
	 * The display name for the item
	 */
	@delegate:Transient
	val name by lazy { name ?: "${getMaterialName()} ${this.type.getFormattedName()}" }

	/**
	 * The [name] converted into a [ResourceLocation] to be used as the item registry name
	 */
	@delegate:Transient
	val registryName by lazy { ResourceLocation(SparksTools.MOD_ID, this.name.toLowerCase().replace("\\s", "_")) }

	/**
	 * The colours to use when colouring the texture layers
	 */
	@delegate:Transient
	val textureColours by lazy {
		if (textureColours == null) return@lazy null
		return@lazy textureColours.map {
			var colour: Int? = null
			// Hexadecimal colour
			if (it.startsWith("0x")) colour = it.toIntOrNull(16)
			// Decimal colour
			if (colour == null) colour = it.toIntOrNull()
			return@map colour
		}.filterNotNull().toList()
	}

	/**
	 * If [material] is an item registry name, then tries to get an [ItemStack] of the [Item], otherwise returns null
	 */
	fun getMaterialStack(): NonNullList<ItemStack>? = Item.getByNameOrId(material)?.let { NonNullList.from(ItemStack.EMPTY, ItemStack(it)) }

	/**
	 * If [material] is an ore dictionary name, then tries to get a [NonNullList] of [ItemStack]s registered to it
	 */
	fun getMaterialOres(): NonNullList<ItemStack> = OreDictionary.getOres(material)

	/**
	 * Gets the display name from the [stack] and strips the last word from it
	 */
	private fun getNameFromStack(stack: ItemStack): String = stack.displayName.substringBeforeLast(' ')

	/**
	 * Gets the name of the material [ItemStack] to be prepended to the tool's type for item name generation
	 */
	fun getMaterialName(): String {
		return getMaterialStack()?.let { getNameFromStack(it[0]) } ?:
			getMaterialOres().let { if (it.isNotEmpty()) getNameFromStack(it[0]) else "<null>" }
	}

	override fun toString(): String {
		return MoreObjects.toStringHelper(this)
			.add("type", type)
			.add("name", name)
			.add("registryName", registryName)
			.add("material", material)
			.add("textureColours", textureColours)
			.toString()
	}
}