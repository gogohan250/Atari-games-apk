package com.example.emulator

/**
 * Authentic 128-color Atari 2600 NTSC TIA Palette (ARGB8888).
 * Index by (tiaColorByte ushr 1) and 0x7F.
 */
object AtariNTSCColors {
    val palette: IntArray = intArrayOf(
        0xFF000000.toInt(), 0xFF1A1A1A.toInt(), 0xFF393939.toInt(), 0xFF5B5B5B.toInt(),
        0xFF7E7E7E.toInt(), 0xFFA2A2A2.toInt(), 0xFFC7C7C7.toInt(), 0xFFEDEDED.toInt(),
        0xFF190200.toInt(), 0xFF3A1F00.toInt(), 0xFF5D4100.toInt(), 0xFF826400.toInt(),
        0xFFA78800.toInt(), 0xFFCCAD00.toInt(), 0xFFF2D219.toInt(), 0xFFFFFA40.toInt(),
        0xFF370000.toInt(), 0xFF5E0800.toInt(), 0xFF832700.toInt(), 0xFFA94900.toInt(),
        0xFFCF6C00.toInt(), 0xFFF58F17.toInt(), 0xFFFFB439.toInt(), 0xFFFFDF60.toInt(),
        0xFF470000.toInt(), 0xFF730000.toInt(), 0xFF981300.toInt(), 0xFFBE3216.toInt(),
        0xFFE45335.toInt(), 0xFFFF7657.toInt(), 0xFFFF9A7A.toInt(), 0xFFFFC19F.toInt(),
        0xFF440008.toInt(), 0xFF6F001F.toInt(), 0xFF960640.toInt(), 0xFFBB2462.toInt(),
        0xFFE14585.toInt(), 0xFFFF67A8.toInt(), 0xFFFF8BCB.toInt(), 0xFFFFB1EE.toInt(),
        0xFF300030.toInt(), 0xFF57005C.toInt(), 0xFF7D0584.toInt(), 0xFFA122A8.toInt(),
        0xFFC743CC.toInt(), 0xFFED65F0.toInt(), 0xFFFF88FF.toInt(), 0xFFFFAEFF.toInt(),
        0xFF140056.toInt(), 0xFF320089.toInt(), 0xFF530FB0.toInt(), 0xFF752BD3.toInt(),
        0xFF994BF5.toInt(), 0xFFBE6DFF.toInt(), 0xFFE390FF.toInt(), 0xFFFFB5FF.toInt(),
        0xFF00006B.toInt(), 0xFF0B079D.toInt(), 0xFF2822C4.toInt(), 0xFF4842E6.toInt(),
        0xFF6963FF.toInt(), 0xFF8D87FF.toInt(), 0xFFB2ACFF.toInt(), 0xFFD8D2FF.toInt(),
        0xFF000067.toInt(), 0xFF001A94.toInt(), 0xFF073ABB.toInt(), 0xFF215CDD.toInt(),
        0xFF4080FF.toInt(), 0xFF63A5FF.toInt(), 0xFF87CBFF.toInt(), 0xFFADF1FF.toInt(),
        0xFF000E4B.toInt(), 0xFF002F73.toInt(), 0xFF005199.toInt(), 0xFF0975BD.toInt(),
        0xFF259AE0.toInt(), 0xFF46C0FF.toInt(), 0xFF6AE6FF.toInt(), 0xFF90FFFF.toInt(),
        0xFF002122.toInt(), 0xFF004041.toInt(), 0xFF006464.toInt(), 0xFF038988.toInt(),
        0xFF1CAFAE.toInt(), 0xFF3DD5D3.toInt(), 0xFF60FBF8.toInt(), 0xFF86FFFF.toInt(),
        0xFF002A00.toInt(), 0xFF004B0F.toInt(), 0xFF006F2C.toInt(), 0xFF0E944D.toInt(),
        0xFF28BA70.toInt(), 0xFF49E195.toInt(), 0xFF6CFFB9.toInt(), 0xFF92FFDF.toInt(),
        0xFF002800.toInt(), 0xFF004800.toInt(), 0xFF0D6C02.toInt(), 0xFF279118.toInt(),
        0xFF47B837.toInt(), 0xFF69DF57.toInt(), 0xFF8DFF79.toInt(), 0xFFB4FF9D.toInt(),
        0xFF001C00.toInt(), 0xFF153B00.toInt(), 0xFF335E00.toInt(), 0xFF548200.toInt(),
        0xFF76A810.toInt(), 0xFF9ACE2E.toInt(), 0xFFBFF54F.toInt(), 0xFFE6FF72.toInt(),
        0xFF0B0A00.toInt(), 0xFF2A2600.toInt(), 0xFF4C4800.toInt(), 0xFF6F6C00.toInt(),
        0xFF949000.toInt(), 0xFFB9B514.toInt(), 0xFFDFDC35.toInt(), 0xFFFFFF59.toInt(),
        0xFF210000.toInt(), 0xFF441200.toInt(), 0xFF673300.toInt(), 0xFF8C5500.toInt(),
        0xFFB17800.toInt(), 0xFFD79C0C.toInt(), 0xFFFDC22D.toInt(), 0xFFFFEA51.toInt()
    )

    fun color(tiaByte: Int): Int = palette[(tiaByte ushr 1) and 0x7F]
}
