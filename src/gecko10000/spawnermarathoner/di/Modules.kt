package gecko10000.geckoanvils.di

import gecko10000.spawnermarathoner.SpawnerMarathoner
import org.koin.dsl.module

fun pluginModules(plugin: SpawnerMarathoner) = module {
    single { plugin }
}
