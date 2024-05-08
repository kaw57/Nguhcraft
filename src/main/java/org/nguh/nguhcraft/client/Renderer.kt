package org.nguh.nguhcraft.client

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.render.*
import net.minecraft.util.math.Vec3d
import java.util.*


@Environment(EnvType.CLIENT)
object Renderer {
    private val Lines = Vector<Pair<Vec3d, Vec3d>>()

    @JvmStatic
    fun DebugRender(WRC: WorldRenderContext) {
        // Draw the lines.
        GlStateManager._disableCull()
        GlStateManager._depthMask(false)
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram)
        RenderSystem.lineWidth(4F)
        val T: Tessellator = RenderSystem.renderThreadTesselator()
        val B: BufferBuilder = T.buffer
        val Proj = WRC.camera().pos
        val MS = WRC.matrixStack()
        MS?.push()
        MS?.translate(-Proj.x, -Proj.y, -Proj.z)
        val MTX = MS?.peek()
        for (L in Lines) {
            B.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR)
            B.vertex(MTX, L.first.x.toFloat(), L.first.y.toFloat(), L.first.z.toFloat()).color(255, 0, 0, 255).next()
            B.vertex(MTX, L.second.x.toFloat(), L.second.y.toFloat(), L.second.z.toFloat()).color(255, 0, 0, 255).next()
            T.draw()
        }
        MS?.pop()
        RenderSystem.lineWidth(1F)
        GlStateManager._enableCull()
        GlStateManager._depthMask(true)

/*
        // Search for an entity to target. Extend the arrow’s bounding box to
        // the block that we’ve hit, or to the max distance if we missed and
        // check for entity collisions.
        val BB = Box.from(VCam).stretch(VEnd.subtract(VCam)).expand(1.0)*/
    }

    fun RenderLine(A: Vec3d, B: Vec3d) {
        Lines.add(Pair(A, B))
    }
}