package com.shayan.amro.e2e

/**
 * Marks a test as end to end: the app's production code against the live APIs.
 *
 * The runner selects on it, which is what separates E2E tests from flow tests.
 * Retained at runtime because that selection happens on the device.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class E2eTest
