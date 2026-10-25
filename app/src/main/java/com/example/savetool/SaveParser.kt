package com.example.savetool
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
class SaveParser(private val bytes: ByteArray) {
    private val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    private val objectIndex = HashMap<Int, BinValue>()
    private var rootId = -1
    private val pendingRefs = mutableListOf<BinValue.Ref>()
    fun parse(): BinValue.Obj {
        while (buf.hasRemaining()) parseRecord()
        for (r in pendingRefs) r.resolved = objectIndex[r.id]
        return objectIndex[rootId] as? BinValue.Obj ?: error("Root (id=$rootId) not found")
    }
    private fun unwrap(v: BinValue?): BinValue? = when (v) {
        null -> null; is BinValue.Ref -> unwrap(v.resolved); else -> v
    }
    private fun readUByte(): Int = buf.get().toInt() and 0xFF
    private fun readSByte(): Int = buf.get().toInt()
    private fun readInt32(): Int = buf.getInt()
    private fun readString(len: Int): String {
        val off = buf.position(); buf.position(off + len)
        return String(bytes, off, len, StandardCharsets.UTF_8)
    }
    private fun readLeaf(t: PrimTypeEnum): BinValue.Leaf {
        val off = buf.position()
        val raw: Any? = when (t) {
            PrimTypeEnum.Boolean -> readUByte() != 0
            PrimTypeEnum.Byte -> readSByte()
            PrimTypeEnum.Char -> readUByte().toChar()
            PrimTypeEnum.Double -> buf.getDouble()
            PrimTypeEnum.Int16 -> buf.getShort().toInt()
            PrimTypeEnum.Int32 -> buf.getInt()
            PrimTypeEnum.Int64 -> buf.getLong()
            PrimTypeEnum.SByte -> buf.get().toInt()
            PrimTypeEnum.Single -> buf.getFloat()
            PrimTypeEnum.UInt16 -> readUByte() or (readUByte() shl 8)
            PrimTypeEnum.UInt32 -> buf.getInt().toLong() and 0xFFFF_FFFFL
            PrimTypeEnum.UInt64 -> buf.getLong()
            else -> error("Unsupported leaf: $t")
        }
        val len = buf.position() - off
        val special = when {
            raw is Float && raw.isNaN() -> "NaN"
            raw is Float && raw.isInfinite() -> if (raw > 0) "Infinity" else "-Infinity"
            raw is Double && raw.isNaN() -> "NaN"
            raw is Double && raw.isInfinite() -> if (raw > 0) "Infinity" else "-Infinity"
            else -> null
        }
        return BinValue.Leaf(off, if (special != null) null else raw, len, t, special)
    }
    private fun readMemberValue(mt: MemberType): BinValue = when (mt.binaryType) {
        BinTypeEnum.Primitive -> readLeaf(mt.primitiveType!!)
        BinTypeEnum.String, BinTypeEnum.Class, BinTypeEnum.SystemClass, BinTypeEnum.Object,
        BinTypeEnum.PrimitiveArray, BinTypeEnum.StringArray -> parseRecord()
        BinTypeEnum.ObjectArray -> error("ObjectArray not impl")
    }
    private fun parseRecord(): BinValue {
        val t = readUByte()
        return when (t) {
            0 -> parseSerializationHeader(); 1 -> parseClassWithId()
            4 -> parseClassWithMembersAndTypes(true); 5 -> parseClassWithMembersAndTypes(false)
            6 -> parseBinaryObjectString(); 7 -> parseBinaryArray()
            9 -> parseMemberReference(); 10 -> BinValue.NullMultiple(1)
            11 -> BinValue.None; 12 -> parseBinaryLibrary()
            13 -> BinValue.NullMultiple(readUByte()); 14 -> BinValue.NullMultiple(readInt32())
            15 -> parseArraySinglePrimitive(); 17 -> parseArraySingleString()
            else -> error("Unknown record 0x${t.toString(16)} at ${buf.position()-1}")
        }
    }
    private fun parseSerializationHeader(): BinValue {
        rootId = readInt32(); readInt32(); readInt32(); readInt32(); return BinValue.None
    }
    private fun parseBinaryLibrary(): BinValue { readInt32(); readString(readUByte()); return BinValue.None }
    private fun parseMemberReference(): BinValue.Ref {
        val id = readInt32(); return BinValue.Ref(id).also { pendingRefs += it }
    }
    private fun parseBinaryObjectString(): BinValue.Str {
        val id = readInt32(); val len = readUByte(); val off = buf.position()
        val s = BinValue.Str(off, readString(len), len); objectIndex[id] = s; return s
    }
    private fun parseClassWithMembersAndTypes(isSystem: Boolean): BinValue.Obj {
        val objId = readInt32(); val className = readString(readUByte())
        val mc = readInt32(); val mn = List(mc) { readString(readUByte()) }
        val btEnums = List(mc) { BinTypeEnum.fromRaw(readUByte()) }
        val ptEnums = List(mc) { i ->
            when (btEnums[i]) {
                BinTypeEnum.Primitive, BinTypeEnum.PrimitiveArray -> PrimTypeEnum.fromRaw(readUByte())
                BinTypeEnum.SystemClass -> { readString(readUByte()); null }
                BinTypeEnum.Class -> { readString(readUByte()); readInt32(); null }
                else -> null
            }
        }
        if (!isSystem) readInt32()
        val mts = List(mc) { i -> MemberType(btEnums[i], ptEnums[i]) }
        val members = List(mc) { readMemberValue(mts[it]) }
        return BinValue.Obj(className, mn, mts, members).also { objectIndex[objId] = it }
    }
    private fun parseClassWithId(): BinValue.Obj {
        val objId = readInt32(); val metaId = readInt32()
        val existing = objectIndex[metaId] as? BinValue.Obj ?: error("ClassWithId: meta $metaId missing")
        val members = List(existing.memberTypes.size) { readMemberValue(existing.memberTypes[it]) }
        return BinValue.Obj(existing.className, existing.memberNames, existing.memberTypes, members)
            .also { objectIndex[objId] = it }
    }
    private fun parseBinaryArray(): BinValue.Arr {
        val objId = readInt32(); val arrayType = readUByte(); val rank = readInt32()
        val lengths = List(rank) { readInt32() }
        if (arrayType in 3..5) List(rank) { readInt32() }
        val typeEnum = BinTypeEnum.fromRaw(readUByte())
        val prim: PrimTypeEnum? = when (typeEnum) {
            BinTypeEnum.Primitive, BinTypeEnum.PrimitiveArray -> PrimTypeEnum.fromRaw(readUByte())
            BinTypeEnum.SystemClass -> { readString(readUByte()); null }
            BinTypeEnum.Class -> { readString(readUByte()); readInt32(); null }
            else -> null
        }
        val total = lengths.fold(1) { a, b -> a * b }
        val mt = MemberType(typeEnum, prim)
        val items = mutableListOf<BinValue>()
        while (items.size < total) {
            val v = readMemberValue(mt)
            if (v is BinValue.NullMultiple && v.count > 1) {
                repeat(v.count) { if (items.size < total) items.add(BinValue.NullMultiple(1)) }
            } else items.add(v)
        }
        return BinValue.Arr(items, prim).also { objectIndex[objId] = it }
    }
    private fun parseArraySinglePrimitive(): BinValue.Arr {
        val objId = readInt32(); val len = readInt32(); val pt = PrimTypeEnum.fromRaw(readUByte())
        val items = List(len) { readLeaf(pt) }
        return BinValue.Arr(items, pt).also { objectIndex[objId] = it }
    }
    private fun parseArraySingleString(): BinValue.Arr {
        val objId = readInt32(); val len = readInt32()
        val items = mutableListOf<BinValue>()
        while (items.size < len) {
            val v = parseRecord()
            if (v is BinValue.NullMultiple && v.count > 1) {
                repeat(v.count) { if (items.size < len) items.add(BinValue.NullMultiple(1)) }
            } else items.add(v)
        }
        return BinValue.Arr(items, PrimTypeEnum.String).also { objectIndex[objId] = it }
    }
    fun extract(doc: BinValue.Obj): SaveDocument {
        val scv = unwrap(doc.member("savedChars")) as? BinValue.Arr
            ?: error("savedChars not Arr. class=${doc.className}, members=${doc.memberNames}")
        val chars = mutableListOf<Character>()
        scv.items.forEachIndexed { idx, item ->
            val obj = unwrap(item) as? BinValue.Obj
            if (obj != null) chars.add(buildCharacter(idx, obj))
        }
        return SaveDocument(doc.className, chars)
    }
    private fun buildCharacter(arrayIndex: Int, obj: BinValue.Obj): Character {
        val name = ((unwrap(obj.member("name")) as? BinValue.Str)?.value) ?: ""
        val statsArr = unwrap(obj.member("stat")) as? BinValue.Arr
        val stats = statsArr?.items?.mapIndexedNotNull { i, v ->
            (unwrap(v) as? BinValue.Leaf)?.let {
                StatSlot(i, it.offset, it.length, it.type, (it.value as? Number)?.toFloat() ?: 0f, it.special)
            }
        } ?: emptyList()
        val extraFieldNames = listOf("age", "height", "armMass", "muscleMass", "bodyMass", "legMass")
        val extraFields = extraFieldNames.mapNotNull { fn ->
            val leaf = unwrap(obj.member(fn)) as? BinValue.Leaf ?: return@mapNotNull null
            EditableLeaf(fn, leaf.offset, leaf.length, leaf.type, (leaf.value as? Number)?.toFloat() ?: 0f, leaf.special)
        }
        return Character(arrayIndex, name, stats, extraFields, obj.memberNames.zip(obj.members).toMap())
    }
}
