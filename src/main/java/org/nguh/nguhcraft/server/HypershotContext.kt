package org.nguh.nguhcraft.server

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.TridentItem
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.world.World
import org.nguh.nguhcraft.Utils.EnchantLvl
import org.nguh.nguhcraft.mixin.server.RangedWeaponItemAccessor
import org.nguh.nguhcraft.packets.ClientboundSyncHypershotStatePacket
import org.nguh.nguhcraft.server.accessors.LivingEntityAccessor


@Environment(EnvType.SERVER)
data class HypershotContext(
    /** The hand the weapon is held in. */
    val Hand: Hand,

    /** The weapon used to fire the projectile. */
    val Weapon: ItemStack,

    /** List of projectiles to fire. */
    val Projectiles: List<ItemStack>,

    /** Initial speed modifier. */
    val Speed: Float,

    /** Initial divergence modifier. */
    val Divergence: Float,

    /** Whether this projectile is fully charged. */
    val Critical: Boolean,

    /** Remaining ticks. */
    var Ticks: Int
) {
    /**
    * Tick this context.
    *
    * @return `true` if we should remove the context, `false` otherwise.
    */
    fun Tick(W: World, Shooter: LivingEntity): Boolean {
        if (TickImpl(W, Shooter) == EXPIRED) {
            (Shooter as LivingEntityAccessor).hypershotContext = null
            if (Shooter is ServerPlayerEntity) ServerPlayNetworking.send(
                Shooter,
                ClientboundSyncHypershotStatePacket(false)
            )

            return EXPIRED
        }

        return !EXPIRED
    }

    private fun TickImpl(W: World, Shooter: LivingEntity): Boolean {
        // Cancel if we should stop or are dead.
        if (Ticks-- < 1 || Shooter.isDead || Shooter.isRemoved) return EXPIRED

        // Make sure we’re still holding the item.
        if (Shooter.getStackInHand(Hand) != Weapon) return EXPIRED

        // Ok, fire the projectile(s).
        //
        // Take care to duplicate the projectile item stacks unless we’re on the
        // last tick, in which case we can just use the original list.
        val I = Weapon.item
        if (I is RangedWeaponItemAccessor) {
            I.InvokeShootAll(
                W,
                Shooter,
                Hand,
                Weapon,
                if (Ticks < 1) Projectiles else Projectiles.toList().map { it.copy() },
                Speed,
                Divergence,
                Critical,
                null
            )

            // Also play a sound effect.
            if (Shooter is PlayerEntity) W.playSound(
                null,
                Shooter.getX(),
                Shooter.getY(),
                Shooter.getZ(),
                SoundEvents.ENTITY_ARROW_SHOOT,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f / (W.getRandom().nextFloat() * 0.4f + 1.2f)
            )
        }

        // We also support tridents here.
        else if (I is TridentItem && Shooter is PlayerEntity) {
            ServerUtils.ActOnTridentThrown(
                W,
                Shooter,
                Weapon,
                1
            )
        }

        // Keep ticking this.
        return !EXPIRED
    }

    companion object {
        const val EXPIRED: Boolean = true
    }
}
