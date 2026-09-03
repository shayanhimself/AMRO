// Gives the applying module every API source's read credential as a BuildConfig field.
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.VariantDimension

val tokens = API_TOKENS.associateWith { readApiToken(it) }

/**
 * Adds every [tokens] entry as a `BuildConfig` field.
 *
 * A plain function rather than the call inlined where `defaultConfig` is reached, because that
 * call sits inside `extensions.configure`, whose own DSL receiver would otherwise make `forEach`'s
 * receiver ambiguous to a reader.
 */
fun VariantDimension.addApiTokenFields() {
    tokens.forEach { (token, value) ->
        buildConfigField("String", token.buildConfigField, "\"$value\"")
    }
}

// Applied by id rather than by type, because an application and a library configure the same two
// things through different extensions. Whichever this module is, one of these fires.
plugins.withId("com.android.application") {
    extensions.configure<ApplicationExtension> {
        buildFeatures.buildConfig = true
        defaultConfig.addApiTokenFields()
    }
}

plugins.withId("com.android.library") {
    extensions.configure<LibraryExtension> {
        buildFeatures.buildConfig = true
        defaultConfig.addApiTokenFields()
    }
}
