package com.fahim.alyfobserver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class WebsiteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            views.setOnClickPendingIntent(R.id.widget_webview_image, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)

            val serviceIntent = Intent(context, WidgetUpdateService::class.java)
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            context.startService(serviceIntent)
        }
    }
}