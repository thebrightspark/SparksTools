package brightspark.sparkstools.init

import brightspark.sparkstools.SparksTools
import brightspark.sparkstools.item.CustomTool
import brightspark.sparkstools.item.SHToolItem
import brightspark.sparkstools.item.STToolType
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.ICraftingRecipe
import net.minecraft.item.crafting.Ingredient
import net.minecraft.item.crafting.ShapedRecipe
import net.minecraft.item.crafting.ShapelessRecipe
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.Tags
import net.minecraftforge.registries.ForgeRegistries

object SHRecipes {
	private val recipeHammer = """
		|HHH
		| S 
		| S 
	""".trimMargin("|")
	private val recipeExcavator = """
		| H 
		| S 
		| S 
	""".trimMargin("|")
	private val recipeLumberAxe = """
		|HH 
		|HS 
		| S 
	""".trimMargin("|")
	private val recipeLumberAxeReversed = reverseRecipe(recipeLumberAxe)
	private val recipePlow = """
		|HH 
		| S 
		| S 
	""".trimMargin("|")
	private val recipePlowReversed = reverseRecipe(recipePlow)
	private val recipeSickle = """
		| H 
		|HS 
		| S 
	""".trimMargin("|")
	private val recipeSickleReversed = reverseRecipe(recipeSickle)

	private fun reverseRecipe(recipe: String) = recipe.split("\n").joinToString("\n") { it.reversed() }

	private fun fillRecipe(recipe: String, tool: CustomTool) = NonNullList.from<Ingredient>(Ingredient.EMPTY, *recipe.replace("\n", "").map {
		when (it) {
			'H' -> tool.headMaterial
			'S' -> tool.handleMaterial
			else -> Ingredient.EMPTY
		}
	}.toTypedArray())

	private fun getPatternForType(type: STToolType) = when (type) {
		STToolType.HAMMER -> recipeHammer
		STToolType.EXCAVATOR -> recipeExcavator
		STToolType.LUMBER_AXE -> recipeLumberAxe
		STToolType.PLOW -> recipePlow
		STToolType.SICKLE -> recipeSickle
	}

	private fun getReversedPatternForType(type: STToolType) = when (type) {
		STToolType.LUMBER_AXE -> recipeLumberAxeReversed
		STToolType.PLOW -> recipePlowReversed
		STToolType.SICKLE -> recipeSickleReversed
		else -> null
	}

	private val cobblestoneIngredient = Ingredient.fromTag(Tags.Items.COBBLESTONE)

	private fun shapelessRecipe(result: ItemStack, ingredients: NonNullList<Ingredient>): ShapelessRecipe =
		result.item.registryName!!.let { ShapelessRecipe(it, it.toString(), result, ingredients) }

	private fun shapedRecipe(result: ItemStack, ingredients: NonNullList<Ingredient>, id: ResourceLocation = result.item.registryName!!, group: String = id.toString()): ShapedRecipe =
		ShapedRecipe(id, group, 3, 3, ingredients, result)

	fun createRecipes(): List<ICraftingRecipe> {
		val recipes = mutableListOf<ICraftingRecipe>()

		recipes += shapelessRecipe(ItemStack(SHBlocks.compressedCobblestone), NonNullList.withSize(9, cobblestoneIngredient))

		SparksTools.readCustomTools()
			.mapNotNull {
				val customTool = CustomTool(it)
				val item = ForgeRegistries.ITEMS.getValue(customTool.registryName)
				return@mapNotNull if (item !is SHToolItem) {
					SparksTools.logger.warn("Couldn't find registered tool '${customTool.registryName}' for new recipe")
					null
				} else
					item to customTool
			}
			.forEach {
				val toolItem = it.first
				val tool = it.second
				val type = tool.type
				val result = ItemStack(toolItem)
				recipes += shapedRecipe(result, fillRecipe(getPatternForType(type), tool))
				getReversedPatternForType(type)?.let { pattern ->
					val id = toolItem.registryName!!.toString() + "_reversed"
					recipes += shapedRecipe(result, fillRecipe(pattern, tool), ResourceLocation(id), id)
				}
			}

		return recipes
	}
}
