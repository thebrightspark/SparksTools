package brightspark.sparkstools.block

import net.minecraft.block.Block
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material

class BlockCompressedCobble : Block(Properties.create(Material.ROCK).hardnessAndResistance(3F, 15F).sound(SoundType.STONE)) {
	init {
		setRegistryName("compressed_cobblestone")
	}
}
