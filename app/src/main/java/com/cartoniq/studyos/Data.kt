package com.cartoniq.studyos

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import android.net.Uri
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Entity data class Subject(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val color: Long)
@Entity data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String,
    val subjectId: Long? = null, val dueAt: Long? = null, val priority: Int = 1, val done: Boolean = false,
    @ColumnInfo(defaultValue = "0") val recur: Int = 0
)
@Entity data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val body: String,
    val pinned: Boolean = false, val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val tags: String = "",
    @ColumnInfo(defaultValue = "''") val folder: String = ""
)
@Entity data class FocusSession(@PrimaryKey(autoGenerate = true) val id: Long = 0, val startedAt: Long, val minutes: Int)

@Entity data class Deck(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String)
@Entity data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val deckId: Long, val front: String, val back: String,
    val dueAt: Long = 0, val intervalDays: Int = 0, val ease: Int = 250, val reviews: Int = 0
)
@Entity data class Exam(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val examAt: Long, val subjectId: Long? = null)
@Entity data class MockTest(@PrimaryKey(autoGenerate = true) val id: Long = 0, val examId: Long, val takenAt: Long, val score: Int, val outOf: Int)

@Entity data class Question(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val prompt: String, val o1: String, val o2: String,
    val o3: String = "", val o4: String = "", val correct: Int = 0, val explanation: String = "",
    @ColumnInfo(defaultValue = "''") val topic: String = "", @ColumnInfo(defaultValue = "''") val source: String = ""
) { fun options() = listOf(o1, o2, o3, o4) }
@Entity data class QuizAttempt(@PrimaryKey(autoGenerate = true) val id: Long = 0, val takenAt: Long, val score: Int, val total: Int)
@Entity data class Chapter(@PrimaryKey(autoGenerate = true) val id: Long = 0, val subjectId: Long, val title: String)
@Entity data class Topic(@PrimaryKey(autoGenerate = true) val id: Long = 0, val chapterId: Long, val title: String, val status: Int = 0)
@Entity data class Slot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val weekday: Int, val startMin: Int, val endMin: Int,
    val title: String, val subjectId: Long? = null
)

@Dao interface StudyDao {
    @Query("SELECT * FROM Subject ORDER BY id") fun subjects(): Flow<List<Subject>>
    @Insert suspend fun addSubject(s: Subject)
    @Query("DELETE FROM Subject WHERE id = :id") suspend fun deleteSubject(id: Long)
    @Query("UPDATE Task SET subjectId = NULL WHERE subjectId = :id") suspend fun unlinkTasks(id: Long)

    @Query("SELECT * FROM Task ORDER BY done, dueAt IS NULL, dueAt") fun tasks(): Flow<List<Task>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addTask(t: Task)
    @Update suspend fun updateTask(t: Task)
    @Delete suspend fun deleteTask(t: Task)

    @Query("SELECT * FROM Note ORDER BY pinned DESC, updatedAt DESC") fun notes(): Flow<List<Note>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveNote(n: Note): Long
    @Delete suspend fun deleteNote(n: Note)

    @Query("SELECT * FROM FocusSession ORDER BY startedAt DESC LIMIT 200") fun sessions(): Flow<List<FocusSession>>
    @Insert suspend fun addSession(s: FocusSession)

