import java.util.*

object Config {
    private val properties: Properties
    init {
        val propertiesFileName = "controlling.properties"
        val propertiesFileUrl = this::class.java.classLoader.getResource(propertiesFileName)
            ?: error("Properties file not found: $propertiesFileName.")
        properties = Properties().apply { load(propertiesFileUrl.openStream()) }
    }

    val targetDir: String = properties.getProperty("targetDir")

    object DB {
        val host: String = properties.getProperty("db.host")
        val port: String = properties.getProperty("db.port")
        val name: String = properties.getProperty("db.name")
        val user: String = properties.getProperty("db.user")
        val password: String = properties.getProperty("db.password")
    }

    object Google {
        val apiClientSecret: String = properties.getProperty("google.api.clientSecret")
        val tokenDir: String = properties.getProperty("google.tokenDir")
    }

}

