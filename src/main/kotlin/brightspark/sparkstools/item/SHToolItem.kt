package brightspark.sparkstools.item

import brightspark.sparkstools.SparksTools
import brightspark.sparkstools.ToolUtils
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

class SHToolItem(name: String, private val material: SHToolMaterial) : Item() {
    init {
        setRegistryName(name)
        translationKey = name
        creativeTab = SparksTools.tab
    }

    fun breakBlocks(stack: ItemStack, pos: BlockPos, sideHit: EnumFacing, player: EntityPlayer, breakInputPos: Boolean = false) {
        val positions = ToolUtils.getBreakArea(stack, pos, sideHit, player)
        val start = positions.first
        val end = positions.second
        for (x in start.x..end.x) {
            for (y in start.y..end.y) {
                for (z in start.z..end.z) {
                    val posToBreak = BlockPos(x, y, z)
                    if (posToBreak == pos && !breakInputPos)
                        continue
                    if (!super.onBlockStartBreak(stack, pos, player))
                        ToolUtils.breakBlock(stack, player.world, player, posToBreak, pos)
                }
            }
        }
    }

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
        if (isEffective(stack, state)) material.efficiency else 1F

    private fun isEffective(stack: ItemStack, state: IBlockState): Boolean =
        getToolClasses(stack).any { state.block.isToolEffective(it, state) } ||
            material.type.effectiveMaterials.contains(state.material)

    override fun getHarvestLevel(stack: ItemStack, toolClass: String, player: EntityPlayer?, blockState: IBlockState?): Int =
        material.harvestLevel

    override fun getToolClasses(stack: ItemStack): Set<String> = material.type.toolTypes
}