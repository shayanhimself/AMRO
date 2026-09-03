package com.shayan.amro.core.model

/**
 * An image at the two sizes the app renders. It is owned by the app, and sources map their own
 * sizes to it. A source that reports only one size, repeats it here, so the fallback sits in that
 * source's mapper rather than at every call site
 *
 * A size is not a pixel width. A size is relative to the image it belongs to, so a small poster and
 * a small backdrop can have different widths and ratios.
 *
 * @property small the URL a list row loads.
 * @property large the URL the detail screen loads.
 */
data class ImageRef(
    val small: String,
    val large: String,
)
