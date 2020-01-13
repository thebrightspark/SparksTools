package brightspark.sparkstools.item

import brightspark.sparkstools.SparksTools
import net.minecraft.block.material.Material
import net.minecraft.block.material.Material.*
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.ToolType

enum class STToolType(toolTypeNames: Array<String>, val effectiveMaterials: Set<Material>) {
	HAMMER(arrayOf("pickaxe", "hammer"), setOf(ROCK, IRON, ANVIL, GLASS, REDSTONE_LIGHT, ICE, PACKED_ICE, PISTON)),
	EXCAVATOR(arrayOf("shovel", "excavator"), setOf(EARTH, SAND, SNOW, CLAY)),
	LUMBER_AXE(arrayOf("axe", "lumber_axe"), setOf(WOOD, CACTUS, GOURD, BAMBOO)),
	PLOW(arrayOf("hoe", "plow"), setOf(PLANTS, GOURD)),
	SICKLE(arrayOf("sickle"), setOf(PLANTS, LEAVES, WEB, WOOL, OCEAN_PLANT, TALL_PLANTS, SEA_GRASS));

	val toolTypes = toolTypeNames.map { ToolType.get(it) }.toSet()
	val lowerCase = name.toLowerCase()
	val formattedName = lowerCase.split('_').joinToString(" ") { it.capitalize() }

	val modelLocation = ResourceLocation(SparksTools.MOD_ID, "item/$lowerCase")
	val textureHeadLocation = ResourceLocation(SparksTools.MOD_ID, "items/${lowerCase}_head")
	val textureHandleLocation = ResourceLocation(SparksTools.MOD_ID, "items/${lowerCase}_handle")
}
