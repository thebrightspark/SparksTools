package brightspark.sparkstools.item

import brightspark.sparkstools.SparksTools
import com.google.common.base.MoreObjects
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraftforge.oredict.OreDictionary

class CustomTool(private val data: CustomToolData) {
	/**
	 * The type of this tool
	 */
	val type = ToolType.valueOf(data.type.replace(Regex("\\s"), "_").toUpperCase())

	/**
	 * The display name for the item
	 */
	val name = data.name ?: "${getMaterialName()} ${type.formattedName}"

	/**
	 * The [name] converted into a [ResourceLocation] to be used as the item registry name
	 */
	val registryName = ResourceLocation(SparksTools.MOD_ID, name.toLowerCase().replace(Regex("\\s"), "_"))

	/**
	 * The colours to use when colouring the texture layers
	 */
	var textureColours = if (data.textureColours == null) emptyList() else
		data.textureColours.map {
			var colour: Int? = null
			// Hexadecimal colour
			if (it.startsWith("0x")) colour = it.toIntOrNull(16)
			// Decimal colour
			if (colour == null) colour = it.toIntOrNull()
			return@map colour
		}.filterNotNull().toList()

	/**
	 * If material is an item registry name, then tries to get an [ItemStack] of the [Item], otherwise returns null
	 */
	fun getMaterialStack(): NonNullList<ItemStack>? = Item.getByNameOrId(data.material)?.let { NonNullList.from(ItemStack.EMPTY, ItemStack(it)) }

	/**
	 * If material is an ore dictionary name, then tries to get a [NonNullList] of [ItemStack]s registered to it
	 */
	fun getMaterialOres(): NonNullList<ItemStack> = OreDictionary.getOres(data.material)

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

	val harvestLevel: Int
		get() = data.harvestLevel

	val durability: Int
		get() = data.durability

	val efficiency: Float
		get() = data.efficiency

	override fun toString(): String {
		return MoreObjects.toStringHelper(this)
			.add("type", type)
			.add("name", name)
			.add("registryName", registryName)
			.add("material", data.material)
			.add("textureColours", textureColours)
			.toString()
	}
}