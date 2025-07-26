@file:UseSerializers(MMComponentSerializer::class)

package gecko10000.spawnermarathoner.config

import gecko10000.geckolib.config.serializers.MMComponentSerializer
import gecko10000.geckolib.extensions.MM
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.kyori.adventure.text.Component
import org.bukkit.Material

@Serializable
data class Config(
    val adjacentBlockRequired: Boolean = true,
    val adjacentBlockType: Material = Material.SOUL_SAND,
    val spawnerBreakConfirmationEnabled: Boolean = true,
    val spawnerBreakConfirmationTimeoutTicks: Int = 200,
    val spawnerBreakConfirmationMessageTitle: Component = MM.deserialize(
        "<red>Mine the spawner again"
    ),
    val spawnerBreakConfirmationMessageSubtitle: Component = MM.deserialize(
        "<red>if you're sure you want to destroy it."
    )
)
