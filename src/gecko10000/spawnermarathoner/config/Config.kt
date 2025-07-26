package gecko10000.spawnermarathoner.config

import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
data class Config(
    val adjacentBlockRequired: Boolean = true,
    val adjacentBlockType: Material = Material.SOUL_SAND,
)
