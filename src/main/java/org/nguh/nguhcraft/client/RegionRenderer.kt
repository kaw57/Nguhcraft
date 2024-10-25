package org.nguh.nguhcraft.client

import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Colors
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.nguh.nguhcraft.client.ClientUtils.Client
import org.nguh.nguhcraft.protect.ProtectionManager
import kotlin.math.min


object RegionRenderer {
    const val PADDING = 2
    const val COLOUR = Colors.LIGHT_YELLOW
    var ShouldRender = false

    fun Render(Ctx: WorldRenderContext) {
        if (!ShouldRender) return
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
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.lineWidth(1F)
        RenderSystem.disableCull()
        RenderSystem.enableDepthTest()

        // Render each region.
        for (R in ProtectionManager.GetRegions(CW)) {
            // Only render regions we can actually see.
            if (
                min(
                    Pos.subtract(Vec3d(R.MinX.toDouble(), .0, R.MinZ.toDouble())).horizontalLength(),
                    Pos.subtract(Vec3d(R.MaxX.toDouble(), .0, R.MaxZ.toDouble())).horizontalLength()
                ) > WR.viewDistance * 16
            ) continue

            // Coordinates in Minecraft are at the north-west (-X, -Z) corner of the block,
            // so we need to add 1 to the maximum values to include the last block within
            // the region.
            val MinX = R.MinX
            val MaxX = R.MaxX + 1
            val MinZ = R.MinZ
            val MaxZ = R.MaxZ + 1

            // Draw debug lines so we get a line width of 1 pixel.
            val VC = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)

            // Helper to add a vertex.
            fun Vertex(X: Int, Y: Int, Z: Int) = VC.vertex(
                MTX,
                X.toFloat(),
                Y.toFloat(),
                Z.toFloat()
            ).color(COLOUR)

            // Vertical lines along X axis.
            for (X in MinX.toInt()..MaxX.toInt()) {
                Vertex(X, MinY, MinZ).color(COLOUR)
                Vertex(X, MaxY, MinZ).color(COLOUR)
                Vertex(X, MinY, MaxZ).color(COLOUR)
                Vertex(X, MaxY, MaxZ).color(COLOUR)
            }

            // Vertical lines along Z axis.
            for (Z in MinZ.toInt()..MaxZ.toInt()) {
                Vertex(MinX, MinY, Z).color(COLOUR)
                Vertex(MinX, MaxY, Z).color(COLOUR)
                Vertex(MaxX, MinY, Z).color(COLOUR)
                Vertex(MaxX, MaxY, Z).color(COLOUR)
            }

            // Horizontal lines.
            for (Y in MinY.toInt()..MaxY.toInt()) {
                Vertex(MinX, Y, MinZ).color(COLOUR)
                Vertex(MaxX, Y, MinZ).color(COLOUR)
                Vertex(MinX, Y, MaxZ).color(COLOUR)
                Vertex(MaxX, Y, MaxZ).color(COLOUR)
                Vertex(MinX, Y, MinZ).color(COLOUR)
                Vertex(MinX, Y, MaxZ).color(COLOUR)
                Vertex(MaxX, Y, MinZ).color(COLOUR)
                Vertex(MaxX, Y, MaxZ).color(COLOUR)
            }

            BufferRenderer.drawWithGlobalProgram(VC.end())
        }

        MS.pop()
    }

    fun RenderHUD(Ctx: DrawContext) {
        if (!ShouldRender) return

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
            COLOUR,
            true
        )
    }
}