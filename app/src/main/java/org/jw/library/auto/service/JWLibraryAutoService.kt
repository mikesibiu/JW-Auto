package org.jw.library.auto.service

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import org.jw.library.auto.background.ContentSyncScheduler
import org.jw.library.auto.data.ContentRepository
import org.jw.library.auto.data.PlaybackPositionRepository
import org.jw.library.auto.playback.PlaybackManager
import java.util.ArrayList
import androidx.media.MediaBrowserServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JWLibraryAutoService : MediaBrowserServiceCompat() {
    private lateinit var contentRepository: ContentRepository
    private lateinit var playbackManager: PlaybackManager
    private lateinit var playbackPositionRepository: PlaybackPositionRepository
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        contentRepository = ContentRepository(this)
        playbackPositionRepository = PlaybackPositionRepository(this)
        playbackManager = PlaybackManager(this) { state, position ->
            when (state) {
                PlaybackStateCompat.STATE_PLAYING ->
                    startForeground(NOTIFICATION_ID, playbackManager.buildNotification())
                PlaybackStateCompat.STATE_PAUSED ->
                    stopForegroundCompat(removeNotification = false)
                PlaybackStateCompat.STATE_STOPPED ->
                    stopForegroundCompat(removeNotification = true)
            }
            val currentId = playbackManager.mediaSession.controller.metadata?.description?.mediaId
            if (currentId != null) {
                serviceScope.launch(Dispatchers.IO) {
                    playbackPositionRepository.save(currentId, position)
                }
            }
        }
        sessionToken = playbackManager.mediaSession.sessionToken

        // Schedule background content sync
        ContentSyncScheduler.schedulePeriodicSync(this)
        ContentSyncScheduler.scheduleImmediateSync(this)

        // Ensure we call startForeground quickly after Android Auto starts the service
        startForeground(NOTIFICATION_ID, playbackManager.buildNotification())
        stopForegroundCompat(removeNotification = false)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return if (isClientAllowed(clientPackageName, clientUid)) {
            BrowserRoot(ContentRepository.ROOT_ID, null)
        } else {
            null
        }
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.detach()
        serviceScope.launch {
            try {
                val mediaContents = withContext(Dispatchers.IO) {
                    contentRepository.getChildren(parentId)
                }
                val mediaItems = mediaContents.map { content ->
                    val lastPosition = withContext(Dispatchers.IO) {
                        playbackPositionRepository.get(content.id)
                    }
                    val description = MediaDescriptionCompat.Builder()
                        .setMediaId(content.id)
                        .setTitle(content.title)
                        .setSubtitle(content.subtitle)
                        .setExtras(Bundle().apply {
                            content.streamUrl?.let { putString(PlaybackManager.KEY_STREAM_URI, it) }
                            putString(MediaMetadataCompat.METADATA_KEY_TITLE, content.title)
                            if (content.playlistUrls.isNotEmpty()) {
                                putStringArrayList(
                                    PlaybackManager.KEY_PLAYLIST,
                                    ArrayList(content.playlistUrls)
                                )
                            }
                            putLong(PlaybackManager.KEY_LAST_POSITION, lastPosition)
                        })
                        .build()

                    MediaBrowserCompat.MediaItem(
                        description,
                        if (content.isBrowsable) MediaBrowserCompat.MediaItem.FLAG_BROWSABLE else MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                    )
                }
                result.sendResult(mediaItems.toMutableList())
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to load children for $parentId", t)
                result.sendResult(mutableListOf())
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        playbackManager.release()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return if (SERVICE_INTERFACE == intent?.action) super.onBind(intent) else null
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private val ALLOWED_CLIENT_PREFIXES = setOf(
            "com.google.android.projection.gearhead", // Android Auto host + validator processes
            "com.google.android.car.kitvalidator",    // Validation utility
            "com.google.android.googlequicksearchbox", // Assistant
            "com.google.android.gms" // Play Services sometimes mediates
        )
        private const val TAG = "JWLibraryAutoService"
    }

    private fun stopForegroundCompat(removeNotification: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(removeNotification)
        }
    }

    private fun isClientAllowed(clientPackage: String?, clientUid: Int): Boolean {
        if (clientPackage == null) return false
        if (clientPackage == packageName) return true
        if (ALLOWED_CLIENT_PREFIXES.none { clientPackage.startsWith(it) }) {
            Log.w(TAG, "Rejecting media client $clientPackage")
            return false
        }
        val packagesForUid = packageManager.getPackagesForUid(clientUid) ?: return false
        return packagesForUid.contains(clientPackage)
    }
}
