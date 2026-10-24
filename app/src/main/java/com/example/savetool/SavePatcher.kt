package com.example.savetool

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class StatEdit(val slot: StatSlot, val newValue: Float)

object SavePatcher {
    fun apply(original: ByteArray, edits: List<StatEdit>): ByteArray {
        if (edits.isEmpty()) return original.copyOf()
        val out = original.copyOf()
        val buf = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)

        for (e in edits) {
            val off = e.slot.offset
            require(off >= 0 && off + e.slot.length <= out.size) {
                "Offset out of range: 0x${off.toString(16)}"
            }
            buf.position(off)
            when (e.slot.type) {
                PrimTypeEnum.Single -> buf.putFloat(e.newValue)
                PrimTypeEnum.Double -> buf.putDouble(e.newValue.toDouble())
                PrimTypeEnum.Int32  -> buf.putInt(e.newValue.toInt())
                PrimTypeEnum.Int64  -> buf.putLong(e.newValue.toLong())
                PrimTypeEnum.Int16  -> buf.putShort(e.newValue.toInt().toShort())
                PrimTypeEnum.UInt16 -> buf.putShort(e.newValue.toInt().toShort())
                PrimTypeEnum.UInt32 -> buf.putInt(e.newValue.toLong().toInt())
                PrimTypeEnum.UInt64 -> buf.putLong(e.newValue.toLong())
                PrimTypeEnum.Byte   -> buf.put(e.newValue.toInt().toByte())
                PrimTypeEnum.SByte  -> buf.put(e.newValue.toInt().toByte())
                PrimTypeEnum.Boolean-> buf.put(if (e.newValue != 0f) 1 else 0)
                else -> error("Unsupported patch type: ${e.slot.type}")
            }
        }
        return out
    }
}
