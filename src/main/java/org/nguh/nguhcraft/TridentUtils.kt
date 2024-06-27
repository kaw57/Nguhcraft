package org.nguh.nguhcraft

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EntityType
import net.minecraft.entity.projectile.TridentEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvents
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.nguh.nguhcraft.Utils.EnchantLvl

object TridentUtils {
    @JvmStatic
    fun ActOnBlockHit(TE: TridentEntity, BHR: BlockHitResult) {
        val W = TE.world
        val Lvl = EnchantLvl(W, TE.itemStack, Enchantments.CHANNELING)
        if (W is ServerWorld && Lvl >= 2) {
            StrikeLighting(W, TE, BHR.blockPos)
            TE.playSound(SoundEvents.ITEM_TRIDENT_THUNDER.value(), 5f, 1.0f)
        }
    }

    /** Handle Channeling II on entity hit. */
    @JvmStatic
    fun ActOnEntityHit(TE: TridentEntity, EHR: EntityHitResult) {
        var SE = SoundEvents.ITEM_TRIDENT_HIT
        var Volume = 1.0f

        // Check if it’s thundering or if we have Channeling II.
        val W = TE.world
        val Thunder = W.isThundering
        val Lvl = EnchantLvl(W, TE.itemStack, Enchantments.CHANNELING)
        if (W is ServerWorld && Lvl > 0 && (Thunder || Lvl >= 2)) {
            EHR.entity.timeUntilRegen = 0
            val Where = EHR.entity.blockPos
            if (Lvl >= 2 || W.isSkyVisible(Where)) {
                StrikeLighting(W, TE, Where)
                SE = SoundEvents.ITEM_TRIDENT_THUNDER.value()
                Volume = 5.0f
            }
        }

        TE.playSound(SE, Volume, 1.0f)
    }

    /** Unconditionally strike lightning. */
    @Environment(EnvType.SERVER)
    private fun StrikeLighting(W: ServerWorld, TE: TridentEntity, Where: BlockPos?) {
        val Lightning = EntityType.LIGHTNING_BOLT.create(W)
        if (Lightning != null) {
            val Owner = TE.owner
            Lightning.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(Where))
            Lightning.channeler = if (Owner is ServerPlayerEntity) Owner else null
            W.spawnEntity(Lightning)
        }
    }
}