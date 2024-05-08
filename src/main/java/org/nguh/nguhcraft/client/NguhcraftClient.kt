package org.nguh.nguhcraft.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import org.nguh.nguhcraft.packets.ClientboundChatPacket
import org.nguh.nguhcraft.packets.ClientboundLinkUpdatePacket
import org.nguh.nguhcraft.packets.ClientboundSyncGameRulesPacket
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.ColorHelper
import net.minecraft.world.RaycastContext
import org.lwjgl.glfw.GLFW
import org.nguh.nguhcraft.Constants.MAX_HOMING_DISTANCE
import org.nguh.nguhcraft.Utils.Debug
import org.nguh.nguhcraft.client.ClientUtils.Client

@Environment(EnvType.CLIENT)
class NguhcraftClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundLinkUpdatePacket.ID) { Payload, _ ->
            NetworkHandler.HandleLinkUpdatePacket(Payload)
        }

        ClientPlayNetworking.registerGlobalReceiver(ClientboundChatPacket.ID) { Payload, _ ->
            NetworkHandler.HandleChatPacket(Payload)
        }

        ClientPlayNetworking.registerGlobalReceiver(ClientboundSyncGameRulesPacket.ID) { Payload, _ ->
            NetworkHandler.HandleSyncGameRulesPacket(Payload)
        }

        WorldRenderEvents.LAST.register { Renderer.DebugRender(it) }
    }

    companion object {
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
