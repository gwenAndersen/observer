package com.fahim.alyfobserver

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RemoteViews
import android.view.View

class WidgetUpdateService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val webView = WebView(this)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    view.post {
                        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        view.draw(canvas)

                        val views = RemoteViews(packageName, R.layout.widget_layout)
                        views.setImageViewBitmap(R.id.widget_webview_image, bitmap)

                        val appWidgetManager = AppWidgetManager.getInstance(this@WidgetUpdateService)
                        appWidgetManager.updateAppWidget(appWidgetId, views)

                        stopSelf()
                    }
                }
            }
            webView.loadUrl("file:///android_asset/widget.html")
            webView.layout(0, 0, 1000, 1000) // A reasonable size for the webview to be rendered
        }

        return START_NOT_STICKY
    }
}