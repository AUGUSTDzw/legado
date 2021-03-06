package io.legado.app.service.help

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.constant.Status
import io.legado.app.data.entities.Book
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.AudioPlayService
import io.legado.app.utils.postEvent

object AudioPlay {
    var titleData = MutableLiveData<String>()
    var coverData = MutableLiveData<String>()
    var status = Status.STOP
    var book: Book? = null
    var inBookshelf = false
    var chapterSize = 0
    var durChapterIndex = 0
    var durPageIndex = 0
    var webBook: WebBook? = null
    val loadingChapters = arrayListOf<Int>()

    fun headers(): Map<String, String>? {
        return webBook?.bookSource?.getHeaderMap()
    }

    fun play(context: Context) {
        val intent = Intent(context, AudioPlayService::class.java)
        intent.action = IntentAction.play
        context.startService(intent)
    }

    fun pause(context: Context) {
        if (AudioPlayService.isRun) {
            val intent = Intent(context, AudioPlayService::class.java)
            intent.action = IntentAction.pause
            context.startService(intent)
        }
    }

    fun resume(context: Context) {
        if (AudioPlayService.isRun) {
            val intent = Intent(context, AudioPlayService::class.java)
            intent.action = IntentAction.resume
            context.startService(intent)
        }
    }

    fun stop(context: Context) {
        if (AudioPlayService.isRun) {
            val intent = Intent(context, AudioPlayService::class.java)
            intent.action = IntentAction.stop
            context.startService(intent)
        }
    }

    fun adjustSpeed(context: Context, adjust: Float) {
        if (AudioPlayService.isRun) {
            val intent = Intent(context, AudioPlayService::class.java)
            intent.action = IntentAction.adjustSpeed
            intent.putExtra("adjust", adjust)
            context.startService(intent)
        }
    }

    fun adjustProgress(context: Context, position: Int) {
        if (AudioPlayService.isRun) {
            val intent = Intent(context, AudioPlayService::class.java)
            intent.action = IntentAction.adjustProgress
            intent.putExtra("position", position)
            context.startService(intent)
        }
    }

    fun skipTo(context: Context, index: Int) {
        Coroutine.async {
            book?.let { book ->
                durChapterIndex = index
                durPageIndex = 0
                book.durChapterIndex = durChapterIndex
                book.durChapterPos = 0
                saveRead()
                App.db.bookChapterDao().getChapter(book.bookUrl, durChapterIndex)?.let { chapter ->
                    postEvent(EventBus.AUDIO_SUB_TITLE, chapter.title)
                }
                play(context)
            }
        }
    }

    fun prev(context: Context) {
        Coroutine.async {
            book?.let { book ->
                if (book.durChapterIndex <= 0) {
                    return@let
                }
                durChapterIndex--
                durPageIndex = 0
                book.durChapterIndex = durChapterIndex
                book.durChapterPos = 0
                saveRead()
                App.db.bookChapterDao().getChapter(book.bookUrl, durChapterIndex)?.let { chapter ->
                    postEvent(EventBus.AUDIO_SUB_TITLE, chapter.title)
                }
                play(context)
            }
        }
    }

    fun next(context: Context) {
        Coroutine.async {
            book?.let { book ->
                if (book.durChapterIndex >= book.totalChapterNum) {
                    return@let
                }
                durChapterIndex++
                durPageIndex = 0
                book.durChapterIndex = durChapterIndex
                book.durChapterPos = 0
                saveRead()
                App.db.bookChapterDao().getChapter(book.bookUrl, durChapterIndex)?.let { chapter ->
                    postEvent(EventBus.AUDIO_SUB_TITLE, chapter.title)
                }
                play(context)
            }
        }
    }

    fun saveRead() {
        Coroutine.async {
            book?.let { book ->
                book.lastCheckCount = 0
                book.durChapterTime = System.currentTimeMillis()
                book.durChapterIndex = durChapterIndex
                book.durChapterPos = durPageIndex
                App.db.bookChapterDao().getChapter(book.bookUrl, book.durChapterIndex)?.let {
                    book.durChapterTitle = it.title
                }
                App.db.bookDao().update(book)
            }
        }
    }

}