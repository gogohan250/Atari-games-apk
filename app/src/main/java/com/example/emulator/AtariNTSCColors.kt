package com.example.emulator

/**
 * Authentic 128-color Atari 2600 NTSC & PAL TIA Palettes (ARGB8888).
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

    // Authentic 128-color PAL TIA Palette (grayscale on $0x/$1x/$Ex/$Fx, shifted hues for $2x..$Dx)
    val palPalette: IntArray = intArrayOf(
        // $0x grayscale
        0xFF000000.toInt(), 0xFF1A1A1A.toInt(), 0xFF393939.toInt(), 0xFF5B5B5B.toInt(),
        0xFF7E7E7E.toInt(), 0xFFA2A2A2.toInt(), 0xFFC7C7C7.toInt(), 0xFFEDEDED.toInt(),
        // $1x grayscale on PAL
        0xFF000000.toInt(), 0xFF1A1A1A.toInt(), 0xFF393939.toInt(), 0xFF5B5B5B.toInt(),
        0xFF7E7E7E.toInt(), 0xFFA2A2A2.toInt(), 0xFFC7C7C7.toInt(), 0xFFEDEDED.toInt(),
        // $2x gold / yellow-orange
        0xFF271700.toInt(), 0xFF483500.toInt(), 0xFF6B5600.toInt(), 0xFF8F7900.toInt(),
        0xFFB59D00.toInt(), 0xFFDBC21A.toInt(), 0xFFFFE83D.toInt(), 0xFFFFFF64.toInt(),
        // $3x yellow-green
        0xFF0D2100.toInt(), 0xFF294300.toInt(), 0xFF496600.toInt(), 0xFF6C8A07.toInt(),
        0xFF8FB024.toInt(), 0xFFB4D643.toInt(), 0xFFD9FC66.toInt(), 0xFFFFFF89.toInt(),
        // $4x orange / brown
        0xFF3A0400.toInt(), 0xFF611C00.toInt(), 0xFF863A00.toInt(), 0xFFAC5B0C.toInt(),
        0xFFD27D28.toInt(), 0xFFF8A148.toInt(), 0xFFFFC66B.toInt(), 0xFFFFEC90.toInt(),
        // $5x green
        0xFF002B00.toInt(), 0xFF004B00.toInt(), 0xFF126E0B.toInt(), 0xFF2D9323.toInt(),
        0xFF4DBA41.toInt(), 0xFF70E062.toInt(), 0xFF93FF85.toInt(), 0xFFB8FFA9.toInt(),
        // $6x red
        0xFF4A0004.toInt(), 0xFF750016.toInt(), 0xFF9A0E32.toInt(), 0xFFC02B52.toInt(),
        0xFFE64B73.toInt(), 0xFFFF6E97.toInt(), 0xFFFF92BA.toInt(), 0xFFFFB7DF.toInt(),
        // $7x cyan-green
        0xFF002B0D.toInt(), 0xFF004C2B.toInt(), 0xFF00704C.toInt(), 0xFF08956E.toInt(),
        0xFF25BB92.toInt(), 0xFF45E2B7.toInt(), 0xFF69FFDC.toInt(), 0xFF8FFFFF.toInt(),
        // $8x magenta / purple
        0xFF3B0036.toInt(), 0xFF62005D.toInt(), 0xFF870B84.toInt(), 0xFFAC27A8.toInt(),
        0xFFD148CE.toInt(), 0xFFF76AF3.toInt(), 0xFFFF8EFF.toInt(), 0xFFFFB4FF.toInt(),
        // $9x cyan
        0xFF002032.toInt(), 0xFF004057.toInt(), 0xFF00647D.toInt(), 0xFF0488A2.toInt(),
        0xFF20AEC7.toInt(), 0xFF41D4ED.toInt(), 0xFF65FBFF.toInt(), 0xFF8BFFFF.toInt(),
        // $Ax violet / blue-purple
        0xFF1E005A.toInt(), 0xFF3E008D.toInt(), 0xFF6010B5.toInt(), 0xFF832DD9.toInt(),
        0xFFA74EFB.toInt(), 0xFFCC70FF.toInt(), 0xFFF094FF.toInt(), 0xFFFFB9FF.toInt(),
        // $Bx light blue
        0xFF000D57.toInt(), 0xFF002988.toInt(), 0xFF054BB0.toInt(), 0xFF1D6FD4.toInt(),
        0xFF3D94F6.toInt(), 0xFF5FB9FF.toInt(), 0xFF84DFFF.toInt(), 0xFFA9FFFF.toInt(),
        // $Cx indigo / deep blue
        0xFF00006B.toInt(), 0xFF09069D.toInt(), 0xFF2520C5.toInt(), 0xFF443EE7.toInt(),
        0xFF6660FF.toInt(), 0xFF8A84FF.toInt(), 0xFFAFA9FF.toInt(), 0xFFD5D0FF.toInt(),
        // $Dx royal blue
        0xFF000069.toInt(), 0xFF00129B.toInt(), 0xFF0330C2.toInt(), 0xFF1A52E5.toInt(),
        0xFF3876FF.toInt(), 0xFF599BFF.toInt(), 0xFF7DC1FF.toInt(), 0xFFA2E7FF.toInt(),
        // $Ex grayscale on PAL
        0xFF000000.toInt(), 0xFF1A1A1A.toInt(), 0xFF393939.toInt(), 0xFF5B5B5B.toInt(),
        0xFF7E7E7E.toInt(), 0xFFA2A2A2.toInt(), 0xFFC7C7C7.toInt(), 0xFFEDEDED.toInt(),
        // $Fx grayscale on PAL
        0xFF000000.toInt(), 0xFF1A1A1A.toInt(), 0xFF393939.toInt(), 0xFF5B5B5B.toInt(),
        0xFF7E7E7E.toInt(), 0xFFA2A2A2.toInt(), 0xFFC7C7C7.toInt(), 0xFFEDEDED.toInt()
    )

    fun color(tiaByte: Int, isPal: Boolean = false): Int {
        val idx = (tiaByte ushr 1) and 0x7F
        return if (isPal) palPalette[idx] else palette[idx]
    }
}
