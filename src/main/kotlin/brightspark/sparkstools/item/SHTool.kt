package brightspark.sparkstools.item

import brightspark.sparkstools.SparksTools
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumFacing.*
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class SHTool(name: String, private val material: SHToolMaterial) : Item() {
    init {
        setRegistryName(name)
        translationKey = name
        creativeTab = SparksTools.tab
    }

    /**
     * Gets the start and end [BlockPos] for the area to break
     */
    private fun getBreakArea(stack: ItemStack, pos: BlockPos, sideHit: EnumFacing, player: EntityPlayer): Pair<BlockPos, BlockPos> {
        //val item = stack.item as SHTool
        val mineSize = 1 // TEMP
        val start = BlockPos.MutableBlockPos(pos)
        val end = BlockPos.MutableBlockPos(pos)

        // Offset area upwards if player is standing on ground and mining horizontally
        if (!player.capabilities.isFlying && sideHit.axis.isHorizontal && mineSize > 1) {
            start.move(UP, mineSize - 1)
            end.move(UP, mineSize - 1)
        }

        // Move start and end positions to area corners
        when (sideHit) {
            NORTH, SOUTH -> {
                start.move(DOWN, mineSize).move(WEST, mineSize)
                end.move(UP, mineSize).move(EAST, mineSize)
            }
            WEST, EAST -> {
                start.move(DOWN, mineSize).move(NORTH, mineSize)
                end.move(UP, mineSize).move(SOUTH, mineSize)
            }
            UP, DOWN -> {
                start.move(WEST, mineSize).move(NORTH, mineSize)
                end.move(EAST, mineSize).move(SOUTH, mineSize)
            }
        }

        return start.toImmutable() to end.toImmutable()
    }

    private fun breakBlock(stack: ItemStack, world: World, player: EntityPlayer, pos: BlockPos, refPos: BlockPos) {
        // TODO: Block breaking
    }

    override fun onBlockStartBreak(stack: ItemStack, pos: BlockPos, player: EntityPlayer): Boolean {
        val ray = rayTrace(player.world, player, false) ?: return super.onBlockStartBreak(stack, pos, player)
        val positions = getBreakArea(stack, pos, ray.sideHit, player)
        val start = positions.first
        val end = positions.second
        for (x in start.x..end.x) {
            for (y in start.y..end.y) {
                for (z in start.z..end.z) {
                    val posToBreak = BlockPos(x, y, z)
                    // Don't need to break the block that the player already mined
                    if (posToBreak == pos)
                        continue
                    if (!super.onBlockStartBreak(stack, pos, player))
                        breakBlock(stack, player.world, player, posToBreak, pos)
                }
            }
        }
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