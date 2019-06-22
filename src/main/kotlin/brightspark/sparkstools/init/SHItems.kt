package brightspark.sparkstools.init

import brightspark.sparkstools.SparksTools
import brightspark.sparkstools.item.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.registries.IForgeRegistry
import java.io.FileReader
import java.io.Reader

object SHItems {
	private val items = HashSet<Item>()

	fun init(registry: IForgeRegistry<Item>) {
		val customTools = Gson().fromJson<List<CustomTool>>(
			JsonReader(FileReader(SparksTools.customToolsFile) as Reader?),
			object : TypeToken<List<CustomTool>>() {}.type
		)
		customTools.forEach {
			val item = when (it.type) {
				ToolType.HAMMER -> ItemHammer(it)
				ToolType.EXCAVATOR -> ItemExcavator(it)
				ToolType.LUMBER_AXE -> ItemLumberAxe(it)
			}
			SparksTools.logger.info("Registering ${item.registryName} -> ${item.tool}")
			items += item
			registry.register(item)
		}
	}

	fun regModels() =
		items.forEach { ModelLoader.setCustomModelResourceLocation(it, 0, ModelResourceLocation(it.registryName!!, "inventory")) }
}