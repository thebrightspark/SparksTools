package brightspark.sparkstools.item

import brightspark.sparkstools.SparksTools
import brightspark.sparkstools.ToolUtils
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

abstract class SHToolItem(val tool: CustomTool) : Item() {
    init {
	    registryName = tool.registryName
        translationKey = tool.registryName.path
        creativeTab = SparksTools.tab
    }

	internal abstract fun getBlocksToBreakIfEffective(stack: ItemStack, pos: BlockPos, side: EnumFacing, player: EntityPlayer): Iterable<BlockPos>

	fun getBlocksToBreak(stack: ItemStack, pos: BlockPos, side: EnumFacing, player: EntityPlayer): Iterable<BlockPos> =
		if (isEffective(stack, player.world.getBlockState(pos)))
			getBlocksToBreakIfEffective(stack, pos, side, player)
		else
			emptySet()

    open fun breakBlocks(stack: ItemStack, pos: BlockPos, sideHit: EnumFacing, player: EntityPlayer, breakInputPos: Boolean = false) {
	    getBlocksToBreak(stack, pos, sideHit, player)
		    .filter { (breakInputPos || it != pos) && !super.onBlockStartBreak(stack, it, player) }
		    .forEach { ToolUtils.breakBlock(stack, player.world, player, it, pos) }
    }

	fun isEffective(stack: ItemStack, state: IBlockState): Boolean =
		getToolClasses(stack).any { state.block.isToolEffective(it, state) } ||
			tool.type.effectiveMaterials.contains(state.material)

    override fun onBlockStartBreak(stack: ItemStack, pos: BlockPos, player: EntityPlayer): Boolean {
        @Suppress("UNNECESSARY_SAFE_CALL")
        rayTrace(player.world, player, false)?.let { breakBlocks(stack, pos, it.sideHit, player) }
        return super.onBlockStartBreak(stack, pos, player)
    }

    override fun canHarvestBlock(state: IBlockState, stack: ItemStack): Boolean {
        val requiredTool = state.block.getHarvestTool(state)
        return state.material.isToolNotRequired || requiredTool == null ||
            getHarvestLevel(stack, requiredTool, null, state) >= state.block.getHarvestLevel(state)
    }

    override fun getDestroySpeed(stack: ItemStack, state: IBlockState): Float =
        if (isEffective(stack, state)) tool.efficiency else 1F

    override fun getHarvestLevel(stack: ItemStack, toolClass: String, player: EntityPlayer?, blockState: IBlockState?): Int =
	    tool.harvestLevel

    override fun getToolClasses(stack: ItemStack): Set<String> = tool.type.toolTypes
}