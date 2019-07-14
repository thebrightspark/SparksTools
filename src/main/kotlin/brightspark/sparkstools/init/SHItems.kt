package brightspark.sparkstools.init

import brightspark.sparkstools.SparksTools
import brightspark.sparkstools.item.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.ColorHandlerEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.minecraftforge.oredict.OreDictionary
import net.minecraftforge.registries.IForgeRegistry
import org.apache.commons.lang3.tuple.MutableTriple
import java.awt.Color
import java.io.FileReader
import java.io.Reader
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.sqrt

object SHItems {
	private val colourCache = HashMap<String, Int?>()
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

	fun regItems(registry: IForgeRegistry<Item>) {
		Gson().fromJson<List<CustomToolData>>(
			JsonReader(FileReader(SparksTools.customToolsFile) as Reader?),
			object : TypeToken<List<CustomToolData>>() {}.type
		).mapNotNull {
			try {
				return@mapNotNull CustomTool(it)
			} catch (e: Exception) {
				SparksTools.logger.error("Error when trying to create tool -> $it", e)
				return@mapNotNull null
			}
		}.forEach {
			val item = when (it.type) {
				ToolType.HAMMER -> ItemHammer(it)
				ToolType.EXCAVATOR -> ItemExcavator(it)
				ToolType.LUMBER_AXE -> ItemLumberAxe(it)
				ToolType.PLOW -> ItemPlow(it)
			}
			SparksTools.logger.info("Registering ${item.registryName} -> ${item.tool}")
			toolItems += item
			registry.register(item)
		}
	}

	@SideOnly(Side.CLIENT)
	fun regModels() {
		toolItems.forEach {
			ModelLoader.setCustomModelResourceLocation(it, 0,
				ModelResourceLocation("${it.registryName!!.namespace}:${it.tool.type.name.toLowerCase(Locale.ROOT)}", "inventory")
			)
		}
	}

	@SideOnly(Side.CLIENT)
	fun regColours(event: ColorHandlerEvent.Item) {
		// A compiler error appears here in IntelliJ IDEA, but it builds fine
		event.itemColors.registerItemColorHandler(
			{ stack: ItemStack, tintIndex: Int ->
				if (tintIndex == 1) (stack.item as SHToolItem).tool.textureColour ?: -1 else -1
			},
			toolItems.toTypedArray()
		)
	}

	@SideOnly(Side.CLIENT)
	fun calcMissingMaterialColours() {
		SparksTools.logger.info("Starting to calculate tool colours for tools missing colours")
		toolItems.filter { it.tool.textureColour == null }
			.map { it.tool }
			.forEach { it.textureColour = colourCache.getOrPut(it.data.material) { calcMaterialColour(it) } }
		SparksTools.logger.info("Finished calculating tool colours")
		val toolsMissingColours = toolItems.filter { it.tool.textureColour == null }.joinToString("\n", transform = { it.tool.toString() })
		if (toolsMissingColours.isNotEmpty())
			SparksTools.logger.warn("The following tools are missing colours! This is most likely due to missing material textures - find warnings and errors above regarding them for more details. They will appear uncoloured in-game:\n$toolsMissingColours")
	}

	/**
	 * Calculates the material colour for the [tool] and sets it to the [CustomTool.textureColour]
	 */
	@SideOnly(Side.CLIENT)
	private fun calcMaterialColour(tool: CustomTool): Int? {
		val colours = tool.material.mapNotNull { getAverageColour(it) }.toSet()
		if (colours.isEmpty()) {
			SparksTools.logger.error("Found no valid material to calculate a colour for the tool $tool")
			return null
		}
		val num = colours.size
		val total = colours.reduce { acc, triple ->
			acc.left += triple.left
			acc.middle += triple.middle
			acc.right += triple.right
			return@reduce acc
		}
		val colour = Color(total.left / num, total.middle / num, total.right / num)
		val rgb = colour.rgb
		SparksTools.logger.info("Calculated the colour $colour ($rgb) for the tool material '${tool.data.material}'")
		return rgb
	}

	/**
	 * Gets the average colour of the texture for the [stack] in a [MutableTriple] of RGB components
	 */
	@SideOnly(Side.CLIENT)
	private fun getAverageColour(stack: ItemStack): MutableTriple<Int, Int, Int>? {
		if (stack.metadata == OreDictionary.WILDCARD_VALUE) {
			SparksTools.logger.warn("Can't get the colour of a stack of unknown metadata -> $stack")
			return null
		}
		val model = Minecraft.getMinecraft().renderItem.getItemModelWithOverrides(stack, null, null)
		val texture = model.particleTexture
		if (texture.iconName == "missingno") {
			SparksTools.logger.warn("Got missing texture for stack $stack")
			return null
		}
		val pixels = texture.getFrameTextureData(0)[0]
		val rgbData = pixels.filter { (it shr 24) and 0xFF > 127 }.map { Color(it) }.toList()
		if (rgbData.isEmpty()) {
			SparksTools.logger.warn("Using fallback colour algo - no suitably opaque pixels for $stack")
			return getAverageColourBasic(pixels)
		}

		// Calculate standard deviation of brightnesses
		val brightnesses = rgbData.map { Color.RGBtoHSB(it.red, it.green, it.blue, null)[2] }.toList()
		val size = brightnesses.size
		var sum = 0.0
		var sumOfSquares = 0.0
		brightnesses.forEach {
			sum += it
			sumOfSquares += it * it
		}
		val mean = sum / size
		val stdDev = sqrt((sumOfSquares - sum * sum / size) / (size - 1))

		// Filter using standard deviation and calculate mean
		val filteredColours = rgbData.filterIndexed { i, _ -> abs(brightnesses[i] - mean) < stdDev }
			.map {
				val c = it.brighter()
				MutableTriple(c.red, c.green, c.blue)
			}.toList()
		if (filteredColours.isEmpty()) {
			SparksTools.logger.warn("Using fallback colour algo - no pixels within standard deviation for $stack")
			return getAverageColourBasic(pixels)
		}
		val sizeFiltered = filteredColours.size
		val colourMean = filteredColours.reduce { acc, colour ->
			acc.left += colour.left
			acc.middle += colour.middle
			acc.right += colour.right
			acc
		}
		colourMean.left /= sizeFiltered
		colourMean.middle /= sizeFiltered
		colourMean.right /= sizeFiltered
		return colourMean
	}

	@SideOnly(Side.CLIENT)
	private fun getAverageColourBasic(pixels: IntArray): MutableTriple<Int, Int, Int> {
		val rgb = MutableTriple(0, 0, 0)
		var num = 0
		pixels.forEach {
			val colour = Color(it)
			if (colour.alpha <= 0)
				return@forEach
			rgb.left += colour.red
			rgb.middle += colour.green
			rgb.right += colour.blue
			num++
		}
		rgb.left /= num
		rgb.middle /= num
		rgb.right /= num
		return rgb
	}
}