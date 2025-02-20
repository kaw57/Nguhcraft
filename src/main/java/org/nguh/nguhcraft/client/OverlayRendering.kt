package org.nguh.nguhcraft.client

import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.*
import net.minecraft.util.Colors
import net.minecraft.util.TriState
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.Direction
import net.minecraft.util.profiler.Profilers
import org.nguh.nguhcraft.client.ClientUtils.Client
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.protect.Region
import org.nguh.nguhcraft.server.Barrier
import kotlin.math.min

@Environment(EnvType.CLIENT)
object OverlayRendering {
    private const val PADDING = 2
    private val BARRIER_TEX = RenderPhase.Texture(WorldBorderRendering.FORCEFIELD, TriState.FALSE, false)
    var Barriers = listOf<Barrier>()
    var RenderRegions = false

    private fun ColourFor(R: Region) =
        if (false) Colors.LIGHT_RED
        else Colors.LIGHT_YELLOW

    fun RenderWorld(Ctx: WorldRenderContext) {
        Profilers.get().push("nguhcraft:overlay")
        RenderBarriers(Ctx)
        RenderRegions(Ctx)
        Profilers.get().pop()
    }

    private fun RenderBarriers(Ctx: WorldRenderContext) {
        val CW = Ctx.world()
        val WR = Ctx.worldRenderer()
        val MinY = CW.bottomY
        val MaxY = CW.topYInclusive + 1

        // Transform all points relative to the camera position.
        val Pos = Ctx.camera().pos
        val MS = RenderSystem.getModelViewStack()
        MS.pushMatrix()
        MS.translate(-Pos.x.toFloat(), -Pos.y.toFloat(), -Pos.z.toFloat())

        // Set up rendering params.
        val DT = -(Util.getMeasuringTimeMs() % 3000L).toFloat() / 3000.0f
        RenderSystem.disableCull()
        RenderSystem.enableBlend()
        RenderSystem.enableDepthTest()
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX)
        BARRIER_TEX.startDrawing()

        // Render each barrier.
        for (B in Barriers) {
            if (B.W != CW.registryKey) continue
            if (B.XZ.DistanceFrom(Pos) > WR.viewDistance * 16) continue

            // Set shader colour. This is why we need to render each barrier separately.
            val VC = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE)
            SetShaderColour(B.Colour)

            // Helper to add a quad from (x, y, z).
            val MinX = B.XZ.MinX
            val MaxX = B.XZ.RenderMaxX
            val MinZ = B.XZ.MinZ
            val MaxZ = B.XZ.RenderMaxZ

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

            BufferRenderer.drawWithGlobalProgram(VC.end())
        }

        BARRIER_TEX.endDrawing()
        SetShaderColour(Colors.WHITE)
        MS.popMatrix()
    }

    private fun RenderRegions(Ctx: WorldRenderContext) {
        if (!RenderRegions) return
        val CW = Ctx.world()
        val WR = Ctx.worldRenderer()
        val MinY = CW.bottomY
        val MaxY = CW.topYInclusive + 1

        // Transform all points relative to the camera position.
        val Pos = Ctx.camera().pos
        val MS = Ctx.matrixStack()!!
        MS.push()
        MS.translate(-Pos.x, -Pos.y, -Pos.z)
        val MTX = MS.peek().positionMatrix

        // Set up rendering params.
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)
        RenderSystem.lineWidth(1F)
        RenderSystem.disableCull()
        RenderSystem.enableDepthTest()
        SetShaderColour(Colors.WHITE)

        // Render each region.
        for (R in ProtectionManager.GetRegions(CW)) {
            if (R.DistanceFrom(Pos) > WR.viewDistance * 16) continue
            val MinX = R.MinX
            val MaxX = R.RenderMaxX
            val MinZ = R.MinZ
            val MaxZ = R.RenderMaxZ

            // Draw debug lines so we get a line width of 1 pixel.
            val VC = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)

            // Indicate whether this is a restricted region.
            val Colour = ColourFor(R)

            // Helper to add a vertex.
            fun Vertex(X: Int, Y: Int, Z: Int) = VC.vertex(
                MTX,
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

            BufferRenderer.drawWithGlobalProgram(VC.end())
        }

        MS.pop()
    }

    fun RenderHUD(Ctx: DrawContext) {
        if (!RenderRegions) return

        // Check if we’re in a region.
        val C = Client()
        val PlayerPos = BlockPos.ofFloored(C.player?.pos ?: return)
        val PlayerRegion = ProtectionManager.FindRegionContainingBlock(C.world!!, PlayerPos)
        if (PlayerRegion == null) return

        // If so, draw it in the bottom-right corner.
        val TextToRender = "Region: ${PlayerRegion.Name}"
        val Width = C.textRenderer.getWidth(TextToRender)
        val Height = C.textRenderer.fontHeight
        Ctx.drawText(
            C.textRenderer,
            TextToRender,
            Ctx.scaledWindowWidth - PADDING - Width,
            Ctx.scaledWindowHeight - PADDING - Height,
            ColourFor(PlayerRegion),
            true
        )
    }

    private fun SetShaderColour(colour: Int) {
        RenderSystem.setShaderColor(
            ColorHelper.getRed(colour) / 255.0f,
            ColorHelper.getGreen(colour) / 255.0f,
            ColorHelper.getBlue(colour) / 255.0f,
            ColorHelper.getAlpha(colour) / 255.0f
        )
    }
}