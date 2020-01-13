package brightspark.sparkstools.item

import brightspark.ksparklib.api.damageItem
import brightspark.sparkstools.ToolUtils
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUseContext
import net.minecraft.util.*
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.stream.Stream

class ItemExcavator(tool: CustomTool) : SHToolItem(tool) {
	companion object {
		private val pathableBlocks = arrayOf<Block>(Blocks.GRASS)
	}

	override fun getBlocksToBreakIfEffective(stack: ItemStack, pos: BlockPos, side: Direction, player: PlayerEntity): Stream<BlockPos> =
		ToolUtils.getSquareBreakArea(stack, pos, side, player)

	override fun onItemUse(context: ItemUseContext): ActionResultType {
		val world = context.world
		val player = context.player
		val stack = context.item
		val pos = context.pos
		val facing = context.face
		if (facing == Direction.DOWN || !world.isAirBlock(pos.up()))
			return ActionResultType.PASS

		if (world.getBlockState(pos).block != Blocks.GRASS)
			return ActionResultType.PASS

		var result = false
		if (facing == Direction.UP)
			ToolUtils.getGroundBlocks(stack, pos, world, pathableBlocks)
				.forEach { result = createPath(player, stack, world, it) || result }
		else
			result = createPath(player, stack, world, pos)

		return if (result) ActionResultType.SUCCESS else ActionResultType.PASS
	}

	private fun createPath(player: PlayerEntity?, stack: ItemStack, world: World, pos: BlockPos): Boolean {
		if (world.getBlockState(pos).block != Blocks.GRASS)
			return false

		world.playSound(player, pos, SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1.0f, 1.0f)
		if (!world.isRemote) {
			world.setBlockState(pos, Blocks.GRASS_PATH.defaultState, 11)
			stack.damageItem(world, player)
		}
		return true
	}
}
