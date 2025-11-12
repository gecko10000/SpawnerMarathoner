package gecko10000.spawnermarathoner

import gecko10000.geckoanvils.di.MyKoinComponent
import gecko10000.geckolib.misc.Task
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.CreatureSpawner
import org.bukkit.entity.EntityType
import org.bukkit.entity.minecart.RideableMinecart
import org.bukkit.entity.minecart.SpawnerMinecart
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.SpawnerSpawnEvent
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent
import org.bukkit.event.vehicle.VehicleDestroyEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.spawner.Spawner
import org.koin.core.component.inject
import java.util.*

class SpawnerListeners : Listener, MyKoinComponent {

    companion object {
        private val gson = GsonComponentSerializer.gson()
    }

    private val plugin: SpawnerMarathoner by inject()
    private val ORIGINAL_RPR_KEY = NamespacedKey(plugin, "original_rpr")
    private val ORIGINAL_CUSTOM_NAME = NamespacedKey(plugin, "original_custom_name")

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    private fun copySpawnerExceptRPR(from: Spawner, to: Spawner) {
        to.spawnCount = from.spawnCount
        to.spawnRange = from.spawnRange
        to.setPotentialSpawns(from.potentialSpawns)
        to.spawnedEntity = from.spawnedEntity
        to.maxSpawnDelay = from.maxSpawnDelay
        to.minSpawnDelay = from.minSpawnDelay
        to.delay = from.delay
        to.maxNearbyEntities = from.maxNearbyEntities
    }

    // "Picks up" the spawner by creating a spawner minecart
    @EventHandler
    fun VehicleBlockCollisionEvent.onMinecartHitSpawner() {
        val minecart = this.vehicle as? RideableMinecart ?: return
        val hitBlock = this.block
        val spawner = hitBlock.state as? Spawner ?: return
        hitBlock.type = Material.AIR
        minecart.world.spawn(hitBlock.location.add(0.5, 0.0, 0.5), SpawnerMinecart::class.java) { m ->
            copySpawnerExceptRPR(spawner, m)
            m.requiredPlayerRange = plugin.config.spawnerMinecartRequiredPlayerRange
            m.persistentDataContainer.set(ORIGINAL_RPR_KEY, PersistentDataType.INTEGER, spawner.requiredPlayerRange)
            val originalCustomName = minecart.customName()?.let { gson.serialize(it) }
            if (originalCustomName != null) {
                m.persistentDataContainer.set(ORIGINAL_CUSTOM_NAME, PersistentDataType.STRING, originalCustomName)
            }
            m.customName(plugin.config.spawnerMinecartCustomName(spawner.spawnedEntity))
            m.isCustomNameVisible = true
        }
        minecart.remove()
        minecart.world.playSound(Sound.sound {
            it.type(Key.key("block.spawner.break"))
        }, minecart.location.x, minecart.location.y, minecart.location.z)
    }

    private fun isValidPlaceLocation(location: Location): Boolean {
        return location.block.type in plugin.config.validPlacementBlocks
    }

    // Places the spawner back down on the ground.
    @EventHandler
    fun VehicleDestroyEvent.onSpawnerMinecartDestroy() {
        val spawnerMinecart = this.vehicle as? SpawnerMinecart ?: return
        val originalRPR = spawnerMinecart.persistentDataContainer.get(ORIGINAL_RPR_KEY, PersistentDataType.INTEGER)
        originalRPR ?: return // not our spawner minecart

        // Invalid location, don't destroy but don't place
        if (!isValidPlaceLocation(spawnerMinecart.location)) {
            this.isCancelled = true
            return
        }
        spawnerMinecart.remove()
        val block = spawnerMinecart.location.block
        block.type = Material.SPAWNER
        val spawner = block.getState(false) as CreatureSpawner
        copySpawnerExceptRPR(spawnerMinecart, spawner)
        spawner.requiredPlayerRange = originalRPR
        val originalCustomName =
            spawnerMinecart.persistentDataContainer.get(ORIGINAL_CUSTOM_NAME, PersistentDataType.STRING)
        val nameComponent = gson.deserializeOrNull(originalCustomName)
        spawnerMinecart.customName(nameComponent)
        spawnerMinecart.world.playSound(Sound.sound {
            it.type(Key.key("block.spawner.place"))
        }, spawnerMinecart.location.x, spawnerMinecart.location.y, spawnerMinecart.location.z)
    }

    private val confirmations = mutableMapOf<UUID, Pair<Task?, Block>>()

    // Shows a confirmation when breaking spawners.
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

    // Prevent spawning when there are multiple spawner minecarts close by.
    // Stops exploitation via stacking of the spawner minecarts.
    @EventHandler
    private fun SpawnerSpawnEvent.onMinecartSpawnerSpawn() {
        if (this.isCancelled || this.spawner != null) return
        val range = plugin.config.nearbySpawnerMinecartsCheck.toDouble()
        val nearbyEntities = this.location.getNearbyEntities(range, range, range)
        if (nearbyEntities.count { it.type == EntityType.SPAWNER_MINECART } > 1) this.isCancelled = true
    }

}
