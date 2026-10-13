package com.example.gallery.data

/**
 * เรียงชื่อแบบธรรมชาติ: ตัวเลขในชื่อเทียบเป็นค่าตัวเลข ไม่ใช่ทีละตัวอักษร
 * เช่น 1, 2, 10, 11, 21 (ไม่ใช่ 1, 10, 11, 2, 21) และไม่สนตัวพิมพ์เล็ก/ใหญ่
 */
object NaturalOrder : Comparator<String> {

    private fun Char.isAsciiDigit() = this in '0'..'9'

    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isAsciiDigit() && cb.isAsciiDigit()) {
                // ข้ามเลข 0 นำหน้า แล้วเทียบความยาวของเลข (ยาวกว่า = มากกว่า) ตามด้วยเทียบทีละหลัก
                var si = i
                while (si < a.length && a[si] == '0') si++
                var sj = j
                while (sj < b.length && b[sj] == '0') sj++
                var ei = si
                while (ei < a.length && a[ei].isAsciiDigit()) ei++
                var ej = sj
                while (ej < b.length && b[ej].isAsciiDigit()) ej++

                val lenA = ei - si
                val lenB = ej - sj
                if (lenA != lenB) return lenA.compareTo(lenB)
                for (k in 0 until lenA) {
                    val d = a[si + k].compareTo(b[sj + k])
                    if (d != 0) return d
                }
                // ค่าเท่ากัน: ตัวที่มี 0 นำหน้าน้อยกว่าไปก่อน
                val zerosA = si - i
                val zerosB = sj - j
                if (zerosA != zerosB) return zerosA.compareTo(zerosB)
                i = ei
                j = ej
            } else {
                val d = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (d != 0) return d
                i++
                j++
            }
        }
        return (a.length - i).compareTo(b.length - j)
    }
}
