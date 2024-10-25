package org.nguh.nguhcraft.client

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.server.integrated.IntegratedServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.RaycastContext
import org.lwjgl.glfw.GLFW
import org.nguh.nguhcraft.Constants.MAX_HOMING_DISTANCE
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.Utils.Debug
import org.nguh.nguhcraft.block.NguhBlocks
import org.nguh.nguhcraft.client.ClientUtils.Client


@Environment(EnvType.CLIENT)
class NguhcraftClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientNetworkHandler.Init()

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register { RegionRenderer.Render(it) }

        Registry.register(Registries.ITEM_GROUP, Id("treasures"), TREASURES_ITEM_GROUP)

        BlockRenderLayerMap.INSTANCE.putBlock(NguhBlocks.LOCKED_DOOR, RenderLayer.getCutout())

        ClientCommandRegistrationCallback.EVENT.register { Dispatcher, _ ->
            Dispatcher.register(RenderCommand())
        }
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
            return false
        }

        fun RenderCommand(): LiteralArgumentBuilder<FabricClientCommandSource> = literal<FabricClientCommandSource>("render")
            .then(literal<FabricClientCommandSource>("regions")
                .executes {
                    RegionRenderer.ShouldRender = !RegionRenderer.ShouldRender
                    it.source.sendFeedback(Text.literal(
                        "Region rendering is now ${if (RegionRenderer.ShouldRender) "enabled" else "disabled"}."
                    ).formatted(Formatting.YELLOW))
                    0
                }
            )
    }
}
