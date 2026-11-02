package com.cartoniq.studyos

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Privacy-conscious widget: shows counts only, never task or note titles. */
class StudyWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = Db.get(ctx).dao()
                val today = dayStart()
                val tasks = dao.allTasks().count { !it.done && it.dueAt != null && it.dueAt < today + 86_400_000L }
                val cards = dao.allCards().count { it.dueAt <= System.currentTimeMillis() }
                val exam = dao.allExams().filter { it.examAt >= today }.minByOrNull { it.examAt }
                val examText = if (exam != null) "Next exam: " + examCountdown(exam.examAt, today) else "No upcoming exam"
                val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
                ids.forEach { id ->
                    val v = RemoteViews(ctx.packageName, R.layout.widget)
                    v.setTextViewText(R.id.wCounts, "$tasks due · $cards to review")
                    v.setTextViewText(R.id.wExam, examText)
                    v.setOnClickPendingIntent(R.id.wRoot, open)
                    mgr.updateAppWidget(id, v)
                }
            } finally { pending.finish() }
        }
    }
}
