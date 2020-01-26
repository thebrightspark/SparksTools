package brightspark.sparkstools.item

import brightspark.sparkstools.SparksTools
import com.google.common.base.MoreObjects
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.oredict.OreDictionary
import kotlin.math.abs

class CustomTool(val data: CustomToolData) {

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

	private val materialSplit = data.material.split(':')

	/**
	 * A list of [ItemStack]s that can be used to create this tool
	 */
	val material: NonNullList<ItemStack> by lazy {
		val parts = materialSplit.size
		if (parts > 1) {
			val regName = if (parts == 2) data.material else "${materialSplit[0]}:${materialSplit[1]}"
			val meta = if (parts == 2) 0 else materialSplit[2].toIntOrNull() ?: 0
			Item.getByNameOrId(regName)?.let { item ->
				return@let NonNullList.from(ItemStack.EMPTY, ItemStack(item, 1, meta))
			}?.let { stack ->
				return@lazy stack
			}
			SparksTools.logger.warn("Tried parsing material '${data.material}' as an item unsuccessfully (tried using registry name '$regName' and meta '$meta') - falling back to trying ore dictionary")
		}

		val stacks = OreDictionary.getOres(data.material)
		if (stacks.isEmpty())
			throw RuntimeException("Couldn't find an item or ore dictionary for the material ${data.material}")
		return@lazy stacks
	}

	/**
	 * Gets the name of the material [ItemStack] to be prepended to the tool's type for item name generation
	 */
	private fun getMaterialName(): String =
		if (material.isNotEmpty()) {
			var name = TextFormatting.getTextWithoutFormattingCodes(material[0].displayName)!!
			if (name.startsWith("Block of "))
				name = name.substringAfter("Block of ")
			if (name.endsWith("Ingot"))
				name = name.substringBefore("Ingot")
			name.trim()
		} else
			"<null>"

	/**
	 * The display name for the item
	 */
	val name: String by lazy { data.name ?: "${getMaterialName()} ${type.formattedName}" }

	/**
	 * The [name] converted into a [ResourceLocation] to be used as the item registry name
	 */
	val registryName = ResourceLocation(SparksTools.MOD_ID, (data.name?.replace(Regex("\\s"), "_")
		?: "${type.name}_${materialSplit.take(2).joinToString("_")}").toLowerCase())

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
		get() = data.effectSize?.let { abs(it) } ?: 1

	val harvestLevel: Int
		get() = data.harvestLevel?.let { abs(it) } ?: 0

	val durability: Int
		get() = data.durability?.let { abs(it) } ?: 1000

	val efficiency: Float
		get() = data.efficiency?.let { abs(it) } ?: 1F

	val attackDamage: Float
		get() = data.attackDamage?.let { abs(it) } ?: 1F

	val attackSpeed: Float
		get() = data.attackSpeed ?: 1F

	val enchantability: Int
		get() = data.enchantability?.let { abs(it) } ?: 0

	override fun toString(): String = MoreObjects.toStringHelper(this)
		.add("type", type)
		.add("registryName", registryName)
		.add("material", data.material)
		.add("textureColour", textureColour)
		.toString()
}