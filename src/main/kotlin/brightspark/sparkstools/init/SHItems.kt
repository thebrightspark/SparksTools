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
import kotlin.math.abs
import kotlin.math.sqrt

object SHItems {
	private val tools = ArrayList<SHToolItem>()
	private var lastTabIconSecond = 0L
	private var lastTabIconStack = ItemStack.EMPTY

	fun getTabIcon(): ItemStack {
		val second = System.currentTimeMillis() / 1000
		if (second != lastTabIconSecond) {
			lastTabIconSecond = second
			lastTabIconStack = ItemStack(tools[(second % tools.size).toInt()])
		}
		return lastTabIconStack
	}

	fun init(registry: IForgeRegistry<Item>) {
		val customTools = Gson().fromJson<List<CustomToolData>>(
			JsonReader(FileReader(SparksTools.customToolsFile) as Reader?),
			object : TypeToken<List<CustomToolData>>() {}.type
		)
		customTools.mapNotNull {
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
			}
			SparksTools.logger.info("Registering ${item.registryName} -> ${item.tool}")
			tools += item
			registry.register(item)
		}
	}

	@SideOnly(Side.CLIENT)
	fun regModels() {
		tools.forEach {
			ModelLoader.setCustomModelResourceLocation(it, 0,
				ModelResourceLocation("${it.registryName!!.namespace}:${it.tool.type.name.toLowerCase(Locale.ROOT)}", "inventory")
			)
		}
	}

	@SideOnly(Side.CLIENT)
	fun regColours(event: ColorHandlerEvent.Item) {
		// Register item colour handler
		// A compiler error appears here in IntelliJ IDEA, but it builds fine
		event.itemColors.registerItemColorHandler(
			{ stack: ItemStack, tintIndex: Int ->
				val colours = (stack.item as SHToolItem).tool.textureColours
				if (tintIndex > 0 && tintIndex < colours.size) colours[tintIndex] else -1
			},
			tools.toTypedArray()
		)
	}

	@SideOnly(Side.CLIENT)
	fun calcMissingMaterialColours() {
		SparksTools.logger.info("Starting to calculate tool colours for tools missing colours")
		tools.filter { it.tool.textureColours.isEmpty() }.forEach { calcMaterialColour(it.tool) }
		SparksTools.logger.info("Finished calculating tool colours")
		val toolsMissingColours = tools.filter { it.tool.textureColours.isEmpty() }.joinToString("\n", transform = { it.tool.toString() })
		if (toolsMissingColours.isNotEmpty())
			SparksTools.logger.warn("The following tools are missing colours! This is most likely due to missing material textures - find warnings and errors above regarding them for more details. They will appear uncoloured in-game:\n$toolsMissingColours")
	}

	/**
	 * Calculates the material colour for the [tool] and sets it to the [CustomTool.textureColours]
	 */
	@SideOnly(Side.CLIENT)
	private fun calcMaterialColour(tool: CustomTool) {
		val colours = tool.material.mapNotNull { getAverageColour(it) }.toSet()
		if (colours.isEmpty()) {
			SparksTools.logger.error("Found no valid material to calculate a colour for the tool $tool")
			return
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
		SparksTools.logger.info("Calculated the colour $colour ($rgb) for the tool $tool")
		tool.textureColours = listOf(-1, rgb)
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
		val rgbData = ArrayList<Triple<Int, Int, Int>>()
		val hsbData = ArrayList<Triple<Float, Float, Float>>()
		val hsbMean = MutableTriple(0F, 0F, 0F)
		var weightTotal = 0F
		pixels.forEach {
			if ((it shr 24) and 0xFF < 128)
				return@forEach
			val colour = Color(it)
			rgbData += Triple(colour.red, colour.green, colour.blue)
			val colourHsb = Color.RGBtoHSB(colour.red, colour.green, colour.blue, null)
			hsbData += Triple(colourHsb[0], colourHsb[1], colourHsb[2])
			val weight = calcWeight(colourHsb[2])
			hsbMean.left += colourHsb[0] * weight
			hsbMean.middle += colourHsb[1] * weight
			hsbMean.right += colourHsb[2] * weight
			weightTotal += weight
		}
		if (hsbData.isEmpty()) {
			SparksTools.logger.warn("Using fallback colour algo - no suitably opaque pixels for $stack")
			return getAverageColourBasic(pixels)
		}

		hsbMean.left /= weightTotal
		hsbMean.middle /= weightTotal
		hsbMean.right /= weightTotal
		val hsbStdDev = weightedStdDev(hsbData, hsbMean, weightTotal)
		val rgbBin = MutableTriple(0, 0, 0)
		var total = 0
		hsbData.forEachIndexed { i, hsb ->
			if (!withinStdDev(hsb, hsbMean, hsbStdDev))
				return@forEachIndexed
			val rgb = rgbData[i]
			rgbBin.left += rgb.first
			rgbBin.middle += rgb.second
			rgbBin.right += rgb.third
			total++
		}
		return if (total == 0) {
			SparksTools.logger.warn("Using fallback colour algo - no pixels in 1 stddev for $stack")
			getAverageColourBasic(pixels)
		}
		else {
			rgbBin.left /= total
			rgbBin.middle /= total
			rgbBin.right /= total
			rgbBin
		}
	}

	private fun calcWeight(b: Float): Float =
		if (b > 0.8F) 1F else if (b > 0.5F) 0.5F else 0.1F

	private fun weightedStdDev(hsb: ArrayList<Triple<Float, Float, Float>>, hsbMean: MutableTriple<Float, Float, Float>, weightTotal: Float): Triple<Double, Double, Double> {
		val acc = MutableTriple(0F, 0F, 0F)
		hsb.forEach {
			acc.left += weightedStdDevAcc(it.first, hsbMean.left)
			acc.middle += weightedStdDevAcc(it.second, hsbMean.middle)
			acc.right += weightedStdDevAcc(it.third, hsbMean.right)
		}
		val hsbSize = hsb.size.toFloat()
		return Triple(
			sqrt((acc.left * hsbSize / ((hsbSize - 1) * weightTotal)).toDouble()),
			sqrt((acc.middle * hsbSize / ((hsbSize - 1) * weightTotal)).toDouble()),
			sqrt((acc.right * hsbSize / ((hsbSize - 1) * weightTotal)).toDouble())
		)
	}

	private fun weightedStdDevAcc(hsbPart: Float, hsbMeanPart: Float): Float {
		val diff = hsbPart - hsbMeanPart
		return diff * diff * calcWeight(hsbPart)
	}

	private fun withinStdDev(hsb: Triple<Float, Float, Float>, hsbMean: MutableTriple<Float, Float, Float>, hsbStdDev: Triple<Double, Double, Double>): Boolean =
		abs(hsb.first - hsbMean.left) <= hsbStdDev.first &&
			abs(hsb.second - hsbMean.middle) <= hsbStdDev.second &&
			abs(hsb.third - hsbMean.right) <= hsbStdDev.third

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

	// Old basic averaging
	/*@SideOnly(Side.CLIENT)
	private fun getAverageColour(stack: ItemStack): MutableTriple<Int, Int, Int> {
		val model = Minecraft.getMinecraft().renderItem.getItemModelWithOverrides(stack, null, null)
		val texture = model.particleTexture
		val pixels = texture.getFrameTextureData(0)[0]
		val rgb = MutableTriple(0, 0, 0)
		var num = 0
		pixels.forEach { pixel: Int ->
			val colour = Color(pixel)
			if (colour.alpha != 255 || (colour.red <= 10 && colour.green <= 10 && colour.blue <= 10))
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
	}*/
}