package brightspark.sparkstools.item

import brightspark.sparkstools.ToolUtils
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

class ItemPulverizer(tool: CustomTool) : SHToolItem(tool) {
	override fun getBlocksToBreakIfEffective(stack: ItemStack, pos: BlockPos, side: EnumFacing, player: EntityPlayer): Iterable<BlockPos> =
		ToolUtils.getSquareBreakArea(stack, pos, side, player)

	override fun isEffective(stack: ItemStack, state: IBlockState): Boolean {
		// TODO: Only effective against configured blocks
		return super.isEffective(stack, state)
	}
}