package brightspark.sparkstools

import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.ResourceLocation
import net.minecraftforge.oredict.ShapedOreRecipe
import net.minecraftforge.oredict.ShapelessOreRecipe
import net.minecraftforge.registries.IForgeRegistry

private val recipeGroup = ResourceLocation(SparksTools.MOD_ID, "tools")

fun IForgeRegistry<IRecipe>.registerShapedRecipe(result: Item, vararg recipe: Any) {
	val r = ShapedOreRecipe(recipeGroup, result, *recipe)
	r.registryName = result.registryName
	this.register(r)
}

fun IForgeRegistry<IRecipe>.registerShapelessRecipe(result: Block, vararg recipe: Block) {
	val r = ShapelessOreRecipe(recipeGroup, result, *recipe)
	r.registryName = result.registryName
	this.register(r)
}
