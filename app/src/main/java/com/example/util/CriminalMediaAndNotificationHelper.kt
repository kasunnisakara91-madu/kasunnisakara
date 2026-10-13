package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import android.util.Base64
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.google.firebase.Timestamp
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

object CriminalMediaAndNotificationHelper {

    private const val MSG_CHANNEL_ID = "criminal_app_messages"
    private const val CALL_CHANNEL_ID = "criminal_app_calls"

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val msgChannel = NotificationChannel(
                MSG_CHANNEL_ID,
                "CRIMINAL APP Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time message notifications"
                enableVibration(true)
            }
            val callChannel = NotificationChannel(
                CALL_CHANNEL_ID,
                "CRIMINAL APP Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice and video call alerts"
                enableVibration(true)
            }
            manager.createNotificationChannel(msgChannel)
            manager.createNotificationChannel(callChannel)
        }
    }

    fun showMessageNotification(
        context: Context,
        senderName: String,
        messageText: String,
        enableSound: Boolean,
        enableVibrate: Boolean
    ) {
        ensureNotificationChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                if (enableVibrate) triggerHaptic(context)
                return
            }
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val builder = NotificationCompat.Builder(context, MSG_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (!enableSound) {
            builder.setSilent(true)
        }
        if (enableVibrate) {
            triggerHaptic(context)
        }
        manager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
    }

    fun showIncomingCallNotification(
        context: Context,
        callerName: String,
        callType: String,
        enableVibrate: Boolean
    ) {
        ensureNotificationChannels(context)
        if (enableVibrate) triggerHaptic(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val builder = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Incoming $callType Call")
            .setContentText("$callerName is calling on CRIMINAL APP")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)

        manager.notify(99001, builder.build())
    }

    fun triggerHaptic(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {
        }
    }

    fun encodeImageUriToBase64(context: Context, uri: Uri, maxDimension: Int = 520): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null

            val ratio = min(
                maxDimension.toFloat() / original.width.toFloat(),
                maxDimension.toFloat() / original.height.toFloat()
            ).coerceAtMost(1f)
            val width = (original.width * ratio).toInt().coerceAtLeast(1)
            val height = (original.height * ratio).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(original, width, height, true)

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun decodeBase64ToImageBitmap(base64Str: String?): ImageBitmap? {
        if (base64Str.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bmp?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    fun resolveDocumentDisplayName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            } ?: uri.lastPathSegment ?: "Shared_File.pdf"
        } catch (e: Exception) {
            "Shared_File.pdf"
        }
    }

    fun parseColorSafe(hex: String, fallback: Color = Color(0xFF00E676)): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            fallback
        }
    }

    fun formatMessageTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: Date()
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
    }

    fun formatDateSeparator(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: Date()
        val nowCal = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { time = date }

        return when {
            nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) -> "Today"
            nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
        }
    }

    fun formatCallDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    fun buildRealisticWebRtcSdpOffer(callType: String, callerUid: String): String {
        val sessionId = System.currentTimeMillis()
        return buildString {
            appendLine("v=0")
            appendLine("o=$callerUid $sessionId 2 IN IP4 0.0.0.0")
            appendLine("s=CRIMINAL_APP_${callType}_SESSION")
            appendLine("t=0 0")
            appendLine("a=ice-options:trickle")
            appendLine("a=ICE-STUN:stun:stun.l.google.com:19302")
            appendLine("m=audio 9 UDP/TLS/RTP/SAVPF 111")
            appendLine("a=rtpmap:111 opus/48000/2")
            if (callType == "VIDEO") {
                appendLine("m=video 9 UDP/TLS/RTP/SAVPF 96")
                appendLine("a=rtpmap:96 VP8/90000")
            }
        }
    }
}
