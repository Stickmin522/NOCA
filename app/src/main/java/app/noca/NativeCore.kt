package app.noca

object NativeCore {
    init {
        System.loadLibrary("noca")
    }

    external fun buildRead(idm: ByteArray, service: Int, start: Int, count: Int): ByteArray

    external fun readResponse(idm: ByteArray, response: ByteArray): ByteArray

    external fun decodeHistory(blocks: ByteArray): String

    external fun attributeBalance(block: ByteArray): Int
}
