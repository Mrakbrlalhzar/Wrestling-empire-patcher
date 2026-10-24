package com.example.savetool

class SaveSession private constructor(
    val originalBytes: ByteArray,
    val document: SaveDocument
) {
    private val edits = LinkedHashMap<Int, StatEdit>()

    fun setStat(slot: StatSlot, newValue: Float) {
        edits[slot.offset] = StatEdit(slot, newValue)
    }

    fun setAllStatsOf(char: Character, newValue: Float) =
        char.stats.forEach { setStat(it, newValue) }

    fun setAllStatsOfEveryone(newValue: Float) =
        document.characters.forEach { c -> c.stats.forEach { setStat(it, newValue) } }

    fun currentValueOf(slot: StatSlot): Float =
        edits[slot.offset]?.newValue ?: slot.value

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
