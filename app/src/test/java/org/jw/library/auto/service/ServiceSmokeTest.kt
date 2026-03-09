package org.jw.library.auto.service

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smoke test: verifies the service starts without crashing.
 *
 * Catches: Room main-thread access exceptions, missing permissions, bad onCreate() wiring.
 * Any crash in onCreate() (e.g. calling a suspend DAO method on the main thread) fails here
 * before the app ever reaches a phone.
 *
 * Rule: if a new onCreate() crash reaches the user, reproduce it here first, then fix it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ServiceSmokeTest {

    @Before
    fun setUp() {
        // WorkManager requires explicit initialization in Robolectric test environment
        WorkManagerTestInitHelper.initializeTestWorkManager(
            ApplicationProvider.getApplicationContext()
        )
    }

    @Test
    fun `service onCreate completes without throwing`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val intent = Intent(context, JWLibraryAutoService::class.java).apply {
            action = "android.media.browse.MediaBrowserService"
        }
        Robolectric.buildService(JWLibraryAutoService::class.java, intent)
            .create()
            .get()
    }
}
