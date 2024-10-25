package org.nguh.nguhcraft.client

import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Colors
import net.minecraft.util.math.Vec3d
import org.nguh.nguhcraft.protect.ProtectionManager
import kotlin.math.min


object RegionRenderer {
    var ShouldRender = false
    fun Render(Ctx: WorldRenderContext) {
        if (!ShouldRender) return
        val CW = Ctx.world()
        val WR = Ctx.worldRenderer()
        val MinY = CW.bottomY
        val MaxY = CW.topYInclusive

        // Transform all points relative to the camera position.
        val Pos = Ctx.camera().pos
        val MS = Ctx.matrixStack()!!
        MS.push()
        MS.translate(-Pos.x, -Pos.y, -Pos.z)
        val MTX = MS.peek().positionMatrix

        // Set up rendering params.
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES)
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
            ).color(Colors.LIGHT_RED)

            // Vertical lines along X axis.
            for (X in MinX.toInt()..MaxX.toInt()) {
                Vertex(X, MinY, MinZ).color(Colors.LIGHT_RED)
                Vertex(X, MaxY, MinZ).color(Colors.LIGHT_RED)
                Vertex(X, MinY, MaxZ).color(Colors.LIGHT_RED)
                Vertex(X, MaxY, MaxZ).color(Colors.LIGHT_RED)
            }

            // Vertical lines along Z axis.
            for (Z in MinZ.toInt()..MaxZ.toInt()) {
                Vertex(MinX, MinY, Z).color(Colors.LIGHT_RED)
                Vertex(MinX, MaxY, Z).color(Colors.LIGHT_RED)
                Vertex(MaxX, MinY, Z).color(Colors.LIGHT_RED)
                Vertex(MaxX, MaxY, Z).color(Colors.LIGHT_RED)
            }

            // Horizontal lines.
            for (Y in MinY.toInt()..MaxY.toInt()) {
                Vertex(MinX, Y, MinZ).color(Colors.LIGHT_RED)
                Vertex(MaxX, Y, MinZ).color(Colors.LIGHT_RED)
                Vertex(MinX, Y, MaxZ).color(Colors.LIGHT_RED)
                Vertex(MaxX, Y, MaxZ).color(Colors.LIGHT_RED)
                Vertex(MinX, Y, MinZ).color(Colors.LIGHT_RED)
                Vertex(MinX, Y, MaxZ).color(Colors.LIGHT_RED)
                Vertex(MaxX, Y, MinZ).color(Colors.LIGHT_RED)
                Vertex(MaxX, Y, MaxZ).color(Colors.LIGHT_RED)
            }

            BufferRenderer.drawWithGlobalProgram(VC.end())
        }

        MS.pop()
    }
}