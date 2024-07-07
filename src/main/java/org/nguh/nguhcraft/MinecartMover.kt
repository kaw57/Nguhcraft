package org.nguh.nguhcraft

import net.minecraft.block.*
import net.minecraft.block.enums.RailShape
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.vehicle.AbstractMinecartEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.tag.BlockTags
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.nguh.nguhcraft.mixin.server.AbstractMinecartEntityAccessor
import org.nguh.nguhcraft.mixin.server.DetectorRailBlockAccessor
import org.nguh.nguhcraft.server.CentreOn
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

@Suppress("DEPRECATION")
private val BlockState.BlocksMovement get() = this.blocksMovement()

/** Perform a single movement action for minecart on a tick.  */
class MinecartMover private constructor(
    private val C: AbstractMinecartEntity,
    Pos: Vec3d,
    StartDelta: Vec3d,
    AbsX: Double,
    AbsZ: Double,
    Block: BlockPos,
    State: BlockState,
    Track: RailShape
) {
    private val StartPos: Vec3d
    private val Block: BlockPos.Mutable
    private var CrossedPoweredRail = false
    private lateinit var Dir: Direction
    private var DX = 0.0
    private var DZ = 0.0
    private var State: BlockState
    private var Track: RailShape

    init {
        this.State = State
        this.Track = Track
        this.Block = Block.mutableCopy()
        this.StartPos = Pos

        // Drop all secondary movement.
        if (AbsX > AbsZ) {
            this.DX = StartDelta.x
            this.DZ = 0.0
        } else {
            this.DX = 0.0
            this.DZ = StartDelta.z
        }

        ComputeDirection()
    }

    /** Apply acceleration and slowdown.  */
    private fun Accelerate() {
        // Apply acceleration if we’re on a powered rail that is turned on.
        // If the rail is unpowered, do not decelerate as we only get here
        // in that case if we’re being pushed, in which case just keep going.
        val ActivatedRail = CrossedPoweredRail || (State isa Blocks.POWERED_RAIL && State.get(PoweredRailBlock.POWERED))
        val A = if (ActivatedRail) POWERED_RAIL_BOOST else NATURAL_SLOWDOWN

        // If this ends up inverting the movement, stop the cart instead.
        val XSign = sign(DX)
        val ZSign = sign(DZ)
        var NewDeltaX = DX + A * XSign
        var NewDeltaZ = DZ + A * ZSign
        if (sign(NewDeltaX) != XSign) NewDeltaX = .0
        if (sign(NewDeltaZ) != ZSign) NewDeltaZ = .0

        // Make sure we don’t end up accelerating infinitely.
        NewDeltaX = min(NewDeltaX, MAX_SPEED)
        NewDeltaZ = min(NewDeltaZ, MAX_SPEED)

        // Finally, apply the new movement.
        C.setVelocity(
            NewDeltaX,
            .0,
            NewDeltaZ
        )
    }

    /** Advance the block position by one block in the right direction.  */
    private fun Advance(World: ServerWorld) {
        // Advance to the next block.
        Block.move(Dir)

        // If the previous rail goes up, go up, if it goes down, go down,
        // unless we would hit a solid block.
        val Vert: Direction? = VertMovement(Track)
        if (Vert != null) {
            Block.move(Vert)
            State = World.getBlockState(Block)
            if (State.isSideSolidFullSquare(World, Block, Vert.opposite)) {
                Block.move(Vert.opposite)
            }
        }

        // Update the state.
        State = World.getBlockState(Block)
        UpdateRailShape()
    }

    /** Calculate our new Y position if we need to move down onto an ascending rail.  */
    private fun CalculateYOffs(X: Double, Z: Double): Double {
        // Compute the horizontal offset between us and the block we just left.
        //
        // Note that the coordinate system is such the block is the lower north-west
        // (negative-negative) corner of the coordinate system, i.e. we round *down*.
        //
        // VISUALISATION:
        //
        //  - <──────> +
        //    ┌──────┐
        //    │      │
        //    │      │
        //    └──────┘
        //
        // Assume we’re moving along the Z axis; if we take the fractional part of our
        // Z coordinate after applying horizontal movement, we end up inside the block;
        // note that which part we get depends on whether Z is negative or positive (we
        // count 0 as ‘positive’ here).
        //
        //     Z position                Z is negative               Z is ‘positive’
        //
        //      Z                           Z                            Z
        //      |                           [────] <─── = frac(Z)      [─] <─── = frac(Z)
        //    ┌──────┐                    ┌──────┐                     ┌──────┐
        //    │      │                    │      │                     │      │
        //    │      │                    │      │                     │      │
        //    └──────┘                    └──────┘                     └──────┘
        //
        // Furthermore, we may be entering the block from the left or right depending on
        // whether we’re moving in the positive or negative direction along the Z axis. Let
        // us assume that negative is left and positive is right here. Observe how the
        // fractional part matches the distance from the side on which we entered the block
        // if Z is negative, and we’re moving in the negative direction, and vice versa.
        //
        // Thus, we can simply take absolute value of the fractional part or our Z, or its
        // difference from 1 if the sign of our Z doesn’t match the direction we’re moving
        // in.
        //
        // The result of that computation is the distance from the face on which we entered
        // the block; we then simply need to offset from the bottom or top of the block that
        // contains the rail, depending on whether we’re moving up or down.
        val A = Dir.axis
        val V = if (A == Direction.Axis.X) MathHelper.fractionalPart(X) else MathHelper.fractionalPart(Z)
        val Sign = Dir.direction.offset()
        return if (V.sign.toInt() == Sign) abs(V) else 1 - abs(V)
    }

    /** Check for collisions. Return true if we need to stop moving.  */
    private fun CheckCollisions(X: Double, Y: Double, Z: Double): Boolean {
        // No collisions if we’re moving too slowly.
        val Speed = max(abs(DX), abs(DZ)).toFloat()
        if (Speed < COLLISION_THRESHOLD) return false

        // Calculate bounding box at the target position.
        val BB = EntityType.MINECART.getSpawnBox(X, Y, Z)

        // Check for colliding entities.
        val Level = C.world as ServerWorld
        val Entities = Level.getOtherEntities(C, BB, MinecartMover::CollisionCheckPredicate)
        val OurPlayer = C.firstPassenger as? ServerPlayerEntity
        for (E in Entities) {
            // Don’t collide with our own passenger.
            if (E == OurPlayer) continue

            // Extract the minecart and the player.
            var OtherMC: AbstractMinecartEntity? = null
            var OtherPlayer: ServerPlayerEntity? = null
            if (E is ServerPlayerEntity) {
                OtherPlayer = E
                val V = E.getVehicle()
                if (V is AbstractMinecartEntity) OtherMC = V
            } else if (E is AbstractMinecartEntity) {
                OtherMC = E
                val P = E.getFirstPassenger()
                if (P is ServerPlayerEntity) OtherPlayer = P
            }

            // Entity is a minecart (or a player riding one) that is moving in the same
            // direction as us; do not collide unless we’re moving faster than 1 block
            // per tick and the other one isn’t.
            val OtherSpeed = OtherMC?.velocity?.horizontalLength()?.toFloat() ?: 0f

            // Kaboom.
            val Where = E.pos
            var DealtDamage = false

            // Deal damage to the poor soul we just ran over.
            val Dmg = max(Speed, OtherSpeed) * 4
            if (OtherPlayer != null && !OtherPlayer.isCreative && OtherPlayer.hurtTime == 0) {
                val DS = GetDamageSource(Level, OtherMC != null, OurPlayer)
                OtherPlayer.damage(DS, Dmg)
                DealtDamage = true
            }

            // If there is a minecart, kill it and us as well. Note that we can also
            // collide with players that aren’t riding a minecart, so there may not
            // be a minecart here.
            if (OtherMC != null) {
                if (
                    OurPlayer != null &&
                    !OurPlayer.isSpectator &&
                    !OurPlayer.isCreative &&
                    OurPlayer.hurtTime == 0
                ) {
                    OurPlayer.damage(GetDamageSource(Level, true, OtherPlayer), Dmg)
                    DealtDamage = true
                }

                DropMinecart(Level, C, X, Y, Z)
                DropMinecart(Level, OtherMC, X, Y, Z)
                OtherMC.kill()
                C.kill()
            }

            // Play sound and particles.
            if (DealtDamage) {
                C.playSound(SoundEvents.ITEM_TOTEM_USE, 2f, 1f)
                Level.spawnParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    Where.x,
                    Where.y,
                    Where.z,
                    1,
                    .0,
                    .0,
                    .0,
                    .0
                )
            }

            // Do not process any more collisions. It is extremely unlikely that more than
            // two parties are ever involved in one, and we don’t want to get into a weird
            // state where we kill the same entity more than once.
            //
            // If we ran into another cart, then we’ve been destroyed. Stop ‘moving’,
            return OtherMC != null
        }

        // No collisions.
        return false
    }

    /** Compute the direction we’re moving in.  */
    private fun ComputeDirection() {
        Dir = if (DX != 0.0) if (DX > 0) Direction.EAST else Direction.WEST
        else if (DZ > 0) Direction.SOUTH else Direction.NORTH
    }

    /** Get the horizontal distance we need to travel  */
    private fun Distance(): Double {
        return max(abs(DX), abs(DZ))
    }

    /** Move within a block.  */
    private fun FinishMovement(Level: ServerWorld, HorizDist: Double, HorizDistBlocks: Int) {
        val HorizDistRem = HorizDist - HorizDistBlocks
        val BasePos: Vec3d = (if (HorizDistBlocks == 0) StartPos else Vec3d.ofBottomCenter(Block))
        val X = BasePos.x + HorizDistRem * sign(this.DX)
        val Z = BasePos.z + HorizDistRem * sign(this.DZ)
        val VertDir: Direction? = VertMovement(Track)
        val Down = VertDir === Direction.DOWN

        // Get the block in case we crossed over to a new one.
        Block.set(X, BasePos.y, Z)
        State = Level.getBlockState(Block)

        // If we’re in the air, and there is a rail below us, then we just
        // need move down. Similarly, if we’re in a solid block, and there’s
        // a rail above us, we need to move up.
        //
        // Only check at most one of the two conditions. Specifically, do
        // attempt to evade a solid block if we’re going downwards, else
        // we’ll end up phasing through blocks.
        if (if (Down) !State.isSideSolidFullSquare(Level, Block, Direction.UP) else State.BlocksMovement) {
            val MaybeRail = if (Down) Block.down() else Block.up()
            val RailState = Level.getBlockState(MaybeRail)

            // We’re above/below a rail.
            if (RailState isa BlockTags.RAILS) {
                Block.set(MaybeRail)
                State = RailState
            }
        }

        // If we’re moving downwards and are still blocked, then we’re up against
        // a solid block while going down a slope; stop here and don’t move the
        // cart; the code that handles stopping will do the rest.
        UpdateRailShape()
        if (Down && State.BlocksMovement) return

        // Calculate the height of the cart.
        //
        // On a straight rail, the height is a constant number above the block
        // that is the rail, equal to the height of the rail itself.
        //
        // On an ascending rail, the cart’s vertical offset from the top (if we’re
        // going down) or bottom (if we’re going up) of the rail block is equal to
        // how far into that block we have already moved.
        //
        // Calculate offset in the ascending case.
        if (Track.isAscending) {
            val Dist = CalculateYOffs(X, Z)

            // If we need to go down, offset from the top of the block; if we need to go
            // up, offset from the bottom of the block.
            C.setPos(
                X,
                Block.y + (if (Down) 1 - Dist else Dist),
                Z
            )

            // Done here.
            return
        }

        // Just stay here. If we’re just randomly in the air, we’ll derail later.
        C.setPos(X, Block.y + RAIL_HEIGHT, Z)
    }

    /**
     * Turn at an intersection.
     *
     * @return true if we actually made a turn.
     */
    private fun MaybeTurn(): Boolean {
        when (Track) {
            RailShape.NORTH_EAST -> {
                if (Dir === Direction.WEST) return Set(0.0, DX)
                else if (Dir === Direction.SOUTH) return Set(DZ, 0.0)
            }

            RailShape.NORTH_WEST -> {
                if (Dir === Direction.EAST) return Set(0.0, -DX)
                else if (Dir === Direction.SOUTH) return Set(-DZ, 0.0)
            }

            RailShape.SOUTH_EAST -> {
                if (Dir === Direction.WEST) return Set(0.0, -DX)
                else if (Dir === Direction.NORTH) return Set(-DZ, 0.0)
            }

            RailShape.SOUTH_WEST -> {
                if (Dir === Direction.EAST) return Set(0.0, DX)
                else if (Dir === Direction.NORTH) return Set(DZ, 0.0)
            }

            else -> {}
        }
        // No turn.
        return false
    }

    /** Actually move it.  */
    private fun Move() {
        val HorizDist = Distance()
        val HorizDistBlocks = HorizDist.toInt()
        val Level = C.world as ServerWorld

        // TODO:
        //   - Allow players to push a cart even when on an unpowered rail?
        //   - Single ascending unpowered rail next to an ascending normal rail (opposite direction) should stop a cart.
        //   - It also doesn’t work if there’s an ascending unpowered rail that goes downwards and then another unpowered rail.
        //   - Test we don’t fall through the floor if we move into stairs w/ no blocks below the stairs
        //   - ... or generally when moving downwards.

        // If we’re only moving one block, we need to remember to handle
        // turns first. There’s no need to do this after the loop as we
        // can’t get there if we end block movement on a turn.
        if (HorizDistBlocks == 0) MaybeTurn()
        else if (!MoveAlongTrack(Level, HorizDistBlocks)) return

        // Handle the remaining movement within the current block.
        FinishMovement(Level, HorizDist, HorizDistBlocks)

        // Stop if we derailed etc.
        val Pos = C.pos
        if (StopCart(Pos.x, Pos.y, Pos.z)) return

        // Apply acceleration etc.
        Accelerate()
    }

    /**
     * Move along the track for a number of blocks.
     *
     * @return true if we can keep moving, false if we need to stop.
     */
    private fun MoveAlongTrack(World: ServerWorld, NumBlocks: Int): Boolean {
        // Handle bends.
        //
        // We need to turn if we encounter a bend; however, there are a few problems
        // with doing this: if we skip over multiple bends or blocks following and
        // preceding a bend in a single tick, the client will render us as though we
        // just jumped through the air diagonally.
        //
        // To fix this, we stop all movement at this tick if we encounter a bend; at
        // the same time, we need to make sure that we can move out of a bend, so if
        // we start movement while already on a bend, it’s fine to move away from it.
        //
        // However, in that case, we need to take care to adjust the direction we’re
        // moving into accordingly.
        if (MaybeTurn()) ComputeDirection()

        // Do block movement.
        for (i in 0 ..< NumBlocks) {
            // Advance to the next block and stop if need be.
            Advance(World)

            // Activator rails eject passengers. Simply stop here; the rest of the
            // minecart code will take care of actually ejecting us.
            if (State isa Blocks.ACTIVATOR_RAIL && State.get(PoweredRailBlock.POWERED)) {
                C.CentreOn(Block)
                return false
            }

            // Activate detector rails.
            if (State isa Blocks.DETECTOR_RAIL && !State.get(DetectorRailBlock.POWERED)) {
                // Minecart needs to be on this block for this to work.
                val DT = State.block as DetectorRailBlock as DetectorRailBlockAccessor
                val Center = Block.toCenterPos()
                C.setPos(Center.x, Block.y + RAIL_HEIGHT, Center.z)
                DT.`Nguhcraft$UpdatePoweredStatus`(World, Block, State)
            }

            // Stop at turns; we’ve already moved past the first turn if there was one.
            //
            // Also apply deceleration here since we won’t actually get to the code that
            // normally takes care of that; we don’t need to check for powered rails here
            // since they can never be bent, and we don’t need to apply push acceleration
            // since we only enter this loop in the first place if we’re moving faster than
            // 1 block per tick; at that point it really doesn’t matter anymore.
            //
            if (MaybeTurn()) {
                // TODO: This is only here because client movement would look even more janky
                //       than it already was if it weren’t. Investigate whether we still need
                //       this slowdown here.
                C.CentreOn(Block)
                C.setVelocity(
                    DX + NATURAL_SLOWDOWN * sign(DX),
                    .0,
                    DZ + NATURAL_SLOWDOWN * sign(DZ)
                )
                return false
            }

            // Stop the cart if we have derailed.
            if (StopCart(Block.x.toDouble(), Block.y.toDouble(), Block.z.toDouble())) return false

            // Mark that we’re on a powered rail if that is the case to apply acceleration
            // later. If we get here, the rail must be powered, else we would have already returned
            // due to StopCart() above.
            if (State isa Blocks.POWERED_RAIL) CrossedPoweredRail = true
        }

        // Keep moving.
        return true
    }

    /** Used by MaybeTurn(); set new values for DX and DZ and return true.  */
    private fun Set(NewDX: Double, NewDZ: Double): Boolean {
        DX = NewDX
        DZ = NewDZ
        return true
    }

    /**
     * Stop the cart if need be; returns true if stopped.
     *
     * @param VX The current virtual X position.
     * @param VY The current virtual Y position.
     * @param VZ The current virtual Z position.
     * @return true if we need to stop processing movement.
     */
    private fun StopCart(VX: Double, VY: Double, VZ: Double): Boolean {
        // Derailed!!!
        if (!State.isIn(BlockTags.RAILS)) {
            // We hit a solid block; we’ll stop moving in this direction and
            // change directions.
            if (State.isSideSolidFullSquare(C.world, Block, Dir.opposite)) {
                // Move back a block; we started on a rail, so moving back will
                // put us back on a rail.

                Block.move(Dir.opposite)

                // If we were moving downhill, just stop outright.
                if (VertMovement(Track) === Direction.DOWN) {
                    C.velocity = Vec3d.ZERO

                    // Put us right up against the block we moved into.
                    var X = Block.x.toDouble()
                    var Z = Block.z.toDouble()
                    when (Track) {
                        RailShape.ASCENDING_EAST -> X += .001
                        RailShape.ASCENDING_WEST -> X += .999
                        RailShape.ASCENDING_SOUTH -> Z += .001
                        RailShape.ASCENDING_NORTH -> Z += .999
                        else -> {}
                    }
                    C.setPos(X, Block.y + RAIL_HEIGHT, Z)
                    return true
                }

                // Invert movement and slow down a bit. Stop here this tick.
                C.CentreOn(Block)
                C.setVelocity(-DX * HIT_BLOCK_MOD, .0, -DZ * HIT_BLOCK_MOD)
                return true
            }

            // Maybe we were moving straight and ended up in the air because
            // there is now an ascending rail below us that we need to snap
            // down onto.
            //
            // Update the MutableBlockPos destructively here because we’ll
            // derail anyway if we can’t salvage this here.
            Block.move(Direction.DOWN)
            State = C.world.getBlockState(Block)
            if (State isa BlockTags.RAILS) {
                UpdateRailShape()

                // Assume any ascending rail here is connected to the rail we
                // were just on because anything else would be nonsense. However,
                // do derail if the rail is straight and thus not connected to
                // the previous one because that means it’s going perpendicular
                // to where we’re moving.
                if (Track.isAscending) return false
            }

            // Derail on this block. Note that there will be no track here, so
            // we actually need to move down one block.
            C.CentreOn(Block)
            (C as AbstractMinecartEntityAccessor).`Nguhcraft$MoveOffRail`()
            return true
        }

        // Unpowered rail; emergency stop.
        if (State isa Blocks.POWERED_RAIL && !State.get(PoweredRailBlock.POWERED)) {
            C.CentreOn(Block)
            C.velocity = Vec3d.ZERO
            return true
        }

        // Check for collisions.
        return CheckCollisions(VX, VY, VZ)
    }

    /** Determine what rail shape we’re on.  */
    private fun UpdateRailShape() {
        // If we get here and end up on a non-rail, that’s because we’re about
        // to check for derailment anyway, so just no nothing here in that case.
        val B = State.block
        if (B is AbstractRailBlock) Track = State.get(B.shapeProperty)
    }

    /**
     * Based on the current movement direction and a rail, get the vertical
     * direction to move in, if any.
     */
    private fun VertMovement(Track: RailShape): Direction? {
        return when (Track) {
            RailShape.ASCENDING_EAST -> if (DX > 0) Direction.UP else Direction.DOWN
            RailShape.ASCENDING_WEST -> if (DX < 0) Direction.UP else Direction.DOWN
            RailShape.ASCENDING_SOUTH -> if (DZ > 0) Direction.UP else Direction.DOWN
            RailShape.ASCENDING_NORTH -> if (DZ < 0) Direction.UP else Direction.DOWN
            else -> null
        }
    }

    companion object {
        private const val AABB_GROW = 0.20000000298023224
        private const val COLLISION_THRESHOLD = 0.5
        private const val HIT_BLOCK_MOD = 0.95
        private const val MAX_PUSH_SPEED_SQUARED = 0.01
        private const val MAX_SPEED = 6.0
        private const val NATURAL_SLOWDOWN = -0.0001
        private const val NATURAL_SLOWDOWN_WITHOUT_PLAYER = -0.001
        private const val POWERED_RAIL_BOOST = 0.2
        private const val PUSH_ACCEL = 0.001
        private const val RAIL_HEIGHT = 0.0625
        private const val SLOPE_VERTICAL_MOD = 0.0078125
        private const val START_ACCEL = .05
        private const val TURN_MOD = 0.9
        private const val VERT_MOVEMENT_MULT = 1.125

        // Predicate that tests whether an entity can collide with a minecart.
        private fun CollisionCheckPredicate(E: Entity): Boolean {
            if (E.isRemoved) return false
            if (E is AbstractMinecartEntity) return true
            if (E !is ServerPlayerEntity) return false
            return E.isAlive && !E.isSpectator && !E.isCreative
        }

        /**
         * Check if two minecarts that touch each other should collide fatally.
         *
         * @param Us The minecart that is moving.
         * @param Them The minecart that is in the way.
         */
        fun AvoidFatalCollision(Us: AbstractMinecartEntity, Them: AbstractMinecartEntity): Boolean {
            val OurSpeed = Us.velocity.horizontalLength()
            val TheirSpeed = Them.velocity.horizontalLength()
            return OurSpeed < 1 || TheirSpeed > 1
        }

        fun DropMinecart(Level: ServerWorld, C: AbstractMinecartEntity, X: Double, Y: Double, Z: Double) {
            Level.spawnEntity(ItemEntity(
                Level,
                X + C.random.nextFloat(),
                Y,
                Z + C.random.nextFloat(),
                C.pickBlockStack
            ))
        }

        /** Get the damage source to use for a collision.  */
        fun GetDamageSource(
            W: World,
            InCart: Boolean,
            OtherPlayer: ServerPlayerEntity?
        ) = if (InCart) NguhDamageTypes.MinecartCollision(W, OtherPlayer)
            else NguhDamageTypes.MinecartRunOverBy(W, OtherPlayer)

        /**
         * Move a minecart along rails for a single tick.
         *
         * @implNote This must not be called if the minecart is not on a rail or if
         * there is no player in the minecart.
         */
        @JvmStatic
        fun Move(C: AbstractMinecartEntity, StartBlock: BlockPos, StartState: BlockState) {
            C.onLanding()

            // Snap down onto rails if need be.
            val Pos = C.snapPositionToRail(C.x, C.y, C.z)?.also { C.setPosition(it) } ?: C.pos

            // Compute player acceleration.
            //
            // This is how a minecart usually starts moving.
            val StartDelta = C.velocity
            var Pushed: Boolean
            var PlayerDeltaX = 0.0
            var PlayerDeltaZ = 0.0
            val SP = C.firstPassenger as ServerPlayerEntity
            run {
                val PlayerDelta = SP.velocity
                val PlayerMovement = PlayerDelta.horizontalLengthSquared()
                val OurMovement = StartDelta.horizontalLengthSquared()
                Pushed = PlayerMovement > 1.0E-4
                if (Pushed && OurMovement < MAX_PUSH_SPEED_SQUARED) {
                    PlayerDeltaX = PlayerDelta.x
                    PlayerDeltaZ = PlayerDelta.z
                }
            }

            // Initial acceleration check.
            //
            // This step is what gets a minecart moving if it isn’t already; there are
            // several things we need to consider here.
            //
            // First, if the player is ‘pushing’ (i.e. pressing a movement key while
            // sitting in the cart), determine initial movement based on the shape of
            // the rail, and the direction the player is facing.
            //
            // While we’re here, if we’re on a slope, also apply acceleration downwards.
            // Since the latter would outweigh any pushing done by the player anyway,
            // ignore player acceleration in that case.
            //
            // Lastly, also check if we’re up against a block and, if we’re on a powered
            // rail, apply powered rail acceleration in the *opposite* direction. Don’t do
            // anything if we’re between two blocks. Also ignore this if we’re on an ascending
            // rail so we don’t move back up.
            var X = StartDelta.x
            var Z = StartDelta.z
            val Rail = StartState.block as AbstractRailBlock
            val Track = StartState.get(Rail.shapeProperty)!!
            val Level = C.world as ServerWorld
            val Powered = StartState isa Blocks.POWERED_RAIL && StartState.get(PoweredRailBlock.POWERED)
            when (Track) {
                RailShape.ASCENDING_EAST -> X -= SLOPE_VERTICAL_MOD
                RailShape.ASCENDING_WEST -> X += SLOPE_VERTICAL_MOD
                RailShape.ASCENDING_SOUTH -> Z -= SLOPE_VERTICAL_MOD
                RailShape.ASCENDING_NORTH -> Z += SLOPE_VERTICAL_MOD
                RailShape.EAST_WEST -> {
                    X += PlayerDeltaX
                    if (Powered) {
                        val WestIsSolid = Level.getBlockState(StartBlock.west()).BlocksMovement
                        val EastIsSolid = Level.getBlockState(StartBlock.east()).BlocksMovement
                        if (WestIsSolid || EastIsSolid) {
                            // !WestIsSolid -> East is solid. Move *away from* east, i.e. move west,
                            // which is negative; vice versa for the other case. If both are set, do
                            // nothing.
                            if (!WestIsSolid) X -= START_ACCEL
                            else if (!EastIsSolid) X += START_ACCEL
                        }
                    }
                }

                RailShape.NORTH_SOUTH -> {
                    Z += PlayerDeltaZ
                    if (Powered) {
                        val NorthIsSolid = Level.getBlockState(StartBlock.north()).BlocksMovement
                        val SouthIsSolid = Level.getBlockState(StartBlock.south()).BlocksMovement
                        if (NorthIsSolid || SouthIsSolid) {
                            if (!NorthIsSolid) Z -= START_ACCEL
                            else if (!SouthIsSolid) Z += START_ACCEL
                        }
                    }
                }

                RailShape.SOUTH_EAST,
                RailShape.SOUTH_WEST,
                RailShape.NORTH_EAST,
                RailShape.NORTH_WEST -> {
                    // These are never powered, so not much to do here, fortunately.
                    X += PlayerDeltaX
                    Z += PlayerDeltaZ
                }
            }

            // Cart isn’t moving, so we don’t need to do anything.
            val AbsX = abs(X)
            val AbsZ = abs(Z)
            if (AbsX < MathHelper.EPSILON && AbsZ < MathHelper.EPSILON && !Pushed) {
                C.velocity = Vec3d.ZERO
                return
            }

            // Update delta.
            C.setVelocity(X, .0, Z)
            MinecartMover(
                C,
                Pos,
                C.velocity,
                AbsX,
                AbsZ,
                StartBlock,
                StartState,
                Track
            ).Move()
        }
    }
}
