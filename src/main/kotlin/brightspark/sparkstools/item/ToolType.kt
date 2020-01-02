package brightspark.sparkstools.item

import net.minecraft.block.material.Material
import net.minecraft.block.material.Material.*

enum class ToolType(val toolTypes: Set<String>, val effectiveMaterials: Set<Material>) {
    HAMMER(setOf("pickaxe", "hammer"), setOf(ROCK, IRON, ANVIL, CIRCUITS, GLASS, REDSTONE_LIGHT, ICE, PACKED_ICE, PISTON)),
    EXCAVATOR(setOf("shovel", "excavator"), setOf(GRASS, GROUND, SAND, SNOW, CRAFTED_SNOW, CLAY)),
    LUMBER_AXE(setOf("axe", "lumber_axe"), setOf(WOOD, CACTUS, GOURD)),
    PLOW(setOf("hoe", "plow"), setOf(PLANTS, GOURD)),
    SICKLE(setOf("sickle"), setOf(PLANTS, LEAVES, VINE, WEB));

    val formattedName = name.toLowerCase().split('_').joinToString(" ") { it.capitalize() }
}