package gecko10000.spawnermarathoner

import gecko10000.geckolib.config.YamlFileManager
import gecko10000.spawnermarathoner.config.Config
import gecko10000.spawnermarathoner.di.MyKoinContext
import org.bukkit.plugin.java.JavaPlugin

class SpawnerMarathoner : JavaPlugin() {

    private val configFile = YamlFileManager(
        configDirectory = dataFolder,
        initialValue = Config(),
        serializer = Config.serializer(),
    )
    val config: Config
        get() = configFile.value

    override fun onEnable() {
        MyKoinContext.init(this)
        SpawnerListeners()
        CommandHandler().register()
    }

    fun reloadConfigs() {
        configFile.reload()
    }

}
