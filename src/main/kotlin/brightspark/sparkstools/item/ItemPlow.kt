package brightspark.sparkstools.item

import brightspark.ksparklib.api.damageItem
import brightspark.ksparklib.api.toBlockPosList
import brightspark.sparkstools.ToolUtils
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUseContext
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.SoundCategory
import net.minecraft.util.SoundEvents
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraftforge.event.ForgeEventFactory
import java.util.stream.Stream

class ItemPlow(tool: CustomTool) : SHToolItem(tool) {
	companion object {
		private val tillableBlocks = arrayOf(Blocks.GRASS_BLOCK, Blocks.GRASS_PATH, Blocks.DIRT)
	}

	override fun getBlocksToBreakIfEffective(stack: ItemStack, pos: BlockPos, side: Direction, player: PlayerEntity): Stream<BlockPos> =
		ToolUtils.getSquareBreakArea(stack, pos, side, player)

	override fun getBlocksToSelect(stack: ItemStack, pos: BlockPos, side: Direction, player: PlayerEntity): List<BlockPos> {
		val state = player.world.getBlockState(pos)
		return when {
			tillableBlocks.contains(state.block) -> ToolUtils.getGroundBlocks(stack, pos, player.world, tillableBlocks).toBlockPosList()
			isEffective(stack, state) -> ToolUtils.getSquareBreakArea(stack, pos, side, player).toBlockPosList()
			else -> emptyList()
		}
	}

	override fun onItemUse(context: ItemUseContext): ActionResultType {
		val world = context.world
		val stack = context.item
		val facing = context.face
		val pos = context.pos
		if (facing == Direction.DOWN || !world.isAirBlock(pos.up()))
			return ActionResultType.PASS

		if (!tillableBlocks.contains(world.getBlockState(pos).block))
			return ActionResultType.PASS

		var result = false
		if (facing == Direction.UP)
			ToolUtils.getGroundBlocks(stack, pos, world, tillableBlocks)
				.forEach { result = tillBlock(PlowUseContext(context, it)) || result }
		else
			result = tillBlock(context)

		return if (result) ActionResultType.SUCCESS else ActionResultType.PASS
	}

	private fun tillBlock(context: ItemUseContext): Boolean {
		val stack = context.item
		val player = context.player
		val world = context.world
		val pos = context.pos
		val result = ForgeEventFactory.onHoeUse(context)
		if (result != 0) {
			if (result > 0) stack.damageItem(world, player)
			return result > 0
		}

		val state = world.getBlockState(pos)
		when (state.block) {
			Blocks.GRASS_BLOCK, Blocks.GRASS_PATH -> Blocks.FARMLAND
			Blocks.DIRT -> Blocks.FARMLAND
			Blocks.COARSE_DIRT -> Blocks.DIRT
			else -> null
		}?.let {
			world.playSound(player, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0f, 1.0f)
			if (!world.isRemote) {
				world.setBlockState(pos, it.defaultState, 11)
				stack.damageItem(world, player)
			}
			return true
		}
		return false
	}

	private class PlowUseContext(context: ItemUseContext, pos: BlockPos) : ItemUseContext(context.world, context.player, context.hand, context.item, BlockRayTraceResult(context.hitVec, context.face, pos, context.func_221533_k()))
}
