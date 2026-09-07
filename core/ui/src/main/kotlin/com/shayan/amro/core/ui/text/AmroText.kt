package com.shayan.amro.core.ui.text

import android.content.Context
import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalResources
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Text resource wrapper, and the arguments it is formatted with, if any.
 *
 * This is passed to UI elements, which resolve it to a string, without needing the ViewModel to
 * hold a [Context] or [Resources]. The text is resolved in the configuration the composition is
 * current in, so it can change with the language.
 */
@Immutable
sealed interface AmroText {
    /**
     * Text the app does not own, or a value already written out.
     *
     * @property text the words themselves.
     */
    data class Raw(
        val text: String,
    ) : AmroText

    /**
     * A string resource, and the arguments it is formatted with.
     *
     * @property id the resource.
     * @property formatArgs what fills its placeholders, in the order it declares them.
     */
    data class Resource(
        @StringRes val id: Int,
        val formatArgs: ImmutableList<Any>,
    ) : AmroText {
        // Second constructor is for call sites, which are more readable with a vararg than a list.
        constructor(
            @StringRes id: Int,
            vararg formatArgs: Any,
        ) : this(id = id, formatArgs = formatArgs.toImmutableList())
    }

    /**
     * A plural resource, the count that picks its form, and the arguments it is formatted with.
     *
     * @property id the resource.
     * @property count what the form is chosen by, which is not necessarily what is printed.
     * @property formatArgs what fills the chosen form's placeholders.
     */
    data class Plural(
        @PluralsRes val id: Int,
        val count: Int,
        val formatArgs: ImmutableList<Any>,
    ) : AmroText {
        // Second constructor is for call sites, which are more readable with a vararg than a list.
        constructor(
            @PluralsRes id: Int,
            count: Int,
            vararg formatArgs: Any,
        ) : this(id = id, count = count, formatArgs = formatArgs.toImmutableList())
    }

    /**
     * Several texts read as one.
     *
     * @property texts what is joined, in the order it is read.
     * @property separator what sits between two of them.
     */
    data class Joined(
        val texts: ImmutableList<AmroText>,
        val separator: AmroText = Empty,
    ) : AmroText {
        // Second constructor is for call sites, which are more readable with a vararg than a list.
        constructor(
            texts: ImmutableList<AmroText>,
            separator: String,
        ) : this(texts = texts, separator = Raw(separator))
    }

    /** Nothing to say. */
    data object Empty : AmroText

    /** Resolves to a string in the current composition's configuration. */
    @Composable
    fun resolve(): String = resolve(LocalResources.current)

    /**
     * Resolves to a string outside a composition.
     *
     * @param context whose configuration decides the language.
     */
    fun resolve(context: Context): String = resolve(context.resources)

    private fun resolve(resources: Resources): String =
        when (this) {
            is Raw -> text
            is Resource -> resources.getString(id, *formatArgs.resolved(resources))
            is Plural -> resources.getQuantityString(id, count, *formatArgs.resolved(resources))
            is Joined -> texts.joinToString(separator.resolve(resources)) { it.resolve(resources) }
            Empty -> ""
        }

    private fun List<Any>.resolved(resources: Resources): Array<Any> =
        map { arg -> if (arg is AmroText) arg.resolve(resources) else arg }.toTypedArray()
}
