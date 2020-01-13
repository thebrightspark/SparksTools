package brightspark.sparkstools.init

import brightspark.sparkstools.SparksTools
import brightspark.sparkstools.item.*
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.ColorHandlerEvent
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.registries.IForgeRegistry

object SHItems {
	//	private val colourCache = HashMap<String, Int?>()
	val toolItems = ArrayList<SHToolItem>()

	private var lastTabIconSecond = 0L
	private var lastTabIconStack = ItemStack.EMPTY

	fun getTabIcon(): ItemStack {
		val second = System.currentTimeMillis() / 1000
		if (second != lastTabIconSecond) {
			lastTabIconSecond = second
			lastTabIconStack = ItemStack(toolItems[(second % toolItems.size).toInt()])
		}
		return lastTabIconStack
	}

	fun regItems(registry: IForgeRegistry<Item>) = SparksTools.readCustomTools()
		.mapNotNull {
			try {
				return@mapNotNull CustomTool(it)
			} catch (e: Exception) {
				SparksTools.logger.error("Error when trying to create tool -> $it", e)
				return@mapNotNull null
			}
		}.forEach {
			val item = when (it.type) {
				STToolType.HAMMER -> ItemHammer(it)
				STToolType.EXCAVATOR -> ItemExcavator(it)
				STToolType.LUMBER_AXE -> ItemLumberAxe(it)
				STToolType.PLOW -> ItemPlow(it)
				STToolType.SICKLE -> ItemSickle(it)
			}
			SparksTools.logger.info("Registering ${item.registryName} -> ${item.tool}")
			toolItems += item
			registry.register(item)
		}

	@OnlyIn(Dist.CLIENT)
	fun regTextures(event: TextureStitchEvent.Pre) {
		if (event.map.basePath == "textures") {
			STToolType.values().forEach {
				event.addSprite(it.textureHeadLocation)
				event.addSprite(it.textureHandleLocation)
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	fun regModels() = STToolType.values().forEach { ModelLoader.addSpecialModel(it.modelLocation) }

	@OnlyIn(Dist.CLIENT)
	fun regBakedModels(event: ModelBakeEvent) = event.modelRegistry.let { modelRegistry ->
		val toolModels = STToolType.values().associate { it.lowerCase to modelRegistry[it.modelLocation] }
		modelRegistry.keys
			.filter { it.namespace == SparksTools.MOD_ID }
			.forEach { model ->
				toolModels[model.path.substringAfter('_')]?.let {
					modelRegistry.replace(model, it)
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	fun regColours(event: ColorHandlerEvent.Item) =
		event.itemColors.register({ stack, tintIndex ->
			if (tintIndex == 1) (stack.item as SHToolItem).tool.textureColour else -1
		}, toolItems.toTypedArray())

//	@OnlyIn(Dist.CLIENT)
//	fun calcMissingMaterialColours() {
//		SparksTools.logger.info("Starting to calculate tool colours for tools missing colours")
//		toolItems.filter { it.tool.textureColour == null }
//			.map { it.tool }
//			.forEach { it.textureColour = colourCache.getOrPut(it.data.material) { calcMaterialColour(it) } }
//		SparksTools.logger.info("Finished calculating tool colours")
//		val toolsMissingColours = toolItems.filter { it.tool.textureColour == null }.joinToString("\n", transform = { it.tool.toString() })
//		if (toolsMissingColours.isNotEmpty())
//			SparksTools.logger.warn("The following tools are missing colours! This is most likely due to missing material textures - find warnings and errors above regarding them for more details. They will appear uncoloured in-game:\n$toolsMissingColours")
//	}
//
//	/**
//	 * Calculates the material colour for the [tool] and sets it to the [CustomTool.textureColour]
//	 */
//	@OnlyIn(Dist.CLIENT)
//	private fun calcMaterialColour(tool: CustomTool): Int? {
//		val colours = tool.material.mapNotNull { getAverageColour(it) }.toSet()
//		if (colours.isEmpty()) {
//			SparksTools.logger.error("Found no valid material to calculate a colour for the tool $tool")
//			return null
//		}
//		val num = colours.size
//		val total = colours.reduce { acc, triple ->
//			acc.left += triple.left
//			acc.middle += triple.middle
//			acc.right += triple.right
//			return@reduce acc
//		}
//		val colour = Color(total.left / num, total.middle / num, total.right / num)
//		val rgb = colour.rgb
//		SparksTools.logger.info("Calculated the colour $colour ($rgb) for the tool material '${tool.data.material}'")
//		return rgb
//	}
//
//	/**
//	 * Gets the average colour of the texture for the [stack] in a [MutableTriple] of RGB components
//	 */
//	@OnlyIn(Dist.CLIENT)
//	private fun getAverageColour(stack: ItemStack): MutableTriple<Int, Int, Int>? {
//		val model = Minecraft.getInstance().itemRenderer.getItemModelWithOverrides(stack, null, null)
//		val texture = model.getParticleTexture(EmptyModelData.INSTANCE)
//		if (texture.name.path == "missingno") {
//			SparksTools.logger.warn("Got missing texture for stack $stack")
//			return null
//		}
//		val pixels = mutableListOf<Int>()
//		(0 until texture.height).forEach { y ->
//			(0 until texture.width).forEach { x ->
//				pixels += texture.getPixelRGBA(0, x, y)
//			}
//		}
//		val rgbData = pixels.filter { (it shr 24) and 0xFF > 127 }.map { Color(it) }.toList()
//		if (rgbData.isEmpty()) {
//			SparksTools.logger.warn("Using fallback colour algo - no suitably opaque pixels for $stack")
//			return getAverageColourBasic(pixels.toIntArray())
//		}
//
//		// Calculate standard deviation of brightnesses
//		val brightnesses = rgbData.map { Color.RGBtoHSB(it.red, it.green, it.blue, null)[2] }.toList()
//		val size = brightnesses.size
//		var sum = 0.0
//		var sumOfSquares = 0.0
//		brightnesses.forEach {
//			sum += it
//			sumOfSquares += it * it
//		}
//		val mean = sum / size
//		val stdDev = sqrt((sumOfSquares - sum * sum / size) / (size - 1))
//
//		// Filter using standard deviation and calculate mean
//		val filteredColours = rgbData.filterIndexed { i, _ -> abs(brightnesses[i] - mean) < stdDev }
//			.map {
//				val c = it.brighter()
//				MutableTriple(c.red, c.green, c.blue)
//			}.toList()
//		if (filteredColours.isEmpty()) {
//			SparksTools.logger.warn("Using fallback colour algo - no pixels within standard deviation for $stack")
//			return getAverageColourBasic(pixels.toIntArray())
//		}
//		val sizeFiltered = filteredColours.size
//		val colourMean = filteredColours.reduce { acc, colour ->
//			acc.apply {
//				left += colour.left
//				middle += colour.middle
//				right += colour.right
//			}
//		}
//		colourMean.left /= sizeFiltered
//		colourMean.middle /= sizeFiltered
//		colourMean.right /= sizeFiltered
//		return colourMean
//	}
//
//	@OnlyIn(Dist.CLIENT)
//	private fun getAverageColourBasic(pixels: IntArray): MutableTriple<Int, Int, Int> {
//		val rgb = MutableTriple(0, 0, 0)
//		var num = 0
//		pixels.forEach {
//			val colour = Color(it)
//			if (colour.alpha <= 0)
//				return@forEach
//			rgb.left += colour.red
//			rgb.middle += colour.green
//			rgb.right += colour.blue
//			num++
//		}
//		rgb.left /= num
//		rgb.middle /= num
//		rgb.right /= num
//		return rgb
//	}
}