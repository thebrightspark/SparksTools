package brightspark.sparkstools.init

import brightspark.sparkstools.block.BlockCompressedCobble
import net.minecraft.block.Block
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.minecraftforge.registries.IForgeRegistry

object SHBlocks {
	val compressedCobblestone = BlockCompressedCobble()

	fun regBlocks(registry: IForgeRegistry<Block>) = registry.register(compressedCobblestone)

	fun regItemBlocks(registry: IForgeRegistry<Item>) =
		registry.register(ItemBlock(compressedCobblestone).setRegistryName(compressedCobblestone.registryName))

	@SideOnly(Side.CLIENT)
	fun regModels() {
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(compressedCobblestone), 0,
			ModelResourceLocation(compressedCobblestone.registryName!!, "inventory"))
	}
}