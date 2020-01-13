package brightspark.sparkstools.item

import brightspark.sparkstools.ToolUtils
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import java.util.stream.Stream

class ItemHammer(tool: CustomTool) : SHToolItem(tool) {
	override fun getBlocksToBreakIfEffective(stack: ItemStack, pos: BlockPos, side: Direction, player: PlayerEntity): Stream<BlockPos> =
		ToolUtils.getSquareBreakArea(stack, pos, side, player)
}