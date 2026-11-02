package com.cartoniq.studyos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

/** Open Trivia DB (opentdb.com): free, no API key. Questions are CC BY-SA 4.0. */
val TRIVIA_CATEGORIES = listOf(
    0 to "Any", 9 to "General", 17 to "Science", 18 to "Computers", 19 to "Maths", 22 to "Geography", 23 to "History",
    21 to "Sports", 25 to "Art", 27 to "Animals", 11 to "Film", 12 to "Music", 10 to "Books", 20 to "Mythology",
    24 to "Politics", 28 to "Vehicles"
)

class TriviaResult(val questions: List<Question> = emptyList(), val error: String? = null)

suspend fun fetchTrivia(category: Int, difficulty: String, amount: Int): TriviaResult = withContext(Dispatchers.IO) {
    try {
        val url = buildString {
            append("https://opentdb.com/api.php?amount=").append(amount.coerceIn(1, 50)).append("&encode=url3986")
            if (category > 0) append("&category=").append(category)
            if (difficulty.isNotEmpty()) append("&difficulty=").append(difficulty)
        }
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        val body = try {
            if (c.responseCode != 200) return@withContext TriviaResult(error = "Server error (${c.responseCode}). Try again later.")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
        val o = JSONObject(body)
        when (o.optInt("response_code", -1)) {
            0 -> {}
            1 -> return@withContext TriviaResult(error = "Not enough questions for that choice. Try fewer, or another topic.")
            5 -> return@withContext TriviaResult(error = "Too many requests. Wait a few seconds and try again.")
            else -> return@withContext TriviaResult(error = "Import failed. Try again later.")
        }
        val arr = o.getJSONArray("results")
        fun d(s: String) = URLDecoder.decode(s, "UTF-8")
        val out = List(arr.length()) { i ->
            val r = arr.getJSONObject(i)
            val wrong = r.getJSONArray("incorrect_answers")
            val opts = MutableList(wrong.length()) { d(wrong.getString(it)) }
            val right = d(r.getString("correct_answer"))
            opts.add(right)
            opts.shuffle()
            val p = opts + List((4 - opts.size).coerceAtLeast(0)) { "" }
            Question(prompt = d(r.getString("question")), o1 = p[0], o2 = p[1], o3 = p[2], o4 = p[3],
                correct = opts.indexOf(right), topic = d(r.getString("category")), source = "Open Trivia DB")
        }
        TriviaResult(out)
    } catch (e: Exception) {
        TriviaResult(error = "Couldn't reach Open Trivia DB. Check your internet connection.")
    }
}
