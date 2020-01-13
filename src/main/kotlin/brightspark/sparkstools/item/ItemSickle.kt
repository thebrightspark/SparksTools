package brightspark.sparkstools.item

import brightspark.ksparklib.api.damageItem
import brightspark.sparkstools.ToolUtils
import net.minecraft.block.BlockState
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.stream.Stream

class ItemSickle(tool: CustomTool) : SHToolItem(tool) {
	override fun getBlocksToBreakIfEffective(stack: ItemStack, pos: BlockPos, side: Direction, player: PlayerEntity): Stream<BlockPos> =
		ToolUtils.getCubeBreakArea(stack, pos, player)

	override fun onBlockDestroyed(stack: ItemStack, worldIn: World, state: BlockState, pos: BlockPos, entityLiving: LivingEntity): Boolean {
		if (!worldIn.isRemote)
			stack.damageItem(entityLiving)
		return true
	}
}