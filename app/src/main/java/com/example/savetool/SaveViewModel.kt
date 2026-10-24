package com.example.savetool

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class SaveViewModel : ViewModel() {

    var session by mutableStateOf<SaveSession?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** برای trigger کردن recomposition وقتی edits تغییر می‌کنه */
    var editsVersion by mutableStateOf(0)
        private set

    fun load(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                isLoading = true
                error = null
                session = null

                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IOException("نمی‌توان فایل را باز کرد")
                }
                val loaded = withContext(Dispatchers.IO) { SaveSession.load(bytes) }

                session = loaded
                editsVersion = 0
            } catch (e: Exception) {
                error = e.message ?: "خطای نامشخص"
            } finally {
                isLoading = false
            }
        }
    }

    fun reset() {
        session = null
        error = null
        editsVersion = 0
    }

    fun setAllToEveryone(value: Float) {
        session?.setAllStatsOfEveryone(value)
        editsVersion++
    }

    fun setCharacterStats(char: Character, value: Float) {
        session?.setAllStatsOf(char, value)
        editsVersion++
    }

    fun setStat(slot: StatSlot, value: Float) {
        session?.setStat(slot, value)
        editsVersion++
    }

    fun currentValueOf(slot: StatSlot): Float {
        @Suppress("UNUSED_EXPRESSION")
        editsVersion
        return session?.currentValueOf(slot) ?: slot.value
    }

    fun editsCount(): Int {
        @Suppress("UNUSED_EXPRESSION")
        editsVersion
        return session?.editsCount() ?: 0
    }

    fun clearEdits() {
        session?.clearEdits()
        editsVersion++
    }

    fun buildBytes(): ByteArray? = session?.buildPatchedBytes()
}
