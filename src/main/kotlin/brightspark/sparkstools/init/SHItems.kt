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
import net.minecraftforge.registries.IForgeRegistry
import org.apache.commons.lang3.tuple.MutableTriple
import java.awt.Color
import java.io.FileReader
import java.io.Reader
import java.util.*
import kotlin.collections.HashSet

object SHItems {
	private val tools = HashSet<SHToolItem>()

	fun init(registry: IForgeRegistry<Item>) {
		val customTools = Gson().fromJson<List<CustomToolData>>(
			JsonReader(FileReader(SparksTools.customToolsFile) as Reader?),
			object : TypeToken<List<CustomToolData>>() {}.type
		)
		customTools.map { CustomTool(it) }.forEach {
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
	fun calcMissingMaterialColours() =
		tools.filter { it.tool.textureColours.isEmpty() }.forEach { calcMaterialColour(it.tool) }

	/**
	 * Calculates the material colour for the [tool] and sets it to the [CustomTool.textureColours]
	 */
	@SideOnly(Side.CLIENT)
	private fun calcMaterialColour(tool: CustomTool) {
		val material = tool.getMaterialStack() ?: tool.getMaterialOres()
		val colours = material.map { getAverageColour(it) }.toSet()
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
	}

	// This doesn't get the resource...
	/*@SideOnly(Side.CLIENT)
	private fun getAverageColour(stack: ItemStack): MutableTriple<Int, Int, Int> {
		val itemRegName = stack.item.registryName!!
		val textureResLoc = ResourceLocation(itemRegName.namespace, "textures/items/${itemRegName.path}.png")
		val resource = Minecraft.getMinecraft().resourceManager.getResource(textureResLoc)
		val image = ImageIO.read(resource.inputStream)
		val rgb = MutableTriple(0, 0, 0)
		var num = 0
		(0..image.width).forEach { x ->
			(0..image.height).forEach loop@{ y ->
				val pixel = Color(image.getRGB(x, y))
				if (pixel.alpha != 255 || (pixel.red <= 10 && pixel.green <= 10 && pixel.blue <= 10))
					return@loop
				rgb.left += pixel.red
				rgb.middle += pixel.green
				rgb.right += pixel.blue
				num++
			}
		}
		rgb.left /= num
		rgb.middle /= num
		rgb.right /= num
		return rgb
	}*/
}