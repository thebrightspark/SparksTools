package brightspark.sparkstools

import brightspark.ksparklib.api.*
import brightspark.sparkstools.handler.ClientEventHandler
import brightspark.sparkstools.init.SHBlocks
import brightspark.sparkstools.init.SHItems
import brightspark.sparkstools.item.CustomToolData
import brightspark.sparkstools.item.SHToolItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraftforge.client.event.*
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent
import net.minecraftforge.fml.loading.FMLPaths
import java.io.FileReader
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path

@Mod(SparksTools.MOD_ID)
object SparksTools {
	const val MOD_ID = "sparkstools"

	private var customToolsFile: Path
	private val gson = Gson()
	val logger = getLogger()

	init {
//		addModListener<FMLClientSetupEvent> { SHItems.calcMissingMaterialColours() }
		addModListener<ModConfig.ModConfigEvent> { if (it.config.modId == MOD_ID) SHConfig.bake() }
		addModGenericListener<RegistryEvent.Register<Block>, Block> { SHBlocks.regBlocks(it.registry) }
		addModGenericListener<RegistryEvent.Register<Item>, Item> { event ->
			event.registry.let {
				SHBlocks.regItemBlocks(it)
				SHItems.regItems(it)
			}
		}
		runWhenOnClient {
			addModListener<TextureStitchEvent.Pre> { SHItems.regTextures(it) }
			addModListener<ModelRegistryEvent> { SHItems.regModels() }
			addModListener<ModelBakeEvent> { SHItems.regBakedModels(it) }
			addModListener<ColorHandlerEvent.Item> { SHItems.regColours(it) }
			addForgeListener<TickEvent.ClientTickEvent> { ClientEventHandler.updateSelection(it) }
			addForgeListener<RenderWorldLastEvent> { ClientEventHandler.renderSelection(it) }
		}
		addForgeListener<FMLServerAboutToStartEvent> {
			it.server.resourceManager.addReloadListener(DynamicRecipeDataPack)
		}

		val configDir = FMLPaths.CONFIGDIR.get().resolve(MOD_ID)
		Files.createDirectories(configDir)
		customToolsFile = configDir.resolve("custom_tools.json")
		if (Files.notExists(customToolsFile))
			this::class.java.getResourceAsStream("/assets/$MOD_ID/custom_tools_default.json").use {
				Files.copy(it, customToolsFile)
			}

		registerConfig(common = SHConfig.COMMON_SPEC)
	}

	val group = object : ItemGroup(MOD_ID) {
		private val sorter = Comparator<ItemStack> { o1, o2 ->
			val item1 = o1.item
			val item2 = o2.item
			return@Comparator if (item1 is SHToolItem && item2 is SHToolItem)
				item1.tool.harvestLevel.compareTo(item2.tool.harvestLevel)
			else if (item1 !is SHToolItem || item2 !is SHToolItem)
				if (item1 !is SHToolItem) -1 else 1
			else
				item1.registryName!!.compareTo(item2.registryName!!)
		}

		override fun createIcon() = ItemStack.EMPTY

		override fun getIcon() = SHItems.getTabIcon()

		override fun fill(items: NonNullList<ItemStack>) {
			super.fill(items)
			items.sortWith(sorter)
		}
	}

	fun readCustomTools(): List<CustomToolData> = gson.fromJson<List<CustomToolData>>(
		JsonReader(FileReader(customToolsFile.toFile()) as Reader),
		object : TypeToken<List<CustomToolData>>() {}.type
	)
}