    @Query("UPDATE Slot SET subjectId = NULL WHERE subjectId = :id") suspend fun unlinkSlots(id: Long)
    @Query("DELETE FROM Topic WHERE chapterId IN (SELECT id FROM Chapter WHERE subjectId = :id)") suspend fun deleteTopicsOfSubject(id: Long)
    @Query("DELETE FROM Chapter WHERE subjectId = :id") suspend fun deleteChaptersOfSubject(id: Long)
    @Query("SELECT * FROM Question ORDER BY id DESC") fun questions(): Flow<List<Question>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveQuestion(q: Question)
    @Delete suspend fun deleteQuestion(q: Question)
    @Query("SELECT * FROM QuizAttempt ORDER BY takenAt DESC LIMIT 100") fun attempts(): Flow<List<QuizAttempt>>
    @Insert suspend fun addAttempt(a: QuizAttempt)
    @Query("SELECT * FROM Chapter ORDER BY id") fun chapters(): Flow<List<Chapter>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveChapter(c: Chapter)
    @Query("DELETE FROM Chapter WHERE id = :id") suspend fun deleteChapter(id: Long)
    @Query("DELETE FROM Topic WHERE chapterId = :id") suspend fun deleteTopicsOfChapter(id: Long)
    @Query("SELECT * FROM Topic ORDER BY id") fun topics(): Flow<List<Topic>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTopic(t: Topic)
    @Delete suspend fun deleteTopic(t: Topic)
    @Query("SELECT * FROM Slot ORDER BY startMin") fun slots(): Flow<List<Slot>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSlot(s: Slot)
    @Delete suspend fun deleteSlot(s: Slot)
    @Query("SELECT * FROM Question") suspend fun allQuestions(): List<Question>
    @Query("SELECT * FROM QuizAttempt") suspend fun allAttempts(): List<QuizAttempt>
    @Query("SELECT * FROM Chapter") suspend fun allChapters(): List<Chapter>
    @Query("SELECT * FROM Topic") suspend fun allTopics(): List<Topic>
    @Query("SELECT * FROM Slot") suspend fun allSlots(): List<Slot>
    @Query("DELETE FROM Question") suspend fun clearQuestions()
    @Query("DELETE FROM QuizAttempt") suspend fun clearAttempts()
    @Query("DELETE FROM Chapter") suspend fun clearChapters()
    @Query("DELETE FROM Topic") suspend fun clearTopics()
    @Query("DELETE FROM Slot") suspend fun clearSlots()
    @Query("SELECT * FROM Deck ORDER BY id") fun decks(): Flow<List<Deck>>
    @Insert suspend fun addDeck(d: Deck)
    @Query("DELETE FROM Deck WHERE id = :id") suspend fun deleteDeck(id: Long)
    @Query("DELETE FROM Flashcard WHERE deckId = :id") suspend fun deleteCardsOf(id: Long)
    @Query("SELECT * FROM Flashcard ORDER BY id") fun cards(): Flow<List<Flashcard>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveCard(c: Flashcard)
    @Delete suspend fun deleteCard(c: Flashcard)
    @Query("SELECT * FROM Exam ORDER BY examAt") fun exams(): Flow<List<Exam>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveExam(e: Exam)
    @Query("DELETE FROM Exam WHERE id = :id") suspend fun deleteExam(id: Long)
    @Query("DELETE FROM MockTest WHERE examId = :id") suspend fun deleteMocksOf(id: Long)
    @Query("SELECT * FROM MockTest ORDER BY takenAt DESC") fun mocks(): Flow<List<MockTest>>
    @Insert suspend fun addMock(m: MockTest)
    @Delete suspend fun deleteMock(m: MockTest)
    @Query("SELECT * FROM Deck") suspend fun allDecks(): List<Deck>
    @Query("SELECT * FROM Flashcard") suspend fun allCards(): List<Flashcard>
    @Query("SELECT * FROM Exam") suspend fun allExams(): List<Exam>
    @Query("SELECT * FROM MockTest") suspend fun allMocks(): List<MockTest>
    @Query("DELETE FROM Deck") suspend fun clearDecks()
    @Query("DELETE FROM Flashcard") suspend fun clearCards()
    @Query("DELETE FROM Exam") suspend fun clearExams()
    @Query("DELETE FROM MockTest") suspend fun clearMocks()

    @Query("SELECT * FROM Subject") suspend fun allSubjects(): List<Subject>
    @Query("SELECT * FROM Task") suspend fun allTasks(): List<Task>
    @Query("SELECT * FROM Note") suspend fun allNotes(): List<Note>
    @Query("SELECT * FROM FocusSession") suspend fun allSessions(): List<FocusSession>
    @Query("DELETE FROM Subject") suspend fun clearSubjects()
    @Query("DELETE FROM Task") suspend fun clearTasks()
    @Query("DELETE FROM Note") suspend fun clearNotes()
    @Query("DELETE FROM FocusSession") suspend fun clearSessions()
}

