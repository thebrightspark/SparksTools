package brightspark.sparkstools.item

import brightspark.sparkstools.SparksTools
import brightspark.sparkstools.ToolUtils
import com.google.common.collect.Multimap
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.ai.attributes.AttributeModifier
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.minecraftforge.oredict.OreDictionary

abstract class SHToolItem(val tool: CustomTool) : Item() {
    init {
	    registryName = tool.registryName
        translationKey = tool.registryName.path
        creativeTab = SparksTools.tab
	    maxStackSize = 1
	    maxDamage = tool.durability
    }

	internal abstract fun getBlocksToBreakIfEffective(stack: ItemStack, pos: BlockPos, side: EnumFacing, player: EntityPlayer): Iterable<BlockPos>

	fun getBlocksToBreak(stack: ItemStack, pos: BlockPos, side: EnumFacing, player: EntityPlayer): Iterable<BlockPos> =
		if (isEffective(stack, player.world.getBlockState(pos)))
			getBlocksToBreakIfEffective(stack, pos, side, player)
		else
			emptySet()

    open fun breakBlocks(stack: ItemStack, pos: BlockPos, sideHit: EnumFacing, player: EntityPlayer, breakInputPos: Boolean = false) {
	    getBlocksToBreak(stack, pos, sideHit, player)
		    .filter { (breakInputPos || it != pos) && player.canPlayerEdit(pos, sideHit, stack) && !super.onBlockStartBreak(stack, it, player) }
		    .forEach { ToolUtils.breakBlock(stack, player.world, player, it, pos) }
    }

	open fun isEffective(stack: ItemStack, state: IBlockState): Boolean =
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

	override fun hitEntity(stack: ItemStack, target: EntityLivingBase, attacker: EntityLivingBase): Boolean {
		stack.damageItem(2, attacker)
		return true
	}

	override fun onBlockDestroyed(stack: ItemStack, worldIn: World, state: IBlockState, pos: BlockPos, entityLiving: EntityLivingBase): Boolean {
		if (!worldIn.isRemote && state.getBlockHardness(worldIn, pos) > 0F)
			stack.damageItem(1, entityLiving)
		return true
	}

	override fun getItemStackDisplayName(stack: ItemStack): String = tool.name

	@SideOnly(Side.CLIENT)
	override fun isFull3D(): Boolean = true

	override fun getItemEnchantability(stack: ItemStack): Int = tool.enchantability

	override fun getIsRepairable(toRepair: ItemStack, repair: ItemStack): Boolean =
		tool.material.any { OreDictionary.itemMatches(repair, it, false) }

	override fun getAttributeModifiers(slot: EntityEquipmentSlot, stack: ItemStack): Multimap<String, AttributeModifier> {
		val attributes = super.getAttributeModifiers(slot, stack)
		if (slot == EntityEquipmentSlot.MAINHAND) {
			attributes.put(SharedMonsterAttributes.ATTACK_DAMAGE.name, AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", tool.attackDamage.toDouble(), 0))
			attributes.put(SharedMonsterAttributes.ATTACK_SPEED.name, AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", tool.attackSpeed.toDouble(), 0))
		}
		return attributes
	}

	override fun getDestroySpeed(stack: ItemStack, state: IBlockState): Float =
        if (isEffective(stack, state)) tool.efficiency else 1F

    override fun getHarvestLevel(stack: ItemStack, toolClass: String, player: EntityPlayer?, blockState: IBlockState?): Int =
	    tool.harvestLevel

    override fun getToolClasses(stack: ItemStack): Set<String> = tool.type.toolTypes
}