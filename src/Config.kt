import java.util.*

object Config {
    private val properties: Properties

    object DB {
        val host = properties.getProperty("db.host")
        val port = properties.getProperty("db.port")
        val name = properties.getProperty("db.name")
        val user = properties.getProperty("db.user")
        val password = properties.getProperty("db.password")
    }

    object Google {
        val apiClientSecret = properties.getProperty("google.api.clientSecret")
        val tokenDir = properties.getProperty("google.tokenDir")
    }

    init {
        val propertiesFileName = "controlling.properties"
        val propertiesFileUrl = this::class.java.classLoader.getResource(propertiesFileName) ?: error("Properties file not found: $propertiesFileName.")
        properties = Properties().apply { load(propertiesFileUrl.openStream()) }
    }
}

