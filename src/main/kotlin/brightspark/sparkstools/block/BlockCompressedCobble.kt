package brightspark.sparkstools.block

import brightspark.sparkstools.SparksTools
import net.minecraft.block.Block
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material

class BlockCompressedCobble : Block(Material.ROCK) {
	init {
		setRegistryName("compressed_cobblestone")
		translationKey = "compressed_cobblestone"
		creativeTab = SparksTools.tab
		setHardness(3F)
		setResistance(15F)
		soundType = SoundType.STONE
	}
}