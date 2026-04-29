package com.strakk.shared.data.pdf

internal data class ImageRef(val name: String, val width: Int, val height: Int)

internal class PdfWriter {

    companion object {
        const val PAGE_WIDTH = 595.28f
        const val PAGE_HEIGHT = 841.89f
        const val FONT_HELVETICA = "Helvetica"
        const val FONT_HELVETICA_BOLD = "Helvetica-Bold"

        private val WIN_ANSI_MAP: Map<Char, Int> = buildMap {
            for (cp in 0xA0..0xFF) put(cp.toChar(), cp)
            put('€', 0x80)
            put('Š', 0x8A); put('Œ', 0x8C); put('Ž', 0x8E)
            put('š', 0x9A); put('œ', 0x9C); put('ž', 0x9E); put('Ÿ', 0x9F)
            // Windows-1252 smart punctuation — iOS keyboard emits these
            put('‘', 0x91); put('’', 0x92)  // '' curly single quotes / apostrophe
            put('“', 0x93); put('”', 0x94)  // "" curly double quotes
            put('•', 0x95)                        // • bullet
            put('–', 0x96); put('—', 0x97)  // – en dash, — em dash
            put('…', 0x85)                        // … ellipsis
        }
    }

    private val pageStreams = mutableListOf<StringBuilder>()
    private var currentPage: StringBuilder? = null
    private var currentFontName = FONT_HELVETICA
    private var currentFontSize = 12f
    private var colorR = 0f
    private var colorG = 0f
    private var colorB = 0f

    private val fontResources = linkedMapOf(
        FONT_HELVETICA to "Helvetica",
        FONT_HELVETICA_BOLD to "Helvetica-Bold",
    )

    private class ImageEntry(
        val name: String,
        val jpegData: ByteArray,
        val width: Int,
        val height: Int,
    )

    private val registeredImages = mutableListOf<ImageEntry>()

    fun addPage() {
        val sb = StringBuilder()
        pageStreams.add(sb)
        currentPage = sb
        currentFontName = FONT_HELVETICA
        currentFontSize = 12f
        colorR = 0f; colorG = 0f; colorB = 0f
    }

    fun setFont(name: String, size: Float) {
        currentFontName = name
        currentFontSize = size
        stream("BT /F${fontIdx(name)} $size Tf ET")
    }

    fun setColor(r: Float, g: Float, b: Float) {
        colorR = r; colorG = g; colorB = b
        stream("${fmt(r)} ${fmt(g)} ${fmt(b)} rg")
        stream("${fmt(r)} ${fmt(g)} ${fmt(b)} RG")
    }

    fun drawText(x: Float, y: Float, text: String) {
        val enc = encodePdfString(text)
        stream("BT /F${fontIdx(currentFontName)} $currentFontSize Tf")
        stream("${fmt(colorR)} ${fmt(colorG)} ${fmt(colorB)} rg")
        stream("${fmt(x)} ${fmt(y)} Td ($enc) Tj ET")
    }

    fun drawTextRightAligned(x: Float, y: Float, text: String) {
        drawText(x - measureText(text, currentFontSize), y, text)
    }

