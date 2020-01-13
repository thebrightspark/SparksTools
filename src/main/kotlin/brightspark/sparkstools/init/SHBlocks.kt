package brightspark.sparkstools.init

import brightspark.sparkstools.SparksTools
import brightspark.sparkstools.block.BlockCompressedCobble
import net.minecraft.block.Block
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraftforge.registries.IForgeRegistry

object SHBlocks {
	val compressedCobblestone = BlockCompressedCobble()

	fun regBlocks(registry: IForgeRegistry<Block>) = registry.register(compressedCobblestone)

	fun regItemBlocks(registry: IForgeRegistry<Item>) =
		registry.register(BlockItem(compressedCobblestone, Item.Properties().group(SparksTools.group)).setRegistryName(compressedCobblestone.registryName))
}
