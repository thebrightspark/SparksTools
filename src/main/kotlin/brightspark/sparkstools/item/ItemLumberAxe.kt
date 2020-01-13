package brightspark.sparkstools.item

import brightspark.sparkstools.SHConfig
import brightspark.sparkstools.ToolUtils
import net.minecraft.block.LogBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import java.util.stream.Stream

class ItemLumberAxe(tool: CustomTool) : SHToolItem(tool) {
	override fun getBlocksToBreakIfEffective(stack: ItemStack, pos: BlockPos, side: Direction, player: PlayerEntity): Stream<BlockPos> {
		val world = player.world
		return if (!player.isSneaking && world.getBlockState(pos).block is LogBlock)
			ToolUtils.getConnectedBlocks(pos, world, SHConfig.lumberAxeMaxBlocks)
		else
			ToolUtils.getSquareBreakArea(stack, pos, side, player)
	}
}