private val MIG_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `Deck` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `Flashcard` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `deckId` INTEGER NOT NULL, `front` TEXT NOT NULL, `back` TEXT NOT NULL, `dueAt` INTEGER NOT NULL, `intervalDays` INTEGER NOT NULL, `ease` INTEGER NOT NULL, `reviews` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `Exam` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `examAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `MockTest` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `examId` INTEGER NOT NULL, `takenAt` INTEGER NOT NULL, `score` INTEGER NOT NULL, `outOf` INTEGER NOT NULL)")
    }
}

private val MIG_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `Question` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `prompt` TEXT NOT NULL, `o1` TEXT NOT NULL, `o2` TEXT NOT NULL, `o3` TEXT NOT NULL, `o4` TEXT NOT NULL, `correct` INTEGER NOT NULL, `explanation` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `QuizAttempt` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `takenAt` INTEGER NOT NULL, `score` INTEGER NOT NULL, `total` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `Chapter` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subjectId` INTEGER NOT NULL, `title` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `Topic` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `chapterId` INTEGER NOT NULL, `title` TEXT NOT NULL, `status` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `Slot` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `weekday` INTEGER NOT NULL, `startMin` INTEGER NOT NULL, `endMin` INTEGER NOT NULL, `title` TEXT NOT NULL, `subjectId` INTEGER)")
        db.execSQL("ALTER TABLE `Task` ADD COLUMN `recur` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `Note` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
    }
}

private val MIG_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `Question` ADD COLUMN `topic` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `Question` ADD COLUMN `source` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `Note` ADD COLUMN `folder` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `Exam` ADD COLUMN `subjectId` INTEGER")
    }
}

@Database(entities = [Subject::class, Task::class, Note::class, FocusSession::class, Deck::class, Flashcard::class, Exam::class, MockTest::class,
    Question::class, QuizAttempt::class, Chapter::class, Topic::class, Slot::class], version = 4, exportSchema = false)
abstract class Db : RoomDatabase() {
    abstract fun dao(): StudyDao
    companion object {
        @Volatile private var inst: Db? = null
        fun get(c: Context): Db = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, Db::class.java, "studyos.db").addMigrations(MIG_1_2, MIG_2_3, MIG_3_4).build().also { inst = it }
        }
    }
}

val Context.store by preferencesDataStore("settings")
private val THEME = intPreferencesKey("theme")
private val GOAL = intPreferencesKey("goal")
private val F_END = longPreferencesKey("focus_end")
private val F_MIN = intPreferencesKey("focus_min")
private val F_REM = longPreferencesKey("focus_rem")
val K_ONBOARD = booleanPreferencesKey("onboarded")
val K_REM_ON = booleanPreferencesKey("rem_on")
val K_REM_H = intPreferencesKey("rem_h")
val K_REM_M = intPreferencesKey("rem_m")
val K_AMOLED = booleanPreferencesKey("amoled")
val K_ACCENT = intPreferencesKey("accent")
val K_FSCALE = intPreferencesKey("fscale")
val K_AWAKE = booleanPreferencesKey("awake")
val K_HAPTIC = booleanPreferencesKey("haptic")
val K_SECURE = booleanPreferencesKey("secure")
val K_LOCKMIN = intPreferencesKey("lockmin")
private val PIN = stringPreferencesKey("pin")
private val LAST_BACKUP = longPreferencesKey("last_backup")

