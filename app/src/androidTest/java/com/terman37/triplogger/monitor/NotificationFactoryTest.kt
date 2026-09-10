package com.terman37.triplogger.monitor

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.terman37.triplogger.R
import com.terman37.triplogger.core.TripRecorder
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device tests for the monitoring notification — the Android piece JVM tests
 * cannot cover. A missing small icon used to make NotificationManager throw
 * ("Invalid notification (no valid small icon)") and crash the service; these
 * tests post the notification for every state and assert it is valid.
 *
 * No foreground service is started here on purpose: starting one from
 * instrumentation is subject to background-start restrictions and made the
 * tests flaky. The factory is the piece that broke, so it is what we test.
 */
@RunWith(AndroidJUnit4::class)
class NotificationFactoryTest {

    @get:Rule
    val notifications: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS,
    )

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    @After
    fun tearDown() {
        manager.cancelAll()
    }

    @Test
    fun everyPhase_producesAValidPostableNotification() {
        NotificationFactory.ensureChannel(context)

        val phases = listOf(
            TripRecorder.Phase.IDLE to 0.0,
            TripRecorder.Phase.RECORDING to 12.4,
            TripRecorder.Phase.GRACE to 12.4,
        )
        phases.forEach { (phase, km) ->
            val notification = NotificationFactory.create(context, phase, km)

            // THE regression this test exists for.
            assertNotNull("phase=$phase: small icon missing", notification.smallIcon)
            assertTrue(
                "phase=$phase: small icon must be a resource",
                notification.smallIcon!!.type == Icon.TYPE_RESOURCE,
            )
            assertNotNull("phase=$phase: content intent", notification.contentIntent)
            // Swiping it away must notify the receiver (dismissal = stop).
            assertNotNull("phase=$phase: delete intent", notification.deleteIntent)
            assertTrue(
                "phase=$phase: must be ongoing",
                notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
            )

            // Posting exercises NotificationManager.fixNotification, where an
            // invalid notification (e.g. no small icon) throws and crashes the
            // caller. That exception is the regression this guards; whether the
            // notification shows up in activeNotifications depends on the
            // permission state, so it is not asserted.
            try {
                manager.notify(NotificationFactory.NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                throw AssertionError("phase=$phase: system rejected the notification", e)
            }
            manager.cancel(NotificationFactory.NOTIFICATION_ID)
        }
    }

    @Test
    fun text_alwaysLeadsWithMonitoringActive() {
        val idle = NotificationFactory.textFor(context, TripRecorder.Phase.IDLE, 0.0)
        val recording = NotificationFactory.textFor(context, TripRecorder.Phase.RECORDING, 12.4)
        val grace = NotificationFactory.textFor(context, TripRecorder.Phase.GRACE, 12.4)

        assertTrue(idle, idle.startsWith(context.getString(R.string.notification_monitoring_text)))
        assertTrue(idle, idle == context.getString(R.string.notification_monitoring_text))
        assertTrue(recording, recording.startsWith("Monitoring active") && recording.contains("12.4 km"))
        assertTrue(grace, grace.startsWith("Monitoring active") && grace.contains("Disconnected"))
    }

    @Test
    fun pendingIntents_areConfiguredForOpenAndDismiss() {
        val notification = NotificationFactory.create(context, TripRecorder.Phase.IDLE, 0.0)
        // Non-null and immutable pending intents are what make the notification
        // open the app on tap and react to dismissal.
        assertNotNull(notification.contentIntent)
        assertNotNull(notification.deleteIntent)
        assertTrue(notification.contentIntent.isImmutable)
    }
}