    fun drawTextCentered(centerX: Float, y: Float, text: String) {
        drawText(centerX - measureText(text, currentFontSize) / 2f, y, text)
    }

    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, lineWidth: Float = 0.5f) {
        stream("${fmt(colorR)} ${fmt(colorG)} ${fmt(colorB)} RG")
        stream("$lineWidth w ${fmt(x1)} ${fmt(y1)} m ${fmt(x2)} ${fmt(y2)} l S")
    }

    fun fillRect(x: Float, y: Float, width: Float, height: Float) {
        stream("${fmt(colorR)} ${fmt(colorG)} ${fmt(colorB)} rg")
        stream("${fmt(x)} ${fmt(y)} ${fmt(width)} ${fmt(height)} re f")
    }

    fun addImage(jpegData: ByteArray): ImageRef? {
        val dims = parseJpegDimensions(jpegData) ?: return null
        val name = "Im${registeredImages.size + 1}"
        registeredImages.add(ImageEntry(name, jpegData, dims.first, dims.second))
        return ImageRef(name, dims.first, dims.second)
    }

    fun drawImage(x: Float, y: Float, width: Float, height: Float, imageName: String) {
        stream("q")
        stream("${fmt(width)} 0 0 ${fmt(height)} ${fmt(x)} ${fmt(y)} cm")
        stream("/$imageName Do")
        stream("Q")
    }

    fun measureText(text: String, fontSize: Float): Float =
        text.sumOf { (HELVETICA_WIDTHS[it] ?: 556).toDouble() }.toFloat() / 1000f * fontSize

    fun drawWrappedText(x: Float, y: Float, maxWidth: Float, text: String, lineHeight: Float): Float {
        val words = text.split(" ")
        var line = StringBuilder()
        var curY = y
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (measureText(candidate, currentFontSize) <= maxWidth) {
                line = StringBuilder(candidate)
            } else {
                if (line.isNotEmpty()) {
                    drawText(x, curY, line.toString())
                    curY -= lineHeight
                }
                line = StringBuilder(word)
            }
        }
        if (line.isNotEmpty()) {
            drawText(x, curY, line.toString())
            curY -= lineHeight
        }
        return curY
    }

    fun toByteArray(): ByteArray {
        val buf = BinaryBuffer()
        var nextId = 1
        val objs = mutableListOf<ObjRecord>()

        fun alloc(): Int {
            val id = nextId++
            objs.add(ObjRecord(id))
            return id
        }

        val catalogId = alloc()
        val pageTreeId = alloc()

        val streamIds = pageStreams.map { s ->
            val id = alloc()
            objs[id - 1] = ObjRecord(id, stream = s.toString().encodeToByteArray())
            id
        }

        val fontIds = fontResources.keys.associateWith { name ->
            val id = alloc()
            objs[id - 1] = ObjRecord(
                id,
                dict = "/Type /Font /Subtype /Type1 /BaseFont /$name /Encoding /WinAnsiEncoding",
            )
            id
        }

        val imgIds = registeredImages.associate { img ->
            val id = alloc()
            objs[id - 1] = ObjRecord(
                id,
                imageData = img.jpegData,
                imgWidth = img.width,
                imgHeight = img.height,
            )
            img.name to id
        }

        val pageIds = streamIds.map { sid ->
            val pid = alloc()
            val fRefs = fontIds.entries.joinToString(" ") { (n, i) -> "/F${fontIdx(n)} $i 0 R" }
            val xPart = if (imgIds.isEmpty()) {
                ""
            } else {
                " /XObject << ${imgIds.entries.joinToString(" ") { (n, i) -> "/$n $i 0 R" }} >>"
            }
            objs[pid - 1] = ObjRecord(
                pid,
                dict = "/Type /Page /Parent $pageTreeId 0 R " +
                    "/MediaBox [0 0 ${fmt(PAGE_WIDTH)} ${fmt(PAGE_HEIGHT)}] " +
                    "/Contents $sid 0 R " +
                    "/Resources << /Font << $fRefs >>$xPart >>",
            )
            pid
        }

        objs[pageTreeId - 1] = ObjRecord(
            pageTreeId,
            dict = "/Type /Pages /Kids [${pageIds.joinToString(" ") { "$it 0 R" }}] /Count ${pageIds.size}",
        )
        objs[catalogId - 1] = ObjRecord(catalogId, dict = "/Type /Catalog /Pages $pageTreeId 0 R")

        buf.text("%PDF-1.4\n")
        @Suppress("MagicNumber")
        buf.raw(byteArrayOf(0x25, 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), 0x0A))

        val offsets = mutableListOf<Int>()
        for (obj in objs) {
            offsets.add(buf.pos)
            when {
                obj.dict != null ->
                    buf.text("${obj.id} 0 obj\n<< ${obj.dict} >>\nendobj\n")

                obj.stream != null -> {
                    buf.text("${obj.id} 0 obj\n<< /Length ${obj.stream.size} >>\nstream\n")
                    buf.raw(obj.stream)
                    buf.text("\nendstream\nendobj\n")
                }

                obj.imageData != null -> {
                    buf.text("${obj.id} 0 obj\n<< /Type /XObject /Subtype /Image")
                    buf.text(" /Width ${obj.imgWidth} /Height ${obj.imgHeight}")
                    buf.text(" /ColorSpace /DeviceRGB /BitsPerComponent 8")
                    buf.text(" /Filter /DCTDecode /Length ${obj.imageData.size} >>\n")
                    buf.text("stream\n")
                    buf.raw(obj.imageData)
                    buf.text("\nendstream\nendobj\n")
                }

                else -> buf.text("${obj.id} 0 obj\nnull\nendobj\n")
            }
        }

        val xrefOff = buf.pos
        buf.text("xref\n0 ${objs.size + 1}\n0000000000 65535 f \n")
        for (off in offsets) {
            buf.text("${off.toString().padStart(10, '0')} 00000 n \n")
        }
        buf.text("trailer\n<< /Size ${objs.size + 1} /Root 1 0 R >>\n")
        buf.text("startxref\n$xrefOff\n%%EOF\n")

        return buf.toByteArray()
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun stream(line: String) {
        checkNotNull(currentPage) { "addPage() first" }.append(line).append('\n')
    }

    private fun fontIdx(name: String): Int =
        fontResources.keys.indexOfFirst { it == name } + 1

    private fun fmt(v: Float): String {
        val r = (v * 1000).toLong().toDouble() / 1000.0
        return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
    }

    private fun encodePdfString(text: String): String = buildString {
        for (ch in text) {
            val b = winAnsiByte(ch)
            when {
                b == '('.code || b == ')'.code || b == '\\'.code -> {
                    append('\\'); append(b.toChar())
                }
                b < 0x20 || b > 0x7E ->
                    append("\\${b.toString(8).padStart(3, '0')}")
                else -> append(b.toChar())
            }
        }
    }

    private fun winAnsiByte(ch: Char): Int =
        if (ch.code <= 0x7E) ch.code else (WIN_ANSI_MAP[ch] ?: '?'.code)

    @Suppress("MagicNumber")
    private fun parseJpegDimensions(data: ByteArray): Pair<Int, Int>? {
        if (data.size < 4 || data[0] != 0xFF.toByte() || data[1] != 0xD8.toByte()) return null
        var i = 2
        while (i < data.size - 9) {
            if (data[i] != 0xFF.toByte()) { i++; continue }
            val m = data[i + 1].toInt() and 0xFF
            if (m == 0xC0 || m == 0xC2) {
                val h = ((data[i + 5].toInt() and 0xFF) shl 8) or (data[i + 6].toInt() and 0xFF)
                val w = ((data[i + 7].toInt() and 0xFF) shl 8) or (data[i + 8].toInt() and 0xFF)
                return w to h
            }
            if (m == 0xD9) break
            if (m in 0xD0..0xD7 || m == 0x01) { i += 2; continue }
            if (i + 3 >= data.size) break
            val segLen = ((data[i + 2].toInt() and 0xFF) shl 8) or (data[i + 3].toInt() and 0xFF)
            i += 2 + segLen
        }
        return null
    }

    private class ObjRecord(
        val id: Int,
        val dict: String? = null,
        val stream: ByteArray? = null,
        val imageData: ByteArray? = null,
        val imgWidth: Int = 0,
        val imgHeight: Int = 0,
    )

    private class BinaryBuffer {
        private val chunks = mutableListOf<ByteArray>()
        var pos = 0; private set

        fun text(s: String) {
            val b = s.encodeToByteArray()
            chunks.add(b); pos += b.size
        }

        fun raw(data: ByteArray) {
            chunks.add(data); pos += data.size
        }

        fun toByteArray(): ByteArray {
            val r = ByteArray(pos); var o = 0
            for (c in chunks) { c.copyInto(r, o); o += c.size }
            return r
        }
    }

    @Suppress("MagicNumber")
    private val HELVETICA_WIDTHS: Map<Char, Int> = buildMap {
        put(' ', 278); put('!', 278); put('"', 355); put('#', 556)
        put('$', 556); put('%', 889); put('&', 667); put('\'', 191)
        put('(', 333); put(')', 333); put('*', 389); put('+', 584)
        put(',', 278); put('-', 333); put('.', 278); put('/', 278)
        put('0', 556); put('1', 556); put('2', 556); put('3', 556)
        put('4', 556); put('5', 556); put('6', 556); put('7', 556)
        put('8', 556); put('9', 556); put(':', 278); put(';', 278)
        put('<', 584); put('=', 584); put('>', 584); put('?', 556)
        put('@', 1015); put('A', 667); put('B', 667); put('C', 722)
        put('D', 722); put('E', 667); put('F', 611); put('G', 778)
        put('H', 722); put('I', 278); put('J', 500); put('K', 667)
        put('L', 556); put('M', 833); put('N', 722); put('O', 778)
        put('P', 667); put('Q', 778); put('R', 722); put('S', 667)
        put('T', 611); put('U', 722); put('V', 667); put('W', 944)
        put('X', 667); put('Y', 667); put('Z', 611)
        put('[', 278); put('\\', 278); put(']', 278); put('^', 469)
        put('_', 556); put('`', 333)
        put('a', 556); put('b', 556); put('c', 500); put('d', 556)
        put('e', 556); put('f', 278); put('g', 556); put('h', 556)
        put('i', 222); put('j', 222); put('k', 500); put('l', 222)
        put('m', 833); put('n', 556); put('o', 556); put('p', 556)
        put('q', 556); put('r', 333); put('s', 500); put('t', 278)
        put('u', 556); put('v', 500); put('w', 722); put('x', 500)
        put('y', 500); put('z', 500)
        put('{', 334); put('|', 260); put('}', 334); put('~', 584)
        put('À', 667); put('Â', 667); put('Ä', 667); put('Ç', 722)
        put('È', 667); put('É', 667); put('Ê', 667); put('Ë', 667)
        put('Î', 278); put('Ï', 278); put('Ô', 778); put('Ù', 722)
        put('Û', 722); put('Ü', 722)
        put('à', 556); put('â', 556); put('ä', 556); put('ç', 500)
        put('è', 556); put('é', 556); put('ê', 556); put('ë', 556)
        put('î', 222); put('ï', 222); put('ô', 556); put('ù', 556)
        put('û', 556); put('ü', 556)
        put('œ', 944); put('Œ', 1000)
        put('–', 556); put('—', 1000)
        put('’', 222)
    }
}
