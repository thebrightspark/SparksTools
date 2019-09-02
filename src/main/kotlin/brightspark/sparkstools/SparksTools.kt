package brightspark.sparkstools

import brightspark.sparkstools.init.SHBlocks
import brightspark.sparkstools.init.SHItems
import brightspark.sparkstools.init.SHRecipes
import brightspark.sparkstools.item.SHToolItem
import net.minecraft.block.Block
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.NonNullList
import net.minecraftforge.client.event.ColorHandlerEvent
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.common.config.Config
import net.minecraftforge.common.config.ConfigManager
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.client.event.ConfigChangedEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.Logger
import java.io.File

@Mod(modid = SparksTools.MOD_ID, version = SparksTools.VERSION, useMetadata = true,
    dependencies = "required-after:forgelin;",
    modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter")
@Mod.EventBusSubscriber
object SparksTools {
    const val MOD_ID = "sparkstools"
    const val VERSION = "@VERSION@"

    lateinit var logger: Logger
    lateinit var customToolsFile: File

    val tab = object : CreativeTabs(MOD_ID) {
        private val sorter = Comparator<ItemStack> { o1, o2 ->
            val item1 = o1.item
            val item2 = o2.item
            return@Comparator if (item1 is SHToolItem && item2 is SHToolItem)
                item1.tool.material[0].item.registryName!!.compareTo(item2.tool.material[0].item.registryName!!)
            else if (item1 !is SHToolItem || item2 !is SHToolItem)
                if (item1 !is SHToolItem) -1 else 1
            else
                item1.registryName!!.compareTo(item2.registryName!!)
        }

        // Unused
        override fun createIcon(): ItemStack = ItemStack.EMPTY

        override fun getIcon(): ItemStack = SHItems.getTabIcon()

        override fun displayAllRelevantItems(stacks: NonNullList<ItemStack>) {
            super.displayAllRelevantItems(stacks)
            stacks.sortWith(sorter)
        }
    }

    @EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        logger = event.modLog
        val configDir = File(event.modConfigurationDirectory, MOD_ID)
        configDir.mkdirs()
        customToolsFile = File(configDir, "custom_tools.json")
        if (!customToolsFile.exists())
            this::class.java.getResourceAsStream("/assets/$MOD_ID/custom_tools_default.json").use { FileUtils.copyToFile(it, customToolsFile) }
    }

    @EventHandler
    fun init(event: FMLInitializationEvent) {
        if (event.side == Side.CLIENT)
            SHItems.calcMissingMaterialColours()
    }

	@SubscribeEvent
	@JvmStatic
	fun configChanged(event: ConfigChangedEvent.OnConfigChangedEvent) {
		if (event.modID == MOD_ID)
			ConfigManager.sync(MOD_ID, Config.Type.INSTANCE)
	}

    @SubscribeEvent
    @JvmStatic
    fun regBlocks(event: RegistryEvent.Register<Block>) = SHBlocks.regBlocks(event.registry)

    @SubscribeEvent
    @JvmStatic
    fun regItems(event: RegistryEvent.Register<Item>) {
        val registry = event.registry
        SHBlocks.regItemBlocks(registry)
        SHItems.regItems(registry)
    }

    @SubscribeEvent
    @JvmStatic
    fun regRecipes(event: RegistryEvent.Register<IRecipe>) = SHRecipes.regRecipes(event.registry)

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    @JvmStatic
    fun regModels(event: ModelRegistryEvent) {
        SHItems.regModels()
        SHBlocks.regModels()
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    @JvmStatic
    fun regColours(event: ColorHandlerEvent.Item) = SHItems.regColours(event)
}
