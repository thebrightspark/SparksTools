package brightspark.sparkstools.item

import brightspark.sparkstools.ToolUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

class ItemHammer(tool: CustomTool) : SHToolItem(tool) {
	override fun getBlocksToBreak(stack: ItemStack, pos: BlockPos, side: EnumFacing, player: EntityPlayer): Iterable<BlockPos> =
		ToolUtils.getSquareBreakArea(stack, pos, side, player)
}