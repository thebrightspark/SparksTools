package brightspark.sparkstools.item

import brightspark.sparkstools.ToolUtils
import brightspark.sparkstools.lumberAxeMaxBlocks
import net.minecraft.block.BlockLog
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

class ItemLumberAxe(tool: CustomTool) : SHToolItem(tool) {
	override fun getBlocksToBreakIfEffective(stack: ItemStack, pos: BlockPos, side: EnumFacing, player: EntityPlayer): Iterable<BlockPos> {
		val world = player.world
		return if (!player.isSneaking && world.getBlockState(pos).block is BlockLog)
			ToolUtils.getConnectedBlocks(pos, world, lumberAxeMaxBlocks)
		else
			ToolUtils.getSquareBreakArea(stack, pos, side, player)
	}
}