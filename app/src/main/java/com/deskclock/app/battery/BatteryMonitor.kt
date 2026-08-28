package com.deskclock.app.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class BatteryState(
    val percent: Int,
    /** Actively charging (or full). False while a charge limiter holds the level with power connected. */
    val charging: Boolean,
    /** Power source connected, regardless of whether the battery is being charged right now. */
    val plugged: Boolean,
)

/**
 * ACTION_BATTERY_CHANGED is a sticky broadcast, so registering the receiver immediately yields the
 * current state — no separate initial query needed.
 */
fun batteryFlow(context: Context): Flow<BatteryState> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            trySend(intent.toBatteryState())
        }
    }
    val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    sticky?.let { trySend(it.toBatteryState()) }
    awaitClose { context.unregisterReceiver(receiver) }
}

private fun Intent.toBatteryState(): BatteryState {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 0
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    return BatteryState(percent = percent, charging = charging, plugged = plugged)
}
