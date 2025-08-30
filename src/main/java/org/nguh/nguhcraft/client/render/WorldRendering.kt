package org.nguh.nguhcraft.client.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormat
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.*
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters
import net.minecraft.client.render.RenderLayer.of
import net.minecraft.util.Colors
import net.minecraft.util.Util
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.profiler.Profilers
import org.joml.Matrix4f
import org.joml.Matrix4fStack
import org.joml.Vector4f
import org.joml.Vector4fc
import org.nguh.nguhcraft.client.Push
import org.nguh.nguhcraft.entity.EntitySpawnManager
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.protect.Region
import org.nguh.nguhcraft.unaryMinus
import java.util.*
import kotlin.math.min

@Environment(EnvType.CLIENT)
interface RenderLayerMultiPhaseShaderColourAccessor {
    fun `Nguhcraft$SetShaderColour`(Colour: Vector4fc)
}

@Environment(EnvType.CLIENT)
object WorldRendering {
    private const val GOLD = 0xFFFFAA00.toInt()

    // =========================================================================
    //  Render Data
    // =========================================================================
    @JvmField @Volatile var Spawns = listOf<EntitySpawnManager.Spawn>()
    var RenderRegions = false
    var RenderSpawns = false

    // =========================================================================
    //  Pipelines and Layers
    // =========================================================================
    val POSITION_COLOR_LINES_PIPELINE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation("pipeline/debug_line_strip")
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
            .build()
    )

    val REGION_LINES: RenderLayer = of(
        "nguhcraft:region_lines",
        1536,
        POSITION_COLOR_LINES_PIPELINE,
        MultiPhaseParameters.builder()
            .lineWidth(RenderPhase.LineWidth(OptionalDouble.of(1.0)))
            .build(false)
    )

    val REGION_BARRIERS: RenderLayer = of(
        "nguhcraft:barriers",
        1536,
        RenderPipelines.RENDERTYPE_WORLD_BORDER,
        MultiPhaseParameters.builder()
            .texture(RenderPhase.Texture(WorldBorderRendering.FORCEFIELD, false))
            .lightmap(RenderPhase.Lightmap.ENABLE_LIGHTMAP)
            .target(RenderPhase.Target.WEATHER_TARGET)
            .build(false)
    )

    // =========================================================================
    //  Setup
    // =========================================================================
    fun ActOnSessionStart() {
        Spawns = listOf()
        RenderRegions = false
        RenderSpawns = false
    }

    fun Init() {
        // Does nothing but must be called early to run static constructors.
    }

    fun RenderWorld(Ctx: WorldRenderContext) {
        Profilers.get().push("nguhcraft:world_rendering")
        val MS = Ctx.matrixStack()!!
        MS.Push {
            // Transform all points relative to the camera position.
            translate(-Ctx.camera().pos)

            // Render barriers.
            val DT = -(Util.getMeasuringTimeMs() % 3000L).toFloat() / 3000.0f
            RenderBarriers(Ctx, DT)

            // Render regions.
            if (RenderRegions) RenderRegions(Ctx)

            // Render spawn positions.
            if (RenderSpawns) RenderSpawns(Ctx)
        }
        Profilers.get().pop()
    }

    // =========================================================================
    //  Region Barriers
    // =========================================================================
    private fun RenderBarriers(Ctx: WorldRenderContext, DT: Float) {
        val CW = Ctx.world()
        val WR = Ctx.worldRenderer()
        val MinY = CW.bottomY
        val MaxY = CW.topYInclusive + 1
        val CameraPos = Ctx.camera().pos
        val MTX = Ctx.matrixStack()!!.peek().positionMatrix

        // Render barriers for each region.
        for (R in ProtectionManager.GetRegions(CW)) {
            if (!R.ShouldRenderEntryExitBarrier()) continue
            if (R.DistanceFrom(CameraPos) > WR.viewDistance * 16) continue
            val VC = Tessellator.getInstance().begin(REGION_BARRIERS.drawMode, REGION_BARRIERS.vertexFormat)
            RenderBarrier(VC, MTX, R, MinY = MinY, MaxY = MaxY, DT)
            REGION_BARRIERS.draw(VC.endNullable() ?: continue)
        }
    }

    private fun RenderBarrier(VC: VertexConsumer, MTX: Matrix4f, R: Region, MinY: Int, MaxY: Int, DT: Float) {
        val Colour = when {
            R.ColourOverride != null -> R.ColourOverride!!
            !R.AllowsPlayerEntry() && !R.AllowsPlayerExit() -> GOLD
            !R.AllowsPlayerExit() -> Colors.LIGHT_RED
            !R.AllowsPlayerEntry() -> Colors.CYAN
            else -> return
        }

        // Set shader colour. This is why we need to render each barrier separately.
        (REGION_BARRIERS as RenderLayerMultiPhaseShaderColourAccessor).`Nguhcraft$SetShaderColour`(
            Vector4f(
                ColorHelper.getRedFloat(Colour),
                ColorHelper.getGreenFloat(Colour),
                ColorHelper.getBlueFloat(Colour),
                1.0f
            )
        )

        // Helper to add a quad from (x, y, z).
        val MinX = R.MinX
        val MaxX = R.OutsideMaxX
        val MinZ = R.MinZ
        val MaxZ = R.OutsideMaxZ

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
            VC.vertex(MTX, X.toFloat(), Y.toFloat(), Z.toFloat()).texture(U, DT)
            VC.vertex(MTX, EndX.toFloat(), Y.toFloat(), EndZ.toFloat()).texture(EndU, DT)
            VC.vertex(MTX, EndX.toFloat(), EndY.toFloat(), EndZ.toFloat()).texture(EndU, DT + 1F)
            VC.vertex(MTX, X.toFloat(), EndY.toFloat(), Z.toFloat()).texture(U, DT + 1F)
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

    // =========================================================================
    //  Region Outlines
    // =========================================================================
    private fun RenderRegions(Ctx: WorldRenderContext) {
        val VC = Ctx.consumers()!!.getBuffer(REGION_LINES)
        val CW = Ctx.world()
        val WR = Ctx.worldRenderer()
        val MTX = Ctx.matrixStack()!!.peek().positionMatrix
        val MinY = CW.bottomY
        val MaxY = CW.topYInclusive + 1
        val CameraPos = Ctx.camera().pos
        for (R in ProtectionManager.GetRegions(CW)) {
            if (R.DistanceFrom(CameraPos) > WR.viewDistance * 16) continue
            RenderRegion(VC, MTX, R, Colour = R.ColourOverride ?: Colors.LIGHT_YELLOW, MinY = MinY, MaxY = MaxY)
        }
    }

    private fun RenderRegion(VC: VertexConsumer, MTX: Matrix4f, R: Region, Colour: Int, MinY: Int, MaxY: Int) {
        val MinX = R.MinX
        val MaxX = R.OutsideMaxX
        val MinZ = R.MinZ
        val MaxZ = R.OutsideMaxZ

        // Helper to add a vertex.
        fun Vertex(X: Int, Y: Int, Z: Int) = VC.vertex(
            MTX,
            X.toFloat(),
            Y.toFloat(),
            Z.toFloat()
        ).color(Colour)

        // Vertical lines along X axis.
        for (X in MinX..MaxX) {
            Vertex(X, MinY, MinZ)
            Vertex(X, MaxY, MinZ)
            Vertex(X, MinY, MaxZ)
            Vertex(X, MaxY, MaxZ)
        }

        // Vertical lines along Z axis.
        for (Z in MinZ..MaxZ) {
            Vertex(MinX, MinY, Z)
            Vertex(MinX, MaxY, Z)
            Vertex(MaxX, MinY, Z)
            Vertex(MaxX, MaxY, Z)
        }

        // Horizontal lines.
        for (Y in MinY..MaxY) {
            Vertex(MinX, Y, MinZ)
            Vertex(MaxX, Y, MinZ)
            Vertex(MinX, Y, MaxZ)
            Vertex(MaxX, Y, MaxZ)
            Vertex(MinX, Y, MinZ)
            Vertex(MinX, Y, MaxZ)
            Vertex(MaxX, Y, MinZ)
            Vertex(MaxX, Y, MaxZ)
        }
    }

    private fun RenderSpawns(Ctx: WorldRenderContext) {
        val VC = Ctx.consumers()!!.getBuffer(RenderLayer.getLines())
        for (S in Spawns) VertexRendering.drawBox(
            Ctx.matrixStack(),
            VC,
            S.SpawnPos.x - .15,
            S.SpawnPos.y + .15,
            S.SpawnPos.z - .15,
            S.SpawnPos.x + .15,
            S.SpawnPos.y + .45,
            S.SpawnPos.z + .15,
            .4f,
            .4f,
            .8f,
            1f,
        )
    }
}