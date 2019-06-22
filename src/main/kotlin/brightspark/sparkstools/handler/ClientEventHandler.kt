package brightspark.sparkstools.handler

import brightspark.sparkstools.SparksTools
import brightspark.sparkstools.item.SHToolItem
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.relauncher.Side

@Mod.EventBusSubscriber(modid = SparksTools.MOD_ID, value = [Side.CLIENT])
object ClientEventHandler {
	private var lastStack = ItemStack.EMPTY
	private var lastPos: BlockPos? = null
	private var positionsToRender: Iterable<BlockPos>? = null

	@SubscribeEvent
	@JvmStatic
	fun updateSelection(event: TickEvent.ClientTickEvent) {
		if (event.phase != TickEvent.Phase.START) return
		val mc = Minecraft.getMinecraft()
		val player = mc.player ?: return
		val heldStack = player.heldItemMainhand
		var posLookingAt: BlockPos? = null
		var side: EnumFacing? = null
		mc.objectMouseOver?.let { ray ->
			if (ray.typeOfHit == RayTraceResult.Type.BLOCK) {
				posLookingAt = ray.blockPos
				side = ray.sideHit
			}
		}

		if (heldStack.item != lastStack.item || posLookingAt != lastPos) {
			lastStack = heldStack
			lastPos = posLookingAt
			positionsToRender = if (heldStack.item is SHToolItem && posLookingAt != null && side != null)
				(heldStack.item as SHToolItem).getBlocksToBreak(heldStack, posLookingAt!!, side!!, player)
			else
				null
		}
	}

	@SubscribeEvent
	@JvmStatic
	fun renderSelection(event: RenderWorldLastEvent) {
		positionsToRender?.forEach {
			@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
			event.context.drawSelectionBox(Minecraft.getMinecraft().player,
				RayTraceResult(RayTraceResult.Type.BLOCK, Vec3d.ZERO, null, it),
				0, event.partialTicks)
		}
	}
}