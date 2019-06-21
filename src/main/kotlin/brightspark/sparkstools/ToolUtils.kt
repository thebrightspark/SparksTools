package brightspark.sparkstools

import brightspark.sparkstools.item.SHToolItem
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.server.SPacketBlockChange
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.event.ForgeEventFactory

object ToolUtils {
	/**
	 * Gets the start and end [BlockPos] for the area to break
	 */
	fun getSquareBreakArea(stack: ItemStack, pos: BlockPos, sideHit: EnumFacing, player: EntityPlayer): Iterable<BlockPos> {
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

		return BlockPos.getAllInBox(start, end)
	}

	/**
	 * Gets all similar blocks connected to the input [pos]
	 */
	fun getConnectedBlocks(pos: BlockPos, world: World): Set<BlockPos> {
		val collected = HashSet<BlockPos>()
		collected += pos
		getConnectedBlocks(pos, world.getBlockState(pos), world, collected)
		return collected
	}

	/**
	 * Recursively collects all blocks connected horizontally, vertically or diagonally from the input
	 */
	private fun getConnectedBlocks(pos: BlockPos, refState: IBlockState, world: World, collected: MutableSet<BlockPos>) {
		val refBlock = refState.block
		val refProps = refState.propertyKeys
		BlockPos.getAllInBox(pos.add(-1, -1, -1), pos.add(1, 1, 1)).forEach {
			if (!collected.contains(it)) {
				val state = world.getBlockState(it)
				if (state.block == refBlock && state.propertyKeys == refProps && collected.add(it))
					getConnectedBlocks(it, refState, world, collected)
			}
		}
	}

	/**
	 * Tries to break the block at [pos] using the [stack]
	 */
	fun breakBlock(stack: ItemStack, world: World, player: EntityPlayer, pos: BlockPos, refPos: BlockPos) {
		val state = world.getBlockState(pos)
		val block = state.block
		if (world.isAirBlock(pos)
			|| stack.item !is SHToolItem
			|| !(stack.item as SHToolItem).isEffective(stack, state)
			|| !ForgeHooks.canHarvestBlock(block, player, world, pos))
			return

		// If block strength is too much stronger than the reference block, then don't break it
		val strength = ForgeHooks.blockStrength(state, player, world, pos)
		val refState = world.getBlockState(refPos)
		val refStrength = ForgeHooks.blockStrength(refState, player, world, refPos)
		if (refStrength / strength > 10F)
			return

		if (player.capabilities.isCreativeMode) {
			block.onBlockHarvested(world, pos, state, player)
			if (block.removedByPlayer(state, world, pos, player, false))
				block.onPlayerDestroy(world, pos, state)
			if (!world.isRemote && player is EntityPlayerMP)
				player.connection.sendPacket(SPacketBlockChange(world, pos))
			return
		}

		stack.onBlockDestroyed(world, state, pos, player)

		if (!world.isRemote && player is EntityPlayerMP) {
			val xp = ForgeHooks.onBlockBreakEvent(world, player.interactionManager.gameType, player, pos)
			if (xp == -1)
				return
			val te = world.getTileEntity(pos)
			if (block.removedByPlayer(state, world, pos, player, true)) {
				block.onPlayerDestroy(world, pos, state)
				block.harvestBlock(world, player, pos, state, te, stack)
				block.dropXpOnBlockBreak(world, pos, xp)
			}
			player.connection.sendPacket(SPacketBlockChange(world, pos))
		} else {
			if (block.removedByPlayer(state, world, pos, player, true))
				block.onPlayerDestroy(world, pos, state)
			stack.onBlockDestroyed(world, state, pos, player)

			if (stack.count == 0 && player.heldItemMainhand == stack) {
				ForgeEventFactory.onPlayerDestroyItem(player, stack, EnumHand.MAIN_HAND)
				player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY)
			}
			val mc = Minecraft.getMinecraft()
			mc.connection!!.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, mc.objectMouseOver.sideHit))
		}
	}
}