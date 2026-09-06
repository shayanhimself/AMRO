package com.shayan.amro.core.ui.viewmodel

/**
 * How long a `stateIn` pipeline keeps collecting after its last subscriber leaves. Long enough to
 * survive a configuration change, short enough that a backgrounded screen stops reading its
 * sources.
 */
const val SUBSCRIPTION_TIMEOUT_MILLIS: Long = 5_000L
