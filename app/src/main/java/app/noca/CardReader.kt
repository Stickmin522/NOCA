package app.noca

import android.nfc.Tag
import android.nfc.tech.NfcF
import java.io.ByteArrayOutputStream
import java.io.IOException

class ReadFailure(val key: String) : IOException(key)

class CardReader {
    fun read(tag: Tag): CardSnapshot {
        val tech = NfcF.get(tag) ?: throw ReadFailure("unsupported_card")
        tech.use {
            it.connect()
            it.timeout = 1800
            // Poll specifically for the Japanese transit system. Never use service 0xFFFF
            // from a discovery response for reading an unrelated card system.
            val polling = it.transceive(byteArrayOf(6, 0, 0, 3, 1, 0))
            if (
                polling.size != 20 ||
                    (polling[0].toInt() and 255) != polling.size ||
                    polling[1].toInt() != 1
            )
                throw ReadFailure("unsupported_card")
            val idm = polling.copyOfRange(2, 10)
            if (!(polling[18].toInt() == 0 && polling[19].toInt() == 3))
                throw ReadFailure("unsupported_card")
            fun blocks(service: Int, start: Int, count: Int) =
                NativeCore.readResponse(
                    idm,
                    it.transceive(NativeCore.buildRead(idm, service, start, count)),
                )
            var balance: Int? =
                try {
                    NativeCore.attributeBalance(blocks(0x008b, 0, 1))
                } catch (_: IllegalArgumentException) {
                    null
                }
            val history = ByteArrayOutputStream()
            var partial = false
            // Small requests fit conservative handset transceive limits. A service status
            // can mean fewer supported blocks; retry individually, never silently fabricate.
            for (start in 0 until 20 step 4) {
                try {
                    history.write(blocks(0x090f, start, 4))
                } catch (_: IllegalArgumentException) {
                    for (index in start until start + 4) {
                        try {
                            history.write(blocks(0x090f, index, 1))
                        } catch (_: IllegalArgumentException) {
                            partial = true
                            break
                        }
                    }
                    break
                }
            }
            val rawHistory = history.toByteArray()
            val records =
                try {
                    Transaction.parse(NativeCore.decodeHistory(rawHistory))
                } catch (_: IllegalArgumentException) {
                    partial = true
                    val valid = ByteArrayOutputStream()
                    for (offset in rawHistory.indices step 16) {
                        val block = rawHistory.copyOfRange(offset, offset + 16)
                        if (runCatching { NativeCore.decodeHistory(block) }.isSuccess)
                            valid.write(block)
                    }
                    Transaction.parse(NativeCore.decodeHistory(valid.toByteArray()))
                }
            if (balance == null) balance = records.firstOrNull()?.balance
            if (balance == null || balance < 0) throw ReadFailure("unsupported_card")
            val id = idm.joinToString("") { b -> "%02x".format(b.toInt() and 255) }
            return CardSnapshot(id, balance, System.currentTimeMillis(), records, partial)
        }
    }
}
