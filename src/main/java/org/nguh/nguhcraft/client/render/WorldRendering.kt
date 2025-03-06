package org.nguh.nguhcraft.client.render

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.render.VertexConsumer
import net.minecraft.util.Colors
import net.minecraft.util.Util
import net.minecraft.util.math.Direction
import net.minecraft.util.profiler.Profilers
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.protect.Region
import org.nguh.nguhcraft.server.Barrier
import org.nguh.nguhcraft.unaryMinus
import kotlin.math.min

@Environment(EnvType.CLIENT)
object WorldRendering {
    var Barriers = listOf<Barrier>()
    var RenderRegions = false

    fun RenderWorld(Ctx: WorldRenderContext) {
        Profilers.get().push("nguhcraft:world_rendering")

        // Transform all points relative to the camera position.
        Renderer.PushModelViewMatrix(-Ctx.camera().pos) {
            // Render barriers.
            Layers.BARRIERS.Use { RenderBarriers(Ctx, it) }

            // Render regions.
            Layers.REGION_LINES.Use { RenderRegions(Ctx, it) }
        }

        Profilers.get().pop()
    }

    private fun RenderBarriers(Ctx: WorldRenderContext, VA: VertexAllocator) {
        val CW = Ctx.world()
        val WR = Ctx.worldRenderer()
        val MinY = CW.bottomY
        val MaxY = CW.topYInclusive + 1
        val CameraPos = Ctx.camera().pos
        val DT = -(Util.getMeasuringTimeMs() % 3000L).toFloat() / 3000.0f

        // Render each barrier.
        for (B in Barriers) {
            if (B.W != CW.registryKey) continue
            if (B.XZ.DistanceFrom(CameraPos) > WR.viewDistance * 16) continue
            VA.Draw { RenderBarrier(it, B, MinY = MinY, MaxY = MaxY, DT) }
        }
    }

    private fun RenderBarrier(VC: VertexConsumer, B: Barrier, MinY: Int, MaxY: Int, DT: Float) {
        // Set shader colour. This is why we need to render each barrier separately.
        Renderer.SetShaderColour(B.Colour)

        // Helper to add a quad from (x, y, z).
        val MinX = B.XZ.MinX
        val MaxX = B.XZ.OutsideMaxX
        val MinZ = B.XZ.MinZ
        val MaxZ = B.XZ.OutsideMaxZ

        // Helper to draw a 2×2 quad in the XY or ZY plane.
        fun Quad(Dir: Direction, X: Int, Y: Int, Z: Int, InvertScroll: Boolean) {
            // Make sure we don’t draw outside the bounds of the barrier.
            val EndX = min(MaxX, X + Dir.offsetX * 2)
            val EndY = Y + 2
            val EndZ = min(MaxZ, Z + Dir.offsetZ * 2)

            // Clamp the U coordinate so we don’t stretch the texture if the last
            // square happens to be 1×2 instead of 2×2.
            var U = DT
            var EndU = U + (if (Dir == Direction.EAST) EndX - X else EndZ - Z).toFloat() / 2
            if (InvertScroll) {
                U = EndU
                EndU = DT
            }

            // Draw the quad.
            VC.vertex(X.toFloat(), Y.toFloat(), Z.toFloat()).texture(U, DT)
            VC.vertex(EndX.toFloat(), Y.toFloat(), EndZ.toFloat()).texture(EndU, DT)
            VC.vertex(EndX.toFloat(), EndY.toFloat(), EndZ.toFloat()).texture(EndU, DT + 1F)
            VC.vertex(X.toFloat(), EndY.toFloat(), Z.toFloat()).texture(U, DT + 1F)
        }

        // XY min.
        for (X in MinX..<MaxX step 2)
            for (Y in MinY..<MaxY step 2)
                Quad(Direction.EAST, X, Y, MinZ, false)

        // XY max.
        for (X in MinX..<MaxX step 2)
            for (Y in MinY..<MaxY step 2)
                Quad(Direction.EAST, X, Y, MaxZ, true)

        // ZY min.
        for (Z in MinZ..<MaxZ step 2)
            for (Y in MinY..<MaxY step 2)
                Quad(Direction.SOUTH, MinX, Y, Z, true)

        // ZY max.
        for (Z in MinZ..<MaxZ step 2)
            for (Y in MinY..<MaxY step 2)
                Quad(Direction.SOUTH, MaxX, Y, Z, false)
    }

    private fun RenderRegions(Ctx: WorldRenderContext, VA: VertexAllocator) {
        if (!RenderRegions) return
        val CW = Ctx.world()
        val WR = Ctx.worldRenderer()
        val MinY = CW.bottomY
        val MaxY = CW.topYInclusive + 1
        val CameraPos = Ctx.camera().pos

        // Render each region.
        Renderer.SetShaderColour(Colors.WHITE)
        for (R in ProtectionManager.GetRegions(CW)) {
            if (R.DistanceFrom(CameraPos) > WR.viewDistance * 16) continue
            VA.Draw { RenderRegion(it, R, Colour = Colors.LIGHT_YELLOW, MinY = MinY, MaxY = MaxY) }
        }
    }

    private fun RenderRegion(VC: VertexConsumer, R: Region, Colour: Int, MinY: Int, MaxY: Int) {
        val MinX = R.MinX
        val MaxX = R.OutsideMaxX
        val MinZ = R.MinZ
        val MaxZ = R.OutsideMaxZ

        // Helper to add a vertex.
        fun Vertex(X: Int, Y: Int, Z: Int) = VC.vertex(
            X.toFloat(),
            Y.toFloat(),
            Z.toFloat()
        ).color(Colour)

        // Vertical lines along X axis.
        for (X in MinX.toInt()..MaxX.toInt()) {
            Vertex(X, MinY, MinZ).color(Colour)
            Vertex(X, MaxY, MinZ).color(Colour)
            Vertex(X, MinY, MaxZ).color(Colour)
            Vertex(X, MaxY, MaxZ).color(Colour)
        }

        // Vertical lines along Z axis.
        for (Z in MinZ.toInt()..MaxZ.toInt()) {
            Vertex(MinX, MinY, Z).color(Colour)
            Vertex(MinX, MaxY, Z).color(Colour)
            Vertex(MaxX, MinY, Z).color(Colour)
            Vertex(MaxX, MaxY, Z).color(Colour)
        }

        // Horizontal lines.
        for (Y in MinY.toInt()..MaxY.toInt()) {
            Vertex(MinX, Y, MinZ).color(Colour)
            Vertex(MaxX, Y, MinZ).color(Colour)
            Vertex(MinX, Y, MaxZ).color(Colour)
            Vertex(MaxX, Y, MaxZ).color(Colour)
            Vertex(MinX, Y, MinZ).color(Colour)
            Vertex(MinX, Y, MaxZ).color(Colour)
            Vertex(MaxX, Y, MinZ).color(Colour)
            Vertex(MaxX, Y, MaxZ).color(Colour)
        }
    }
}