class StudyVm(app: Application) : AndroidViewModel(app) {
    private val dao = Db.get(app).dao()
    private val ds = app.store
    private fun <T> Flow<T>.hot(init: T) = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), init)

    val subjects = dao.subjects().hot(emptyList())
    val tasks = dao.tasks().hot(emptyList())
    val notes = dao.notes().hot(emptyList())
    val sessions = dao.sessions().hot(emptyList())
    val theme = ds.data.map { it[THEME] ?: 0 }.hot(0)
    val goal = ds.data.map { it[GOAL] ?: 120 }.hot(120)
    val focusEnd = ds.data.map { it[F_END] ?: 0L }.hot(0L)
    val focusMin = ds.data.map { it[F_MIN] ?: 25 }.hot(25)

    fun addSubject(name: String, color: Long) = viewModelScope.launch { dao.addSubject(Subject(name = name, color = color)) }
    fun deleteSubject(s: Subject) = viewModelScope.launch {
        dao.unlinkTasks(s.id); dao.unlinkSlots(s.id); dao.deleteTopicsOfSubject(s.id); dao.deleteChaptersOfSubject(s.id); dao.deleteSubject(s.id)
    }
    fun addTask(t: Task) = viewModelScope.launch { dao.addTask(t) }
    fun updateTask(t: Task) = viewModelScope.launch { dao.updateTask(t) }
    fun deleteTask(t: Task) = viewModelScope.launch { dao.deleteTask(t) }
    suspend fun saveNote(n: Note): Long = dao.saveNote(n.copy(updatedAt = System.currentTimeMillis()))
    fun saveNoteAsync(n: Note) = viewModelScope.launch { saveNote(n) }
    fun deleteNote(n: Note) = viewModelScope.launch { dao.deleteNote(n) }
    fun setTheme(i: Int) = viewModelScope.launch { ds.edit { it[THEME] = i } }
    fun setGoal(m: Int) = viewModelScope.launch { ds.edit { it[GOAL] = m.coerceIn(15, 720) } }

    private val db = Db.get(app)
    val amoled = ds.data.map { it[K_AMOLED] ?: false }.hot(false)
    val accent = ds.data.map { it[K_ACCENT] ?: 0 }.hot(0)
    val fscale = ds.data.map { it[K_FSCALE] ?: 100 }.hot(100)
    val awake = ds.data.map { it[K_AWAKE] ?: false }.hot(false)
    val haptics = ds.data.map { it[K_HAPTIC] ?: true }.hot(true)
    val secure = ds.data.map { it[K_SECURE] ?: false }.hot(false)
    val lockMin = ds.data.map { it[K_LOCKMIN] ?: 1 }.hot(1)
    val lastBackup = ds.data.map { it[LAST_BACKUP] ?: 0L }.hot(0L)
    /** null until settings are loaded, "" when no PIN is set, otherwise the PIN hash */
    val pin = ds.data.map<Preferences, String?> { it[PIN] ?: "" }.hot(null)
    val locked = MutableStateFlow(true)
    var stoppedAt = 0L
    val toast = MutableStateFlow<String?>(null)

    fun <T> put(k: Preferences.Key<T>, v: T) = viewModelScope.launch { ds.edit { it[k] = v } }
    private fun hash(p: String) = MessageDigest.getInstance("SHA-256").digest("studyos:$p".toByteArray()).joinToString("") { "%02x".format(it) }
    fun pinOk(p: String, stored: String) = hash(p) == stored
    fun setPin(p: String) { locked.value = false; put(PIN, if (p.isEmpty()) "" else hash(p)) }

    private suspend fun clearAll() { dao.clearSessions(); dao.clearNotes(); dao.clearTasks(); dao.clearSubjects(); dao.clearCards(); dao.clearDecks(); dao.clearMocks(); dao.clearExams()
        dao.clearQuestions(); dao.clearAttempts(); dao.clearTopics(); dao.clearChapters(); dao.clearSlots() }
    fun deleteAll() = viewModelScope.launch {
        db.withTransaction { clearAll() }
        ds.edit { it.clear() }
        Reminders.cancel(getApplication<Application>())
        locked.value = false
        toast.value = "All data deleted"
    }

    private suspend fun exportJson(): String = JSONObject().apply {
        put("app", "StudyOS"); put("schema", 4); put("createdAt", System.currentTimeMillis())
        put("subjects", JSONArray(dao.allSubjects().map { JSONObject().put("id", it.id).put("name", it.name).put("color", it.color) }))
        put("tasks", JSONArray(dao.allTasks().map {
            JSONObject().put("id", it.id).put("title", it.title).put("subjectId", it.subjectId).put("dueAt", it.dueAt)
                .put("priority", it.priority).put("done", it.done).put("recur", it.recur) }))
        put("notes", JSONArray(dao.allNotes().map {
            JSONObject().put("id", it.id).put("title", it.title).put("body", it.body).put("pinned", it.pinned).put("updatedAt", it.updatedAt).put("tags", it.tags).put("folder", it.folder) }))
        put("sessions", JSONArray(dao.allSessions().map { JSONObject().put("id", it.id).put("startedAt", it.startedAt).put("minutes", it.minutes) }))
        put("decks", JSONArray(dao.allDecks().map { JSONObject().put("id", it.id).put("title", it.title) }))
        put("cards", JSONArray(dao.allCards().map {
            JSONObject().put("id", it.id).put("deckId", it.deckId).put("front", it.front).put("back", it.back).put("dueAt", it.dueAt)
                .put("intervalDays", it.intervalDays).put("ease", it.ease).put("reviews", it.reviews) }))
        put("exams", JSONArray(dao.allExams().map { JSONObject().put("id", it.id).put("title", it.title).put("examAt", it.examAt).put("subjectId", it.subjectId) }))
        put("mocks", JSONArray(dao.allMocks().map {
            JSONObject().put("id", it.id).put("examId", it.examId).put("takenAt", it.takenAt).put("score", it.score).put("outOf", it.outOf) }))
        put("questions", JSONArray(dao.allQuestions().map {
            JSONObject().put("id", it.id).put("prompt", it.prompt).put("o1", it.o1).put("o2", it.o2).put("o3", it.o3).put("o4", it.o4)
                .put("correct", it.correct).put("explanation", it.explanation).put("topic", it.topic).put("source", it.source) }))
        put("attempts", JSONArray(dao.allAttempts().map { JSONObject().put("id", it.id).put("takenAt", it.takenAt).put("score", it.score).put("total", it.total) }))
        put("chapters", JSONArray(dao.allChapters().map { JSONObject().put("id", it.id).put("subjectId", it.subjectId).put("title", it.title) }))
        put("topics", JSONArray(dao.allTopics().map { JSONObject().put("id", it.id).put("chapterId", it.chapterId).put("title", it.title).put("status", it.status) }))
        put("slots", JSONArray(dao.allSlots().map {
            JSONObject().put("id", it.id).put("weekday", it.weekday).put("startMin", it.startMin).put("endMin", it.endMin).put("title", it.title).put("subjectId", it.subjectId) }))
    }.toString()

    private suspend fun restore(text: String) {
        val o = JSONObject(text)
        require(o.optString("app") == "StudyOS" && o.optInt("schema", 99) <= 4)
        fun <T> arr(k: String, f: (JSONObject) -> T): List<T> { val a = o.optJSONArray(k) ?: return emptyList(); return List(a.length()) { i -> f(a.getJSONObject(i)) } }
        val s = arr("subjects") { Subject(it.getLong("id"), it.getString("name"), it.getLong("color")) }
        val t = arr("tasks") {
            Task(it.getLong("id"), it.getString("title"), if (it.has("subjectId")) it.getLong("subjectId") else null,
                if (it.has("dueAt")) it.getLong("dueAt") else null, it.getInt("priority"), it.getBoolean("done"), it.optInt("recur", 0)) }
        val n = arr("notes") { Note(it.getLong("id"), it.getString("title"), it.getString("body"), it.getBoolean("pinned"), it.getLong("updatedAt"), it.optString("tags", ""), it.optString("folder", "")) }
        val f = arr("sessions") { FocusSession(it.getLong("id"), it.getLong("startedAt"), it.getInt("minutes")) }
        val dk = arr("decks") { Deck(it.getLong("id"), it.getString("title")) }
        val cd = arr("cards") {
            Flashcard(it.getLong("id"), it.getLong("deckId"), it.getString("front"), it.getString("back"), it.getLong("dueAt"),
                it.getInt("intervalDays"), it.getInt("ease"), it.getInt("reviews")) }
        val ex = arr("exams") { Exam(it.getLong("id"), it.getString("title"), it.getLong("examAt"), if (it.has("subjectId")) it.getLong("subjectId") else null) }
        val mk = arr("mocks") { MockTest(it.getLong("id"), it.getLong("examId"), it.getLong("takenAt"), it.getInt("score"), it.getInt("outOf")) }
        val qz = arr("questions") {
            Question(it.getLong("id"), it.getString("prompt"), it.getString("o1"), it.getString("o2"), it.getString("o3"), it.getString("o4"),
                it.getInt("correct"), it.getString("explanation"), it.optString("topic", ""), it.optString("source", "")) }
        val at = arr("attempts") { QuizAttempt(it.getLong("id"), it.getLong("takenAt"), it.getInt("score"), it.getInt("total")) }
        val ch = arr("chapters") { Chapter(it.getLong("id"), it.getLong("subjectId"), it.getString("title")) }
        val tp = arr("topics") { Topic(it.getLong("id"), it.getLong("chapterId"), it.getString("title"), it.getInt("status")) }
        val sl = arr("slots") {
            Slot(it.getLong("id"), it.getInt("weekday"), it.getInt("startMin"), it.getInt("endMin"), it.getString("title"),
                if (it.has("subjectId")) it.getLong("subjectId") else null) }
        db.withTransaction {
            clearAll()
            s.forEach { dao.addSubject(it) }; t.forEach { dao.addTask(it) }
            n.forEach { dao.saveNote(it) }; f.forEach { dao.addSession(it) }
            dk.forEach { dao.addDeck(it) }; cd.forEach { dao.saveCard(it) }; ex.forEach { dao.saveExam(it) }; mk.forEach { dao.addMock(it) }
            qz.forEach { dao.saveQuestion(it) }; at.forEach { dao.addAttempt(it) }; ch.forEach { dao.saveChapter(it) }
            tp.forEach { dao.saveTopic(it) }; sl.forEach { dao.saveSlot(it) }
        }
    }

    fun exportTo(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        toast.value = try {
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { it.write(exportJson().toByteArray()) }
            ds.edit { it[LAST_BACKUP] = System.currentTimeMillis() }
            "Backup saved"
        } catch (e: Exception) { "Backup failed. Nothing was changed." }
    }

    fun importFrom(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        toast.value = try {
            val bytes = getApplication<Application>().contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            require(bytes.size <= 20_000_000)
            restore(String(bytes))
            "Backup restored"
        } catch (e: Exception) { "This backup could not be opened. Your current data has not been changed." }
    }

    val questions = dao.questions().hot(emptyList())
    val attempts = dao.attempts().hot(emptyList())
    val chapters = dao.chapters().hot(emptyList())
    val topics = dao.topics().hot(emptyList())
    val slots = dao.slots().hot(emptyList())
    fun saveQuestion(q: Question) = viewModelScope.launch { dao.saveQuestion(q) }
    fun deleteQuestion(q: Question) = viewModelScope.launch { dao.deleteQuestion(q) }
    fun addAttempt(a: QuizAttempt) = viewModelScope.launch { dao.addAttempt(a) }
    fun saveChapter(c: Chapter) = viewModelScope.launch { dao.saveChapter(c) }
    fun deleteChapter(c: Chapter) = viewModelScope.launch { dao.deleteTopicsOfChapter(c.id); dao.deleteChapter(c.id) }
    fun saveTopic(t: Topic) = viewModelScope.launch { dao.saveTopic(t) }
    fun deleteTopic(t: Topic) = viewModelScope.launch { dao.deleteTopic(t) }
    fun saveSlot(s: Slot) = viewModelScope.launch { dao.saveSlot(s) }
    fun deleteSlot(s: Slot) = viewModelScope.launch { dao.deleteSlot(s) }
    fun setDone(t: Task, done: Boolean) = viewModelScope.launch {
        dao.updateTask(t.copy(done = done))
        if (done && t.recur > 0 && t.dueAt != null) dao.addTask(t.copy(id = 0, done = false, dueAt = t.dueAt + (if (t.recur == 1) 1L else 7L) * 86_400_000L))
    }
    val importing = MutableStateFlow(false)
    private var lastImport = 0L
    fun importTrivia(category: Int, difficulty: String, amount: Int) {
        if (importing.value) return
        if (System.currentTimeMillis() - lastImport < 6000) { toast.value = "Please wait a few seconds between imports."; return }
        importing.value = true
        lastImport = System.currentTimeMillis()
        viewModelScope.launch {
            val r = fetchTrivia(category, difficulty, amount)
            if (r.error != null) toast.value = r.error
            else {
                val have = dao.allQuestions().map { it.prompt }.toHashSet()
                val fresh = r.questions.filter { have.add(it.prompt) }
                fresh.forEach { dao.saveQuestion(it) }
                val skipped = r.questions.size - fresh.size
                toast.value = "Added ${fresh.size} new question${if (fresh.size == 1) "" else "s"}" + if (skipped > 0) " ($skipped duplicates skipped)" else ""
            }
            importing.value = false
        }
    }
    val decks = dao.decks().hot(emptyList())
    val cards = dao.cards().hot(emptyList())
    val exams = dao.exams().hot(emptyList())
    val mocks = dao.mocks().hot(emptyList())
    val onboarded = ds.data.map<Preferences, Boolean?> { it[K_ONBOARD] ?: false }.hot(null)
    val remOn = ds.data.map { it[K_REM_ON] ?: false }.hot(false)
    val remH = ds.data.map { it[K_REM_H] ?: 18 }.hot(18)
    val remM = ds.data.map { it[K_REM_M] ?: 0 }.hot(0)
    val focusRem = ds.data.map { it[F_REM] ?: 0L }.hot(0L)

    fun addDeck(title: String) = viewModelScope.launch { dao.addDeck(Deck(title = title)) }
    fun deleteDeck(d: Deck) = viewModelScope.launch { dao.deleteCardsOf(d.id); dao.deleteDeck(d.id) }
    fun saveCard(c: Flashcard) = viewModelScope.launch { dao.saveCard(c) }
    fun deleteCard(c: Flashcard) = viewModelScope.launch { dao.deleteCard(c) }
    fun review(c: Flashcard, grade: Int) = viewModelScope.launch { dao.saveCard(nextCard(c, grade, System.currentTimeMillis())) }
    fun saveExam(e: Exam) = viewModelScope.launch { dao.saveExam(e) }
    fun deleteExam(e: Exam) = viewModelScope.launch { dao.deleteMocksOf(e.id); dao.deleteExam(e.id) }
    fun addMock(m: MockTest) = viewModelScope.launch { dao.addMock(m) }
    fun deleteMock(m: MockTest) = viewModelScope.launch { dao.deleteMock(m) }

    fun setReminder(on: Boolean) = viewModelScope.launch {
        ds.edit { it[K_REM_ON] = on }
        val app = getApplication<Application>()
        if (on) Reminders.schedule(app, remH.value, remM.value) else Reminders.cancel(app)
    }
    fun setReminderTime(h: Int, m: Int) = viewModelScope.launch {
        ds.edit { it[K_REM_H] = h; it[K_REM_M] = m }
        if (remOn.value) Reminders.schedule(getApplication<Application>(), h, m)
    }

    fun startFocus(min: Int) = viewModelScope.launch {
        ds.edit { it[F_END] = System.currentTimeMillis() + min * 60000L; it[F_MIN] = min; it[F_REM] = 0L }
    }
    fun pauseFocus() = viewModelScope.launch {
        ds.edit {
            val e = it[F_END] ?: 0L
            if (e > 0) { it[F_REM] = (e - System.currentTimeMillis()).coerceAtLeast(1000L); it[F_END] = 0L }
        }
    }
    fun resumeFocus() = viewModelScope.launch {
        ds.edit {
            val r = it[F_REM] ?: 0L
            if (r > 0) { it[F_END] = System.currentTimeMillis() + r; it[F_REM] = 0L }
        }
    }
    fun stopFocus(doneMinutes: Int) = viewModelScope.launch {
        var was = false
        ds.edit { was = (it[F_END] ?: 0L) > 0 || (it[F_REM] ?: 0L) > 0; it[F_END] = 0L; it[F_REM] = 0L }
        if (was && doneMinutes > 0) dao.addSession(FocusSession(startedAt = System.currentTimeMillis(), minutes = doneMinutes))
    }
}
