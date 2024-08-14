package org.nguh.nguhcraft.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.server.integrated.IntegratedServer
import net.minecraft.text.Text
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.RaycastContext
import org.lwjgl.glfw.GLFW
import org.nguh.nguhcraft.Constants.MAX_HOMING_DISTANCE
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.Utils.Debug
import org.nguh.nguhcraft.block.NguhBlocks
import org.nguh.nguhcraft.client.ClientUtils.Client
import org.nguh.nguhcraft.protect.ProtectionManager


@Environment(EnvType.CLIENT)
class NguhcraftClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientNetworkHandler.Init()

        WorldRenderEvents.LAST.register { Renderer.DebugRender(it) }

        Registry.register(Registries.ITEM_GROUP, Id("treasures"), TREASURES_ITEM_GROUP)

        BlockRenderLayerMap.INSTANCE.putBlock(NguhBlocks.LOCKED_DOOR, RenderLayer.getCutout())
    }

    companion object {
        val TREASURES_ITEM_GROUP: ItemGroup = net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
            .icon { ItemStack(Items.NETHERITE_INGOT) }
            .displayName(Text.literal("Treasures"))
            .entries {  Ctx, Entries -> Treasures.AddAll(Ctx, Entries) }
            .build()

        @JvmField @Volatile var InHypershotContext = false
        @JvmField @Volatile var BypassesRegionProtection = false
        @JvmField @Volatile var LastInteractedLecternPos: BlockPos = BlockPos.ORIGIN

        @JvmStatic
        fun ActOnSessionStart(S: IntegratedServer) {
            InHypershotContext = false
            BypassesRegionProtection = false
            LastInteractedLecternPos = BlockPos.ORIGIN
        }

        @JvmStatic
        fun ActOnSessionShutdown(S: IntegratedServer) {
            // Nop.
        }

        @JvmStatic
        fun ProcessF3(key: Int): Boolean {
            if (key == GLFW.GLFW_KEY_F12) {
                val C = Client()
                val W = C.world ?: return true
                val Player = C.player ?: return true
                val VCam = Player.getCameraPosVec(1.0f)
                val VRot = Player.getRotationVec(1.0f)
                var VEnd = VCam.add(VRot.x * MAX_HOMING_DISTANCE, VRot.y * MAX_HOMING_DISTANCE, VRot.z * MAX_HOMING_DISTANCE)
                val Ray = W.raycast(RaycastContext(VCam, VEnd, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, Player))

                // If we hit something, don’t go further.
                if (Ray.type !== HitResult.Type.MISS) VEnd = Ray.pos
                Renderer.RenderLine(VCam, VEnd)
                Debug("Raycast: {} -> {}", VCam, VEnd)
                return true
            }
            return false
        }
    }
}
