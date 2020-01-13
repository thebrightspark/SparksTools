package brightspark.sparkstools

import brightspark.sparkstools.item.SHToolItem
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPlayerDiggingPacket
import net.minecraft.network.play.server.SChangeBlockPacket
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.math.RayTraceResult
import net.minecraft.world.World
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.event.ForgeEventFactory
import java.util.stream.Stream

object ToolUtils {
	/**
	 * Gets an [Iterable] of [BlockPos] for the square area to break
	 */
	fun getSquareBreakArea(stack: ItemStack, pos: BlockPos, sideHit: Direction, player: PlayerEntity): Stream<BlockPos> {
		val item = stack.item as SHToolItem
		val size = item.tool.effectSize
		val start = BlockPos.MutableBlockPos(pos)
		val end = BlockPos.MutableBlockPos(pos)

		// Offset area upwards if player is standing on ground and mining horizontally
		if (size > 1 && sideHit.axis.isHorizontal && player.onGround && pos.y == player.position.y + 1) {
			start.move(Direction.UP, size - 1)
			end.move(Direction.UP, size - 1)
		}

		// Move start and end positions to area corners
		when (sideHit) {
			Direction.NORTH, Direction.SOUTH -> {
				start.move(Direction.DOWN, size).move(Direction.WEST, size)
				end.move(Direction.UP, size).move(Direction.EAST, size)
			}
			Direction.WEST, Direction.EAST -> {
				start.move(Direction.DOWN, size).move(Direction.NORTH, size)
				end.move(Direction.UP, size).move(Direction.SOUTH, size)
			}
			Direction.UP, Direction.DOWN -> {
				start.move(Direction.WEST, size).move(Direction.NORTH, size)
				end.move(Direction.EAST, size).move(Direction.SOUTH, size)
			}
		}

		return BlockPos.getAllInBox(start, end).filter { item.isEffective(stack, player.world.getBlockState(it)) }
	}

	/**
	 * Gets an [Iterable] of [BlockPos] that match the [blockTypes] filter around the same Y level as the [pos]
	 */
	fun getGroundBlocks(stack: ItemStack, pos: BlockPos, world: World, blockTypes: Array<Block>): Stream<BlockPos> {
		val size = (stack.item as SHToolItem).tool.effectSize
		val start = pos.add(-size, 0, -size)
		val end = pos.add(size, 0, size)
		return BlockPos.getAllInBox(start, end).filter { blockTypes.contains(world.getBlockState(it).block) }
	}

	/**
	 * Gets an [Iterable] of [BlockPos] for the cube area to break
	 */
	fun getCubeBreakArea(stack: ItemStack, pos: BlockPos, player: PlayerEntity): Stream<BlockPos> {
		val item = stack.item as SHToolItem
		val size = item.tool.effectSize
		val start = BlockPos.MutableBlockPos(pos).move(Direction.UP, size).move(Direction.NORTH, size).move(Direction.WEST, size)
		val end = BlockPos.MutableBlockPos(pos).move(Direction.DOWN, size).move(Direction.SOUTH, size).move(Direction.EAST, size)
		return BlockPos.getAllInBox(start, end).filter { item.isEffective(stack, player.world.getBlockState(it)) }
	}

	/**
	 * Gets all similar blocks connected to the input [pos]
	 */
	// TODO: Change to use a stream builder? Stream.builder()
	fun getConnectedBlocks(pos: BlockPos, world: World, max: Int): Stream<BlockPos> {
		val collected = HashSet<BlockPos>()
		collected += pos
		getConnectedBlocks(pos, world.getBlockState(pos), world, collected, max)
		return collected.stream()
	}

	/**
	 * Recursively collects all blocks connected horizontally, vertically or diagonally from the input
	 */
	private fun getConnectedBlocks(pos: BlockPos, refState: BlockState, world: World, collected: MutableSet<BlockPos>, max: Int) {
		BlockPos.getAllInBoxMutable(pos.add(-1, -1, -1), pos.add(1, 1, 1)).forEach {
			if (collected.size >= max)
				return
			if (!collected.contains(it) && world.getBlockState(it) == refState) {
				val immutablePos = BlockPos(it)
				collected += immutablePos
				getConnectedBlocks(immutablePos, refState, world, collected, max)
			}
		}
	}

	/**
	 * Tries to break the block at [pos] using the [stack]
	 */
	fun breakBlock(stack: ItemStack, world: World, player: PlayerEntity, pos: BlockPos, refPos: BlockPos) {
		val state = world.getBlockState(pos)
		val fluidState = world.getFluidState(pos)
		val block = state.block
		if (world.isAirBlock(pos)
			|| stack.item !is SHToolItem
			|| !(stack.item as SHToolItem).isEffective(stack, state)
			|| !ForgeHooks.canHarvestBlock(state, player, world, pos))
			return

		// If block strength is too much stronger than the reference block, then don't break it
		val strength = state.getBlockHardness(world, pos)
		val refState = world.getBlockState(refPos)
		val refStrength = refState.getBlockHardness(world, refPos)
		if (refStrength / strength > 10F)
			return

		if (player.isCreative) {
			block.onBlockHarvested(world, pos, state, player)
			if (block.removedByPlayer(state, world, pos, player, false, fluidState))
				block.onPlayerDestroy(world, pos, state)
			if (!world.isRemote && player is ServerPlayerEntity)
				player.connection.sendPacket(SChangeBlockPacket(world, pos))
			return
		}

		stack.onBlockDestroyed(world, state, pos, player)

		if (!world.isRemote && player is ServerPlayerEntity) {
			val xp = ForgeHooks.onBlockBreakEvent(world, player.interactionManager.gameType, player, pos)
			if (xp == -1)
				return
			val te = world.getTileEntity(pos)
			if (block.removedByPlayer(state, world, pos, player, true, fluidState)) {
				block.onPlayerDestroy(world, pos, state)
				block.harvestBlock(world, player, pos, state, te, stack)
				block.dropXpOnBlockBreak(world, pos, xp)
			}
			player.connection.sendPacket(SChangeBlockPacket(world, pos))
		} else {
			if (block.removedByPlayer(state, world, pos, player, true, fluidState))
				block.onPlayerDestroy(world, pos, state)
			stack.onBlockDestroyed(world, state, pos, player)

			if (stack.count == 0 && player.heldItemMainhand == stack) {
				ForgeEventFactory.onPlayerDestroyItem(player, stack, Hand.MAIN_HAND)
				player.setHeldItem(Hand.MAIN_HAND, ItemStack.EMPTY)
			}
			val mc = Minecraft.getInstance()
			val sideHit = mc.objectMouseOver.let {
				if (it is BlockRayTraceResult && it.type == RayTraceResult.Type.BLOCK)
					it.face
				else
					Direction.DOWN
			}
			mc.connection!!.sendPacket(CPlayerDiggingPacket(CPlayerDiggingPacket.Action.STOP_DESTROY_BLOCK, pos, sideHit))
		}
	}
}
