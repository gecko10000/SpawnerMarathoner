package gecko10000.spawnermarathoner

import gecko10000.geckoanvils.di.MyKoinComponent
import gecko10000.geckolib.misc.Task
import net.kyori.adventure.title.Title
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.CreatureSpawner
import org.bukkit.entity.Player
import org.bukkit.entity.WindCharge
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.util.Vector
import org.koin.core.component.inject
import java.util.*
import kotlin.math.abs

class SpawnerListeners : Listener, MyKoinComponent {

    companion object {
        private const val PUSH_DISTANCE_PERM_PREFIX = "spawnermarathoner.pushdistance."
    }

    private val plugin: SpawnerMarathoner by inject()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
        plugin.server.consoleSender.hasPermission(PUSH_DISTANCE_PERM_PREFIX) // to register with LP
    }

    private fun velocityToBlockFace(velocity: Vector): BlockFace {
        val absX = abs(velocity.x)
        val absY = abs(velocity.y)
        val absZ = abs(velocity.z)
        if (absY > absX && absY > absZ) {
            return if (velocity.y >= 0) BlockFace.UP else BlockFace.DOWN
        }
        if (absX > absZ) {
            return if (velocity.x >= 0) BlockFace.EAST else BlockFace.WEST
        }
        return if (velocity.z >= 0) BlockFace.SOUTH else BlockFace.NORTH
    }

    private val adjacentFaces = setOf(
        BlockFace.UP, BlockFace.DOWN,
        BlockFace.NORTH, BlockFace.SOUTH,
        BlockFace.EAST, BlockFace.WEST
    )

    private fun adjacentBlocksHave(block: Block, type: Material): Boolean {
        for (face in adjacentFaces) {
            if (block.getRelative(face).type == type) return true
        }
        return false
    }

    private fun moveSpawner(old: CreatureSpawner, block: Block) {
        if (old.block == block) return
        block.type = Material.SPAWNER
        val new = old.copy(block.location)
        new.update()
        old.block.type = Material.AIR
    }

    private fun getPushDistance(player: Player): Int {
        return player.effectivePermissions
            .filter { it.value }
            .map { it.permission }
            .filter { it.startsWith(PUSH_DISTANCE_PERM_PREFIX) }
            .map { it.substringAfter(PUSH_DISTANCE_PERM_PREFIX) }
            .mapNotNull { it.toIntOrNull() }
            .maxOrNull() ?: 1
    }

    private fun isValidSpawnerDestination(block: Block): Boolean {
        if (!block.isEmpty) return false
        if (!plugin.config.adjacentBlockRequired) return true
        return adjacentBlocksHave(block, plugin.config.adjacentBlockType)
    }

    @EventHandler
    fun ProjectileHitEvent.onWindChargeHitSpawner() {
        // Wind charge fired
        val projectile = this.entity
        if (projectile !is WindCharge) return
        // by a player
        val player = projectile.shooter
        if (player !is Player) return
        // into a spawner
        val block = this.hitBlock ?: return
        val state = block.getState(false)
        if (state !is CreatureSpawner) return
        val velocity = projectile.velocity
        val direction = velocityToBlockFace(velocity)
        val distanceToTravel = getPushDistance(player)
        var intermediate = block
        for (i in 0..<distanceToTravel) {
            intermediate = intermediate.getRelative(direction)
            if (!isValidSpawnerDestination(intermediate)) {
                intermediate = intermediate.getRelative(direction.oppositeFace)
                break
            }
        }
        val result = intermediate
        moveSpawner(state, result)
    }

    private val confirmations = mutableMapOf<UUID, Pair<Task?, Block>>()

    @EventHandler
    private fun BlockBreakEvent.onBreakSpawner() {
        if (this.block.type != Material.SPAWNER) return

        val uuid = this.player.uniqueId
        val awaitingConfirm = confirmations.remove(uuid)
        // Confirmed
        if (awaitingConfirm?.second == this.block) {
            awaitingConfirm.first?.cancel()
            return
        }
        val task = if (plugin.config.spawnerBreakConfirmationEnabled)
            Task.syncDelayed(
                { -> confirmations.remove(uuid) },
                plugin.config.spawnerBreakConfirmationTimeoutTicks
            ) else null
        confirmations.put(uuid, task to this.block)
        this.player.showTitle(
            Title.title(
                plugin.config.spawnerBreakConfirmationMessageTitle,
                plugin.config.spawnerBreakConfirmationMessageSubtitle
            )
        )
        this.isCancelled = true
    }

}
