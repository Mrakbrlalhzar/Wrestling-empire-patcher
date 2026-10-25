package com.example.savetool

sealed class BinValue {
    data class Leaf(
        val offset: Int,
        val value: Any?,
        val length: Int,
        val type: PrimTypeEnum,
        val special: String? = null
    ) : BinValue()

    data class Str(val offset: Int, val value: String, val length: Int) : BinValue()

    data class Obj(
        val className: String,
        val memberNames: List<String>,
        val memberTypes: List<MemberType>,
        val members: List<BinValue>
    ) : BinValue()

    data class Arr(val items: List<BinValue>, val elementType: PrimTypeEnum? = null) : BinValue()

    class Ref(val id: Int) : BinValue() {
        var resolved: BinValue? = null
    }

    data class NullMultiple(val count: Int) : BinValue()

    object None : BinValue()
}

data class MemberType(val binaryType: BinTypeEnum, val primitiveType: PrimTypeEnum? = null)

enum class BinTypeEnum(val raw: Int) {
    Primitive(0), String(1), Object(2), SystemClass(3), Class(4),
    ObjectArray(5), StringArray(6), PrimitiveArray(7);
    companion object {
        fun fromRaw(b: Int) = entries.firstOrNull { it.raw == b }
            ?: error("Unknown BinTypeEnum: $b")
    }
}

enum class PrimTypeEnum(val raw: Int, val size: Int) {
    Boolean(1, 1), Byte(2, 1), Char(3, 1), Double(6, 8), Int16(7, 2),
    Int32(8, 4), Int64(9, 8), SByte(10, 1), Single(11, 4),
    UInt16(14, 2), UInt32(15, 4), UInt64(16, 8), Null(17, 0), String(18, -1);
    companion object {
        fun fromRaw(b: Int) = entries.firstOrNull { it.raw == b }
            ?: error("Unknown PrimTypeEnum: $b")
    }
}

interface Editable {
    val offset: Int
    val length: Int
    val type: PrimTypeEnum
    val value: Float
    val special: String?
}

data class StatSlot(
    val index: Int,
    override val offset: Int,
    override val length: Int,
    override val type: PrimTypeEnum,
    override val value: Float,
    override val special: String? = null
) : Editable

data class EditableLeaf(
    val name: String,
    override val offset: Int,
    override val length: Int,
    override val type: PrimTypeEnum,
    override val value: Float,
    override val special: String? = null
) : Editable

data class Character(
    val arrayIndex: Int,
    val name: String,
    val stats: List<StatSlot>,
    val extraFields: List<EditableLeaf>,
    val fields: Map<String, BinValue>
)

data class SaveDocument(
    val rootClassName: String,
    val characters: List<Character>
)

fun BinValue.Obj.member(name: String): BinValue? =
    memberNames.indexOf(name).takeIf { it >= 0 }?.let { members[it] }

fun formatValue(value: Float, type: PrimTypeEnum): String = when (type) {
    PrimTypeEnum.Int32, PrimTypeEnum.Int64, PrimTypeEnum.Int16,
    PrimTypeEnum.UInt32, PrimTypeEnum.UInt64, PrimTypeEnum.UInt16,
    PrimTypeEnum.Byte, PrimTypeEnum.SByte -> value.toInt().toString()
    PrimTypeEnum.Boolean -> if (value != 0f) "true" else "false"
    else -> "%.2f".format(value)
}
