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
    var session by mutableStateOf<SaveSession?>(null); private set
    var isLoading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var editsVersion by mutableStateOf(0); private set
    var selectedIndices by mutableStateOf<Set<Int>>(emptySet()); private set
    fun load(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                isLoading = true; error = null; session = null; selectedIndices = emptySet()
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw IOException("cannot open")
                }
                session = withContext(Dispatchers.IO) { SaveSession.load(bytes) }
                editsVersion = 0
            } catch (e: Exception) { error = e.message ?: "unknown" } finally { isLoading = false }
        }
    }
    fun toggleSelection(i: Int) {
        selectedIndices = if (i in selectedIndices) selectedIndices - i else selectedIndices + i
    }
    fun selectAllVisible(visible: Set<Int>) { selectedIndices = selectedIndices + visible }
    fun invertVisible(visible: Set<Int>) { selectedIndices = (selectedIndices - visible) + (visible - selectedIndices) }
    fun clearSelection() { selectedIndices = emptySet() }
    fun applyBulkChangeToSelected(change: BulkChange) {
        val s = session ?: return
        s.applyBulkChange(s.document.characters.filter { it.arrayIndex in selectedIndices }, change)
        editsVersion++
    }
    fun setAllToEveryone(v: Float) { session?.setAllStatsOfEveryone(v); editsVersion++ }
    fun setCharacterStats(c: Character, v: Float) { session?.setAllStatsOf(c, v); editsVersion++ }
    fun setValue(e: Editable, v: Float) { session?.setValue(e, v); editsVersion++ }
    fun currentValueOf(e: Editable): Float { @Suppress("UNUSED_EXPRESSION") editsVersion; return session?.currentValueOf(e) ?: e.value }
    fun editsCount(): Int { @Suppress("UNUSED_EXPRESSION") editsVersion; return session?.editsCount() ?: 0 }
    fun buildBytes(): ByteArray? = session?.buildPatchedBytes()
}
