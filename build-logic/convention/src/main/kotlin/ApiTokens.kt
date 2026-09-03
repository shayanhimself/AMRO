import org.gradle.api.Project
import java.util.Properties

/**
 * One source's read credential: the `BuildConfig` field it lands in, the key it sits under in
 * `local.properties`, and the environment variable CI supplies it through instead.
 */
data class ApiToken(
    val buildConfigField: String,
    val localProperty: String,
    val environmentVariable: String,
)

/**
 * Every source this build reads a credential for.
 *
 * A new source adds one entry here rather than a new plugin: `amro.api.tokens` already applies to
 * every module that needs one, so growing the list is what "adding a source" costs on the build
 * side.
 */
internal val API_TOKENS =
    listOf(
        ApiToken(
            buildConfigField = "TMDB_READ_ACCESS_TOKEN",
            localProperty = "tmdb.readAccessToken",
            environmentVariable = "TMDB_READ_ACCESS_TOKEN",
        ),
    )

/**
 * Reads [token] for this build.
 *
 * `local.properties` first, so a developer sets it once and nothing else has to know. The
 * environment second, because that file is not in the repository and CI has only its secret.
 *
 * @return the credential, or an empty string when neither supplies one. A clone without one still
 * builds: the app runs and fails its requests, and that source's live check skips.
 */
internal fun Project.readApiToken(token: ApiToken): String {
    val local =
        Properties()
            .apply {
                val file = rootProject.file("local.properties")
                if (file.exists()) {
                    file.inputStream().use { load(it) }
                }
            }.getProperty(token.localProperty)

    return local?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable(token.environmentVariable).orNull.orEmpty()
}
