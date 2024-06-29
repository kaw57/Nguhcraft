package org.nguh.nguhcraft

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.TridentEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvents
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.nguh.nguhcraft.Utils.EnchantLvl
import org.nguh.nguhcraft.accessors.ProjectileEntityAccessor
import org.nguh.nguhcraft.accessors.TridentEntityAccessor
import org.nguh.nguhcraft.server.ServerUtils.MaybeEnterHypershotContext
import org.nguh.nguhcraft.server.accessors.LightningEntityAccessor

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

    /** Handle multishot tridents. */
    @JvmStatic
    @Environment(EnvType.SERVER)
    fun ActOnTridentThrown(W: World, PE: PlayerEntity, S: ItemStack, Extra: Int = 0) {
        val Lvl = EnchantLvl(W, S, Enchantments.MULTISHOT)
        val K = W.getRandom().nextFloat() / 10f // ADDED WITHOUT TESTING; WAS ALWAYS 0 BEFORE.
        val Yaw = PE.yaw
        val Pitch = PE.pitch

        // Enter hypershot context, if applicable.
        val HS = MaybeEnterHypershotContext(PE, PE.activeHand, S, listOf(), 2.5F, 1F, false)

        // Launch tridents.
        W.profiler.push("multishotTridents")
        for (I in 0 until Lvl + Extra) {
            val TE = TridentEntity(W, PE, S)
            TE.setVelocity(PE, Pitch, Yaw, 0F, 2.5F + K * .5F, 1F + .1F * I)

            // Mark that this trident is a copy; this disables item pickup, makes it
            // despawn after 5 seconds, and tells that client that it doesn’t have
            // loyalty so the copies don’t try to return to the owner.
            (TE as TridentEntityAccessor).SetCopy()
            if (HS) (TE as ProjectileEntityAccessor).MakeHypershotProjectile()
            W.spawnEntity(TE)
        }
        W.profiler.pop()
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

            // Tell the entity it was created by the Channeling enchantment,
            // in which case we do NOT want it to set anything on fire, and
            // remember that the trident has summoned it, which causes it to
            // be rendered on fire.
            (Lightning as LightningEntityAccessor).`Nguhcraft$SetCreatedByChanneling`()
            (TE as TridentEntityAccessor).`Nguhcraft$SetStruckLightning`()
        }
    }
}