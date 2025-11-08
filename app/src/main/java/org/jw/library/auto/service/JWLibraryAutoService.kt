package org.jw.library.auto.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import org.jw.library.auto.R
import org.jw.library.auto.data.ContentProvider
import org.jw.library.auto.playback.PlaybackManager
import org.jw.library.auto.ui.MainActivity

/**
 * MediaBrowserService for Android Auto integration
 * This service manages content browsing and audio playback for the car interface
 */
class JWLibraryAutoService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var playbackManager: PlaybackManager
    private lateinit var contentProvider: ContentProvider
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val TAG = "JWLibraryAutoService"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "jw_library_auto_playback"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize notification channel
        createNotificationChannel()
        notificationManager = getSystemService(NotificationManager::class.java)

        // Initialize content provider
        contentProvider = ContentProvider(this)

        // Initialize MediaSession
        mediaSession = MediaSessionCompat(this, TAG).apply {
            // Set flags for media button and transport controls
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            // Set initial playback state
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                    .setActions(getAvailableActions())
                    .build()
            )

            // Set session token
            setSessionToken(sessionToken)
        }

        // Initialize playback manager
        playbackManager = PlaybackManager(
            context = this,
            mediaSession = mediaSession,
            onPlaybackStateChanged = { state ->
                handlePlaybackStateChange(state)
            }
        )

        // Set callback for media session
        mediaSession.setCallback(playbackManager.getMediaSessionCallback())
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        // Whitelist of authorized packages
        val allowedPackages = setOf(
            "com.google.android.projection.gearhead", // Android Auto
            "com.google.android.carassistant", // Google Assistant
            packageName // Your own app
        )

        // Verify package is authorized
        if (!isPackageAuthorized(clientPackageName, clientUid, allowedPackages)) {
            android.util.Log.w(TAG, "Unauthorized package attempted connection: $clientPackageName")
            return null // Reject connection
        }

        return BrowserRoot(ContentProvider.ROOT_ID, null)
    }

    /**
     * Verify that a client package is authorized to connect
     */
    private fun isPackageAuthorized(
        packageName: String,
        uid: Int,
        allowedPackages: Set<String>
    ): Boolean {
        if (!allowedPackages.contains(packageName)) {
            return false
        }

        // Verify UID matches package (prevents package name spoofing)
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appInfo.uid == uid
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        // Detach the result to allow async loading
        result.detach()

        val mediaItems = when (parentId) {
            ContentProvider.ROOT_ID -> {
                // Load root categories
                contentProvider.getRootCategories()
            }
            else -> {
                // Load content for specific category
                contentProvider.getContentForCategory(parentId)
            }
        }

        result.sendResult(mediaItems.toMutableList())
    }

    /**
     * Handle playback state changes
     */
    private fun handlePlaybackStateChange(state: Int) {
        when (state) {
            PlaybackStateCompat.STATE_PLAYING -> {
                // Start foreground with notification
                try {
                    startForeground(NOTIFICATION_ID, createNotification())
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to start foreground service", e)
                }
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                // Stay in foreground but update notification
                notificationManager.notify(NOTIFICATION_ID, createNotification())
            }
            PlaybackStateCompat.STATE_STOPPED -> {
                // Remove foreground status and notification
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
            }
            PlaybackStateCompat.STATE_NONE,
            PlaybackStateCompat.STATE_ERROR -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
            }
        }
    }

    /**
     * Create notification for playback control
     */
    private fun createNotification(): Notification {
        val controller = mediaSession.controller
        val mediaMetadata = controller.metadata
        val playbackState = controller.playbackState

        // Create intent to open app
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(mediaMetadata?.description?.title ?: getString(R.string.app_name))
            .setContentText(mediaMetadata?.description?.subtitle)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)

        // Add play/pause action
        if (playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
            builder.addAction(
                R.drawable.ic_pause,
                getString(R.string.action_pause),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PAUSE
                )
            )
        } else {
            builder.addAction(
                R.drawable.ic_play,
                getString(R.string.action_play),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PLAY
                )
            )
        }

        // Add stop action
        builder.addAction(
            R.drawable.ic_stop,
            getString(R.string.action_stop),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_STOP
            )
        )

        // Set style for media controls
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1) // Show play/pause and stop in compact view
        )

        return builder.build()
    }

    /**
     * Create notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Get available playback actions
     */
    private fun getAvailableActions(): Long {
        return PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_PLAY_PAUSE
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            playbackManager.release()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error releasing playback manager", e)
        }

        try {
            mediaSession.release()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error releasing media session", e)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Clean up before stopping
        playbackManager.release()
        mediaSession.release()
        stopSelf()
    }
}
