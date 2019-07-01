package brightspark.sparkstools.item

import brightspark.sparkstools.SparksTools
import com.google.common.base.MoreObjects
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraftforge.oredict.OreDictionary

class CustomTool(private val data: CustomToolData) {

	init {
		@Suppress("SENSELESS_COMPARISON")
		if (data.type == null)
			throw RuntimeException("The following tool has no type! $data")
		@Suppress("SENSELESS_COMPARISON")
		if (data.material == null)
			throw RuntimeException("The following tool has no material! $data")
	}

	/**
	 * The type of this tool
	 */
	val type = ToolType.valueOf(data.type.replace(Regex("\\s"), "_").toUpperCase())

	/**
	 * A list of [ItemStack]s that can be used to create this tool
	 */
	val material: NonNullList<ItemStack> = data.material.let {
		val stacks = Item.getByNameOrId(it)?.let { item -> NonNullList.from(ItemStack.EMPTY, ItemStack(item)) } ?: OreDictionary.getOres(it)
		if (stacks.isEmpty())
			throw RuntimeException("Couldn't find an item or ore dictionary for the material $it")
		return@let stacks
	}

	/**
	 * Gets the name of the material [ItemStack] to be prepended to the tool's type for item name generation
	 */
	private fun getMaterialName(): String =
		if (material.isNotEmpty()) {
			var name = material[0].displayName
			if (name.startsWith("Block of "))
				name = name.substringAfter("Block of ")
			if (name.endsWith("Ingot"))
				name = name.substringBefore("Ingot")
			name.trim()
		}
		else
			"<null>"

	/**
	 * The display name for the item
	 */
	val name = data.name ?: "${getMaterialName()} ${type.formattedName}"

	/**
	 * The [name] converted into a [ResourceLocation] to be used as the item registry name
	 */
	val registryName = ResourceLocation(SparksTools.MOD_ID, name.toLowerCase().replace(Regex("\\s"), "_"))

	/**
	 * The colour to use when colouring the texture
	 */
	var textureColour = data.textureColour?.let {
		var colour: Int? = null
		// Hexadecimal colour
		if (it.startsWith("0x"))
			colour = it.substringAfter("0x").toIntOrNull(16)
		// Decimal colour
		if (colour == null)
			colour = it.toIntOrNull()
		return@let colour
	}

	val effectSize: Int
		get() = data.effectSize ?: 1

	val harvestLevel: Int
		get() = data.harvestLevel ?: 0

	val durability: Int
		get() = data.durability ?: 1000

	val efficiency: Float
		get() = data.efficiency ?: 1F

	val attackDamage: Float
		get() = data.attackDamage ?: 1F

	val attackSpeed: Float
		get() = data.attackSpeed ?: 1F

	val enchantability: Int
		get() = data.enchantability ?: 0

	override fun toString(): String {
		return MoreObjects.toStringHelper(this)
			.add("type", type)
			.add("name", name)
			.add("registryName", registryName)
			.add("material", data.material)
			.add("textureColour", textureColour)
			.toString()
	}
}