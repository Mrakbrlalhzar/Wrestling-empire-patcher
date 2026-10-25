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
                        ?: throw IOException("Cannot open file")
                }
                val loaded = withContext(Dispatchers.IO) { SaveSession.load(bytes) }
                session = loaded
                editsVersion = 0
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            } finally {
                isLoading = false
            }
        }
    }

    fun setCharacterStats(char: Character, value: Float) {
        session?.setAllStatsOf(char, value)
        editsVersion++
    }

    fun setValue(e: Editable, value: Float) {
        session?.setValue(e, value)
        editsVersion++
    }

    fun applyBulkChange(chars: List<Character>, change: BulkChange) {
        session?.applyBulkChange(chars, change)
        editsVersion++
    }

    fun currentValueOf(e: Editable): Float {
        @Suppress("UNUSED_EXPRESSION")
        editsVersion
        return session?.currentValueOf(e) ?: e.value
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
