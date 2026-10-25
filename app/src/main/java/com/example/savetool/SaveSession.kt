package com.example.savetool

class SaveSession private constructor(
    val originalBytes: ByteArray,
    val document: SaveDocument
) {
    private val edits = LinkedHashMap<Int, StatEdit>()

    fun setValue(e: Editable, newValue: Float) {
        edits[e.offset] = StatEdit(e.offset, e.length, e.type, newValue)
    }

    fun setAllStatsOf(char: Character, newValue: Float) =
        char.stats.forEach { setValue(it, newValue) }

    fun setAllStatsOfEveryone(newValue: Float) =
        document.characters.forEach { c -> c.stats.forEach { setValue(it, newValue) } }

    fun currentValueOf(e: Editable): Float =
        edits[e.offset]?.newValue ?: e.value

    fun editsCount(): Int = edits.size

    fun clearEdits() = edits.clear()

    fun buildPatchedBytes(): ByteArray =
        SavePatcher.apply(originalBytes, edits.values.toList())

    companion object {
        fun load(saveBytes: ByteArray): SaveSession {
            val parser = SaveParser(saveBytes)
            val root = parser.parse()
            val doc = parser.extract(root)
            return SaveSession(saveBytes, doc)
        }
    }
}
