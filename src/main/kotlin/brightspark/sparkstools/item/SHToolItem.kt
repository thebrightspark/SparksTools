package brightspark.sparkstools.item

import brightspark.ksparklib.api.damageItem
import brightspark.ksparklib.api.toBlockPosList
import brightspark.sparkstools.SparksTools
import brightspark.sparkstools.ToolUtils
import com.google.common.collect.Multimap
import net.minecraft.block.BlockState
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.ai.attributes.AttributeModifier
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.EquipmentSlotType
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.math.*
import net.minecraft.util.math.RayTraceContext.FluidMode
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraftforge.common.ToolType
import java.util.stream.Stream

abstract class SHToolItem(val tool: CustomTool) : Item(Properties().apply {
	group(SparksTools.group)
	maxStackSize(1)
	maxDamage(tool.durability)
}) {
	init {
		registryName = tool.registryName
	}

	/**
	 * Returns a [Stream] of [BlockPos] which within scope of this tool to break
	 * Checks are made later for whether each position is editable by the [player] and the [stack]
	 */
	internal abstract fun getBlocksToBreakIfEffective(stack: ItemStack, pos: BlockPos, side: Direction, player: PlayerEntity): Stream<BlockPos>

	/**
	 * Returns a [Stream] of [BlockPos] which should have selection boxes rendered for
	 */
	open fun getBlocksToSelect(stack: ItemStack, pos: BlockPos, side: Direction, player: PlayerEntity): List<BlockPos> =
		getBlocksToBreak(stack, pos, side, player).toBlockPosList()

	/**
	 * If this tool is effective against the block at [pos] then calls [getBlocksToBreakIfEffective] else returns an
	 * empty [Stream]
	 */
	fun getBlocksToBreak(stack: ItemStack, pos: BlockPos, side: Direction, player: PlayerEntity): Stream<BlockPos> =
		if (isEffective(stack, player.world.getBlockState(pos)))
			getBlocksToBreakIfEffective(stack, pos, side, player)
		else
			Stream.empty()

	/**
	 * Attempts to break the extra blocks supplied by [getBlocksToBreak]
	 */
	open fun breakBlocks(stack: ItemStack, pos: BlockPos, sideHit: Direction, player: PlayerEntity, breakInputPos: Boolean = false) {
		getBlocksToBreak(stack, pos, sideHit, player)
			.filter { (breakInputPos || it != pos) && player.world.isBlockModifiable(player, it) }
			.forEach { ToolUtils.breakBlock(stack, player.world, player, BlockPos(it), pos) }
	}

	/**
	 * Returns if this tool is effective against the block
	 */
	fun isEffective(stack: ItemStack, state: BlockState): Boolean =
		getToolTypes(stack).any { state.block.isToolEffective(state, it) } ||
			tool.type.effectiveMaterials.contains(state.material)

	// Copied from Item.rayTrace to increase the reach a little to ensure getting extra blocks always works
	fun rayTraceBlocks(worldIn: World, player: PlayerEntity): BlockRayTraceResult {
		val f = player.rotationPitch
		val f1 = player.rotationYaw
		val vec3d = player.getEyePosition(1f)
		val f2 = MathHelper.cos(-f1 * (Math.PI.toFloat() / 180f) - Math.PI.toFloat())
		val f3 = MathHelper.sin(-f1 * (Math.PI.toFloat() / 180f) - Math.PI.toFloat())
		val f4 = -MathHelper.cos(-f * (Math.PI.toFloat() / 180f))
		val f5 = MathHelper.sin(-f * (Math.PI.toFloat() / 180f))
		val f6 = f3 * f4
		val f7 = f2 * f4
		val d0 = player.getAttribute(PlayerEntity.REACH_DISTANCE).value * 1.5
		val vec3d1 = vec3d.add(f6.toDouble() * d0, f5.toDouble() * d0, f7.toDouble() * d0)
		return worldIn.rayTraceBlocks(RayTraceContext(vec3d, vec3d1, RayTraceContext.BlockMode.OUTLINE, FluidMode.NONE, player))
	}

	override fun onBlockStartBreak(stack: ItemStack, pos: BlockPos, player: PlayerEntity): Boolean {
		val ray = rayTraceBlocks(player.world, player)
		if (ray.type == RayTraceResult.Type.BLOCK)
			breakBlocks(stack, pos, ray.face, player)
		return super.onBlockStartBreak(stack, pos, player)
	}

	override fun canHarvestBlock(stack: ItemStack, state: BlockState): Boolean {
		val requiredTool = state.block.getHarvestTool(state)
		return state.material.isToolNotRequired || requiredTool == null ||
			getHarvestLevel(stack, requiredTool, null, state) >= state.block.getHarvestLevel(state)
	}

	override fun hitEntity(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
		stack.damageItem(attacker, 2)
		return true
	}

	override fun onBlockDestroyed(stack: ItemStack, worldIn: World, state: BlockState, pos: BlockPos, entityLiving: LivingEntity): Boolean {
		if (!worldIn.isRemote && state.getBlockHardness(worldIn, pos) > 0F)
			stack.damageItem(entityLiving)
		return true
	}

	override fun getDisplayName(stack: ItemStack): ITextComponent = StringTextComponent(tool.name)

	override fun getItemEnchantability(stack: ItemStack): Int = tool.enchantability

	override fun getIsRepairable(toRepair: ItemStack, repair: ItemStack): Boolean =
		tool.headMaterial.matchingStacks.any { repair.isItemEqual(it) }

	override fun getAttributeModifiers(slot: EquipmentSlotType, stack: ItemStack): Multimap<String, AttributeModifier> {
		val attributes = super.getAttributeModifiers(slot, stack)
		if (slot == EquipmentSlotType.MAINHAND) {
			attributes.put(SharedMonsterAttributes.ATTACK_DAMAGE.name, AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", tool.attackDamage.toDouble(), AttributeModifier.Operation.ADDITION))
			attributes.put(SharedMonsterAttributes.ATTACK_SPEED.name, AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", tool.attackSpeed.toDouble(), AttributeModifier.Operation.ADDITION))
		}
		return attributes
	}

	override fun getDestroySpeed(stack: ItemStack, state: BlockState): Float =
		if (isEffective(stack, state)) tool.efficiency else 1F

	override fun getHarvestLevel(stack: ItemStack, type: ToolType, player: PlayerEntity?, blockState: BlockState?): Int =
		tool.harvestLevel

	override fun getToolTypes(stack: ItemStack): Set<ToolType> = tool.type.toolTypes
}
