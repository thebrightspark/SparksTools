package brightspark.sparkstools.init

import brightspark.sparkstools.item.ToolType
import brightspark.sparkstools.registerShapedRecipe
import brightspark.sparkstools.registerShapelessRecipe
import net.minecraft.init.Blocks
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import net.minecraftforge.registries.IForgeRegistry

object SHRecipes {
	private val recipeHammer = arrayOf("HHH", " S ", " S ")
	private val recipeExcavator = arrayOf(" H ", " S ", " S ")
	private val recipeLumberAxe = arrayOf("HH ", "HS ", " S ")
	private val recipePlow = arrayOf("HH ", " S ", " S ")

	fun regRecipes(registry: IForgeRegistry<IRecipe>) {
		registry.registerShapelessRecipe(SHBlocks.compressedCobblestone, Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.COBBLESTONE)

		SHItems.toolItems.forEach {
			val pattern = when (it.tool.type) {
				ToolType.HAMMER -> recipeHammer
				ToolType.EXCAVATOR -> recipeExcavator
				ToolType.LUMBER_AXE -> recipeLumberAxe
				ToolType.PLOW -> recipePlow
			}
			registry.registerShapedRecipe(it, *pattern, 'H', Ingredient.fromStacks(*it.tool.material.toTypedArray()), 'S', "stickWood")
		}
	}
}