package brightspark.sparkstools

import brightspark.sparkstools.init.SHRecipes
import com.google.common.collect.ImmutableMap
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.IRecipeType
import net.minecraft.resources.IResourceManager
import net.minecraft.resources.IResourceManagerReloadListener
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.server.ServerLifecycleHooks

object DynamicRecipeDataPack : IResourceManagerReloadListener {
	override fun onResourceManagerReload(resourceManager: IResourceManager) {
		SparksTools.logger.info("Reloading Recipes!")
		val recipeManager = ServerLifecycleHooks.getCurrentServer().recipeManager
		if (recipeManager.recipes is ImmutableMap) {
			val recipeMap = HashMap<IRecipeType<*>, Map<ResourceLocation, IRecipe<*>>>()
			recipeManager.recipes.forEach { (type, map) -> recipeMap[type] = HashMap(map) }
			recipeManager.recipes = recipeMap
		}
		val recipes = recipeManager.recipes[IRecipeType.CRAFTING]!!
		SHRecipes.createRecipes().forEach { recipes[it.id] = it }
	}
}
