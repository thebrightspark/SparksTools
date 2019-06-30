package brightspark.sparkstools

import brightspark.sparkstools.init.SHItems
import brightspark.sparkstools.item.SHToolItem
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.NonNullList
import net.minecraftforge.client.event.ColorHandlerEvent
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.apache.logging.log4j.Logger
import java.io.File

@Mod(modid = SparksTools.MOD_ID, name = SparksTools.NAME, version = SparksTools.VERSION, modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter")
@Mod.EventBusSubscriber
object SparksTools {
    const val MOD_ID = "sparkstools"
    const val NAME = "Spark's Tools"
    const val VERSION = "@VERSION@"

    lateinit var logger: Logger

    @Mod.Instance(MOD_ID)
    lateinit var instance: SparksTools

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
    }

    @EventHandler
    fun init(event: FMLInitializationEvent) {
        if (event.side == Side.CLIENT)
            SHItems.calcMissingMaterialColours()
    }

    @SubscribeEvent
    @JvmStatic
    fun regItems(event: RegistryEvent.Register<Item>) = SHItems.init(event.registry)

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    @JvmStatic
    fun regModels(event: ModelRegistryEvent) = SHItems.regModels()

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    @JvmStatic
    fun regColours(event: ColorHandlerEvent.Item) = SHItems.regColours(event)
}
