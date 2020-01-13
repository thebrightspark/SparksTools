package brightspark.sparkstools.item

import brightspark.sparkstools.SparksTools
import com.google.common.base.MoreObjects
import net.minecraft.item.Item
import net.minecraft.item.crafting.Ingredient
import net.minecraft.tags.ItemTags
import net.minecraft.tags.Tag
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.Tags
import net.minecraftforge.registries.ForgeRegistries
import kotlin.math.abs

@Suppress("SENSELESS_COMPARISON")
class CustomTool(val data: CustomToolData) {
	companion object {
		private val REGEX_WHITESPACE = Regex("\\s")
	}

	init {
		if (data.type == null)
			error("type")
		if (data.name == null)
			error("name")
		if (data.headMaterial == null)
			error("material")
		if (data.textureColour == null)
			error("textureColour")
	}

	private fun error(dataName: String): Unit = throw RuntimeException("The following tool has no $dataName! -> $data")

	/**
	 * The type of this tool
	 */
	val type: STToolType = STToolType.valueOf(data.type.replace(REGEX_WHITESPACE, "_").toUpperCase())

	private fun resolveItem(registryName: String): Item =
		ForgeRegistries.ITEMS.getValue(ResourceLocation(data.headMaterial))
			?: throw RuntimeException("The item '$registryName' does not exist!")

	private fun resolveItemTag(tagName: String): Tag<Item> =
		ItemTags.getCollection()[ResourceLocation(tagName)]
			?: throw RuntimeException("The item tag '$tagName' does not exist!")

	private fun resolveIngredient(material: String): Ingredient = if (material.contains(':')) {
		Ingredient.fromItems(resolveItem(material))
	} else {
		Ingredient.fromTag(resolveItemTag(material))
	}

	/**
	 * The [Ingredient] used as the material for the head of this tool
	 */
	val headMaterial: Ingredient by lazy { resolveIngredient(data.headMaterial) }

	/**
	 * The [Ingredient] used as the material for the handle of this tool
	 */
	val handleMaterial: Ingredient by lazy {
		data.handleMaterial?.let { resolveIngredient(it) } ?: Ingredient.fromTag(Tags.Items.RODS_WOODEN)
	}

	/**
	 * The display name for the item
	 */
	val name: String
		get() = data.name

	/**
	 * The [name] converted into a [ResourceLocation] to be used as the item registry name
	 */
	val registryName: ResourceLocation =
		ResourceLocation(SparksTools.MOD_ID, name.toLowerCase().replace(REGEX_WHITESPACE, "_"))

	/**
	 * The colour to use when colouring the texture
	 */
	var textureColour: Int = data.textureColour.let {
		(if (it.startsWith("0x")) it.substringAfter("0x").toIntOrNull(16) else it.toIntOrNull())
			?: throw RuntimeException("The colour '$it' is invalid!")
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
		.add("name", name)
		.add("registryName", registryName)
		.add("material", data.headMaterial)
		.add("textureColour", textureColour)
		.toString()
}
