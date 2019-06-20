package brightspark.sparkstools

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object ToolUtils {
	/**
	 * Gets the start and end [BlockPos] for the area to break
	 */
	fun getBreakArea(stack: ItemStack, pos: BlockPos, sideHit: EnumFacing, player: EntityPlayer): Pair<BlockPos, BlockPos> {
		//val item = stack.item as SHToolItem
		val mineSize = 1 // TEMP
		val start = BlockPos.MutableBlockPos(pos)
		val end = BlockPos.MutableBlockPos(pos)

		// Offset area upwards if player is standing on ground and mining horizontally
		if (!player.capabilities.isFlying && sideHit.axis.isHorizontal && mineSize > 1) {
			start.move(EnumFacing.UP, mineSize - 1)
			end.move(EnumFacing.UP, mineSize - 1)
		}

		// Move start and end positions to area corners
		when (sideHit) {
			EnumFacing.NORTH, EnumFacing.SOUTH -> {
				start.move(EnumFacing.DOWN, mineSize).move(EnumFacing.WEST, mineSize)
				end.move(EnumFacing.UP, mineSize).move(EnumFacing.EAST, mineSize)
			}
			EnumFacing.WEST, EnumFacing.EAST -> {
				start.move(EnumFacing.DOWN, mineSize).move(EnumFacing.NORTH, mineSize)
				end.move(EnumFacing.UP, mineSize).move(EnumFacing.SOUTH, mineSize)
			}
			EnumFacing.UP, EnumFacing.DOWN -> {
				start.move(EnumFacing.WEST, mineSize).move(EnumFacing.NORTH, mineSize)
				end.move(EnumFacing.EAST, mineSize).move(EnumFacing.SOUTH, mineSize)
			}
		}

		return start.toImmutable() to end.toImmutable()
	}

	/**
	 * Tries to break the block at [pos] using the [stack]
	 */
	fun breakBlock(stack: ItemStack, world: World, player: EntityPlayer, pos: BlockPos, refPos: BlockPos) {
		// TODO: Block breaking
	}
}