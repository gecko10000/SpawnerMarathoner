@file:UseSerializers(MMComponentSerializer::class)

package gecko10000.spawnermarathoner.config

import com.charleskorn.kaml.YamlComment
import gecko10000.geckolib.config.serializers.MMComponentSerializer
import gecko10000.geckolib.extensions.MM
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Material
import org.bukkit.entity.EntitySnapshot

@Serializable
data class Config(
    val spawnerBreakConfirmationEnabled: Boolean = true,
    val spawnerBreakConfirmationTimeoutTicks: Int = 200,
    val spawnerBreakConfirmationMessageTitle: Component = MM.deserialize(
        "<red>Mine the spawner again"
    ),
    val spawnerBreakConfirmationMessageSubtitle: Component = MM.deserialize(
        "<red>if you're sure you want to destroy it."
    ),
    private val spawnerMinecartCustomName: String = "<#0F55B8><bold><type> Spawner",
    private val spawnerMinecartEmptySpawnerEntityName: Component = Component.text("Empty"),
    val spawnerMinecartRequiredPlayerRange: Int = 64,
    @YamlComment("Prevents minecart spawners from spawning if there are any nearby.")
    val nearbySpawnerMinecartsCheck: Int = 64,
    val validPlacementBlocks: Set<Material> = setOf(
        Material.AIR,
        Material.RAIL,
        Material.POWERED_RAIL
    ),
) {
    fun spawnerMinecartCustomName(entity: EntitySnapshot?) = MM.deserialize(
        spawnerMinecartCustomName,
        Placeholder.component(
            "type",
            entity?.entityType?.let { Component.translatable(it.translationKey()) }
                ?: spawnerMinecartEmptySpawnerEntityName
        )
    )
}
