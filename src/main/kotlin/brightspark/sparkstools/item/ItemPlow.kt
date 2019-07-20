package brightspark.sparkstools.item

import brightspark.sparkstools.ToolUtils
import net.minecraft.block.BlockDirt
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
import net.minecraftforge.event.ForgeEventFactory

class ItemPlow(tool: CustomTool) : SHToolItem(tool) {
	companion object {
		private val tillableBlocks = arrayOf(Blocks.GRASS, Blocks.GRASS_PATH, Blocks.DIRT)
	}

	override fun getBlocksToBreakIfEffective(stack: ItemStack, pos: BlockPos, side: EnumFacing, player: EntityPlayer): Iterable<BlockPos> =
		ToolUtils.getSquareBreakArea(stack, pos, side, player)

	override fun getBlocksToSelect(stack: ItemStack, pos: BlockPos, side: EnumFacing, player: EntityPlayer): Iterable<BlockPos> {
		val state = player.world.getBlockState(pos)
		return when {
			tillableBlocks.contains(state.block) -> ToolUtils.getGroundBlocks(stack, pos, player, tillableBlocks)
			isEffective(stack, state) -> ToolUtils.getSquareBreakArea(stack, pos, side, player)
			else -> emptySet()
		}
	}

	override fun onItemUse(player: EntityPlayer, world: World, pos: BlockPos, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): EnumActionResult {
		val stack = player.getHeldItem(hand)
		if (facing == EnumFacing.DOWN || !world.isAirBlock(pos.up()))
			return EnumActionResult.PASS

		if (!tillableBlocks.contains(world.getBlockState(pos).block))
			return EnumActionResult.PASS

		var result = false
		if (facing == EnumFacing.UP)
			ToolUtils.getGroundBlocks(stack, pos, player, tillableBlocks)
				.forEach { result = tillBlock(player, stack, world, it) || result }
		else
			result = tillBlock(player, stack, world, pos)

		return if (result) EnumActionResult.SUCCESS else EnumActionResult.PASS
	}

	private fun tillBlock(player: EntityPlayer, stack: ItemStack, world: World, pos: BlockPos): Boolean {
		val result = ForgeEventFactory.onHoeUse(stack, player, world, pos)
		if (result != 0) {
			if (result > 0) stack.damageItem(1, player)
			return result > 0
		}

		val state = world.getBlockState(pos)
		when (state.block) {
			Blocks.GRASS, Blocks.GRASS_PATH -> Blocks.FARMLAND.defaultState
			Blocks.DIRT -> {
				when (state.getValue(BlockDirt.VARIANT)) {
					BlockDirt.DirtType.DIRT -> Blocks.FARMLAND.defaultState
					BlockDirt.DirtType.COARSE_DIRT -> Blocks.DIRT.defaultState.withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.DIRT)
					else -> null
				}
			}
			else -> null
		}?.let {
			world.playSound(player, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0f, 1.0f)
			if (!world.isRemote) {
				world.setBlockState(pos, it, 11)
				stack.damageItem(1, player)
			}
			return true
		}
		return false
	}
}