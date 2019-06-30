package brightspark.sparkstools.item

import brightspark.sparkstools.ToolUtils
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.SoundEvents
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class ItemExcavator(tool: CustomTool) : SHToolItem(tool) {
	companion object {
		private val pathableBlocks = arrayOf<Block>(Blocks.GRASS)
	}

	override fun getBlocksToBreakIfEffective(stack: ItemStack, pos: BlockPos, side: EnumFacing, player: EntityPlayer): Iterable<BlockPos> =
		ToolUtils.getSquareBreakArea(stack, pos, side, player)

	override fun onItemUse(player: EntityPlayer, world: World, pos: BlockPos, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): EnumActionResult {
		val stack = player.getHeldItem(hand)
		if (facing == EnumFacing.DOWN || !world.isAirBlock(pos.up()))
			return EnumActionResult.PASS

		if (world.getBlockState(pos).block != Blocks.GRASS)
			return EnumActionResult.PASS

		var result = false
		if (facing == EnumFacing.UP)
			ToolUtils.getGroundBlocks(stack, pos, player, pathableBlocks)
				.forEach { result = createPath(player, stack, world, it) || result }
		else
			result = createPath(player, stack, world, pos)

		return if (result) EnumActionResult.SUCCESS else EnumActionResult.PASS
	}

	private fun createPath(player: EntityPlayer, stack: ItemStack, world: World, pos: BlockPos): Boolean {
		if (world.getBlockState(pos).block != Blocks.GRASS)
			return false

		world.playSound(player, pos, SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1.0f, 1.0f)
		if (!world.isRemote) {
			world.setBlockState(pos, Blocks.GRASS_PATH.defaultState, 11)
			stack.damageItem(1, player)
		}
		return true
	}
}