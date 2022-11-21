package com.minar.birday.widgets

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.byteArrayToBitmap
import com.minar.birday.utilities.formatEventList
import com.minar.birday.utilities.getNextYears
import com.minar.birday.utilities.nextDateFormatted
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalStdlibApi::class)
abstract class BirdayWidgetProvider : AppWidgetProvider() {
    abstract var widgetLayout: Int

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update each of the widgets with the remote adapter
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @ExperimentalStdlibApi
    internal fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        try {
            when (widgetLayout) {
                R.layout.widget_upcoming -> {
                    updateUpcoming(context, appWidgetManager, appWidgetId)
                }
                R.layout.widget_minimal -> {
                    updateMinimal(context, appWidgetManager, appWidgetId)
                }
            }

        } catch (e: Exception) {
            Log.d("widget", "${e.message}")
        }
    }

    // Update the minimal old widget
    private fun updateMinimal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_minimal)
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        val intent = Intent(context, MainActivity::class.java)

        Thread {
            // Get the next events and the proper formatter
            val eventDao: EventDao = EventDatabase.getBirdayDatabase(context).eventDao()
            val nextEvents: List<EventResult> = eventDao.getOrderedNextEventsStatic()

            // Launch the app on click
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            views.setOnClickPendingIntent(R.id.minimalWidgetMain, pendingIntent)

            // Remove events in the future today (eg: now is december 1st 2023, an event has original date = december 1st 2050)
            var filteredNextEvents = nextEvents.toMutableList()
            filteredNextEvents.removeIf { getNextYears(it) == 0 }
            // If the events are all in the future, display them but avoid confetti
            if (filteredNextEvents.isEmpty()) {
                filteredNextEvents = nextEvents.toMutableList()
            }

            // Make sure to show if there's more than one event
            var widgetUpcoming = formatEventList(filteredNextEvents, true, context, false)
            if (filteredNextEvents.isNotEmpty()) widgetUpcoming += "\n${
                nextDateFormatted(
                    filteredNextEvents[0],
                    formatter,
                    context
                )
            }"
            views.setTextViewText(R.id.minimalWidgetText, widgetUpcoming)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }.start()
    }

    // Update the modern upcoming widget
    private fun updateUpcoming(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val hideImages = sp.getBoolean("hide_images", false)
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val fullFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        val views = RemoteViews(context.packageName, R.layout.widget_upcoming)
        val intent = Intent(context, MainActivity::class.java)

        views.setTextViewText(R.id.eventWidgetDate, fullFormatter.format(LocalDate.now()))
        Thread {
            // Get the next events and the proper formatter
            val eventDao: EventDao = EventDatabase.getBirdayDatabase(context).eventDao()
            val nextEvents: List<EventResult> = eventDao.getOrderedNextEventsStatic()

            // Launch the app on click
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            views.setOnClickPendingIntent(R.id.background, pendingIntent)

            // If there are zero events, hide the list
            if (nextEvents.isEmpty()) {
                views.setViewVisibility(R.id.eventWidgetList, View.GONE)
                // Restore the widget to its default state
                views.setTextViewText(
                    R.id.eventWidgetText,
                    context.getString(R.string.no_next_event)
                )
                views.setImageViewResource(
                    R.id.eventWidgetImage,
                    R.drawable.placeholder_other_image
                )
            }
            // If there's one event or more, update the list and the main widget
            else {
                views.setViewVisibility(R.id.eventWidgetList, View.VISIBLE)

                // Remove events in the future today (eg: now is december 1st 2023, an event has original date = december 1st 2050)
                var filteredNextEvents = nextEvents.toMutableList()
                filteredNextEvents.removeIf { getNextYears(it) == 0 }
                // If the events are all in the future, display them but avoid confetti
                if (filteredNextEvents.isEmpty()) {
                    filteredNextEvents = nextEvents.toMutableList()
                }

                // Make sure to show if there's more than one event
                var widgetUpcoming = formatEventList(filteredNextEvents, true, context, false)
                if (filteredNextEvents.isNotEmpty()) widgetUpcoming += "\n${
                    nextDateFormatted(
                        filteredNextEvents[0],
                        formatter,
                        context
                    )
                }"
                views.setTextViewText(R.id.eventWidgetText, widgetUpcoming)
                views.setTextViewText(
                    R.id.eventWidgetTitle,
                    context.getString(R.string.appwidget_upcoming)
                )

                // If the image shouldn't be shown, simply hide the view and free up space
                if (hideImages) views.setViewVisibility(R.id.eventWidgetImageGroup, View.GONE)
                // Else proceed to fill the data for the next event
                else {
                    views.setViewVisibility(R.id.eventWidgetImageGroup, View.VISIBLE)
                    if (filteredNextEvents[0].image != null && filteredNextEvents[0].image!!.isNotEmpty()) {
                        views.setImageViewBitmap(
                            R.id.eventWidgetImage,
                            byteArrayToBitmap(filteredNextEvents[0].image!!)
                        )
                    } else views.setImageViewResource(
                        R.id.eventWidgetImage,
                        // Set the image depending on the event type, the drawable are a b&w version
                        when (filteredNextEvents[0].type) {
                            EventCode.BIRTHDAY.name -> R.drawable.placeholder_birthday_image
                            EventCode.ANNIVERSARY.name -> R.drawable.placeholder_anniversary_image
                            EventCode.DEATH.name -> R.drawable.placeholder_death_image
                            EventCode.NAME_DAY.name -> R.drawable.placeholder_name_day_image
                            else -> R.drawable.placeholder_other_image
                        }
                    )
                }


                // Set up the intent that starts the EventViewService, which will provide the views
                val widgetServiceIntent = Intent(context, EventWidgetService::class.java)

                // Set up the RemoteViews object to use a RemoteViews adapter and populate the data
                views.apply {
                    setRemoteAdapter(R.id.eventWidgetList, widgetServiceIntent)
                    // setEmptyView can be used to choose the view displayed when the collection has no items
                }

                // Template to handle the click listener for each item
                val clickIntentTemplate = Intent(context, MainActivity::class.java)
                val clickPendingIntentTemplate: PendingIntent = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(clickIntentTemplate)
                    .getPendingIntent(
                        3,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                views.setPendingIntentTemplate(R.id.eventWidgetList, clickPendingIntentTemplate)

                // Fill the list with the next events
                appWidgetManager.notifyAppWidgetViewDataChanged(
                    appWidgetId,
                    R.id.eventWidgetList
                )
            }
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }.start()
    }
}