package com.example.signalwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import android.widget.RemoteViews

class SignalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_signal)
            val (networkType, signalPercent) = getSignalInfo(context)

            views.setTextViewText(R.id.tv_network_type, networkType)
            views.setTextViewText(R.id.tv_signal_percent, "$signalPercent%")
            views.setProgressBar(R.id.pb_signal, 100, signalPercent, false)

            val intent = Intent(context, SignalWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getSignalInfo(context: Context): Pair<String, Int> {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return Pair("بدون اتصال", 0)
            val capabilities = cm.getNetworkCapabilities(network) ?: return Pair("بدون اتصال", 0)

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val rssi = wifiManager.connectionInfo.rssi
                    val level = WifiManager.calculateSignalLevel(rssi, 100)
                    Pair("شبکه Wi-Fi", level.coerceIn(0, 100))
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        (tm.signalStrength?.level ?: 0) * 25
                    } else {
                        50
                    }
                    Pair("اینترنت موبایل", level.coerceIn(0, 100))
                }
                else -> Pair("اتصال ناشناس", 0)
            }
        }
    }
}
