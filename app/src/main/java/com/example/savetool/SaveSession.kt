package com.example.savetool
data class BulkChange(val statsValue: Float?, val extraFields: Map<String, Float>)
class SaveSession private constructor(val originalBytes: ByteArray, val document: SaveDocument) {
    private val edits = LinkedHashMap<Int, StatEdit>()
    fun setValue(e: Editable, newValue: Float) { edits[e.offset] = StatEdit(e.offset, e.length, e.type, newValue) }
    fun setAllStatsOf(char: Character, newValue: Float) = char.stats.forEach { setValue(it, newValue) }
    fun setAllStatsOfEveryone(newValue: Float) = document.characters.forEach { c -> c.stats.forEach { setValue(it, newValue) } }
    fun applyBulkChange(chars: List<Character>, change: BulkChange) {
        for (c in chars) {
            change.statsValue?.let { v -> c.stats.forEach { setValue(it, v) } }
            for (f in c.extraFields) change.extraFields[f.name]?.let { v -> setValue(f, v) }
        }
    }
    fun currentValueOf(e: Editable): Float = edits[e.offset]?.newValue ?: e.value
    fun editsCount(): Int = edits.size
    fun clearEdits() = edits.clear()
    fun buildPatchedBytes(): ByteArray = SavePatcher.apply(originalBytes, edits.values.toList())
    companion object {
        fun load(saveBytes: ByteArray): SaveSession {
            val p = SaveParser(saveBytes); val r = p.parse(); val d = p.extract(r); return SaveSession(saveBytes, d)
        }
    }
}
