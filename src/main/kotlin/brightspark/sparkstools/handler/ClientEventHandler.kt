package brightspark.sparkstools.handler

import brightspark.sparkstools.item.SHToolItem
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.TickEvent

object ClientEventHandler {
	private var lastStack = ItemStack.EMPTY
	private var lastPos: BlockPos? = null
	private var lastSide: Direction? = null
	private var lastOnGround: Boolean? = null
	private var lastSneaking = false
	private var positionsToRender: List<BlockPos>? = null

	fun updateSelection(event: TickEvent.ClientTickEvent) {
		if (event.phase != TickEvent.Phase.START) return
		val mc = Minecraft.getInstance()
		val player = mc.player ?: return
		val heldStack = player.heldItemMainhand
		var posLookingAt: BlockPos? = null
		var side: Direction? = null
		mc.objectMouseOver?.let { ray ->
			if (ray is BlockRayTraceResult && ray.type == RayTraceResult.Type.BLOCK) {
				posLookingAt = ray.pos
				side = ray.face
			}
		}

		if (heldStack.item != lastStack.item || posLookingAt != lastPos || side != lastSide || player.onGround != lastOnGround || player.isSneaking != lastSneaking) {
			lastStack = heldStack
			lastPos = posLookingAt
			lastSide = side
			lastOnGround = player.onGround
			lastSneaking = player.isSneaking
			positionsToRender = if (heldStack.item is SHToolItem && posLookingAt != null && side != null)
				(heldStack.item as SHToolItem).getBlocksToSelect(heldStack, posLookingAt!!, side!!, player)
			else
				null
		}
	}

	fun renderSelection(event: RenderWorldLastEvent) {
		positionsToRender?.forEach {
			@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
			event.context.drawSelectionBox(Minecraft.getInstance().gameRenderer.activeRenderInfo, BlockRayTraceResult(Vec3d.ZERO, null, it, false), 0)
		}
	}
}