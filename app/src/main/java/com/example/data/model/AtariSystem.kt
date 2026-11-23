package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class AtariSystem(
    val displayName: String,
    val shortName: String,
    val libretroCore: String,
    val coreDescriptionAr: String,
    val extensions: List<String>,
    val accentHex: Long,
    val defaultAspectRatio: Float
) {
    ATARI_2600(
        displayName = "Atari 2600",
        shortName = "2600",
        libretroCore = "Stella (libretro)",
        coreDescriptionAr = "نواة Stella لتشغيل ألعاب Atari 2600 الكلاسيكية بسرعة 60fps",
        extensions = listOf("a26", "bin"),
        accentHex = 0xFFFF5A1F,
        defaultAspectRatio = 4f / 3f
    ),
    ATARI_5200(
        displayName = "Atari 5200",
        shortName = "5200",
        libretroCore = "Atari800 (libretro)",
        coreDescriptionAr = "نواة Atari800 لأنظمة 5200 وحواسيب Atari 8-bit",
        extensions = listOf("a52"),
        accentHex = 0xFF0088CC,
        defaultAspectRatio = 4f / 3f
    ),
    ATARI_800(
        displayName = "Atari 800 / XL",
        shortName = "800",
        libretroCore = "Atari800 (libretro)",
        coreDescriptionAr = "نواة Atari800 لألعاب وأشرطة Atari 800/XL/XE",
        extensions = listOf("atr", "xex", "car", "rom"),
        accentHex = 0xFF7C3AED,
        defaultAspectRatio = 4f / 3f
    ),
    ATARI_7800(
        displayName = "Atari 7800",
        shortName = "7800",
        libretroCore = "ProSystem (libretro)",
        coreDescriptionAr = "نواة ProSystem لتشغيل خراطيش Atari 7800 ProSystem",
        extensions = listOf("a78"),
        accentHex = 0xFF15803D,
        defaultAspectRatio = 4f / 3f
    ),
    ATARI_LYNX(
        displayName = "Atari Lynx",
        shortName = "LYNX",
        libretroCore = "Handy (libretro)",
        coreDescriptionAr = "نواة Handy لجهاز Atari Lynx المحمول بالألوان الكاملة",
        extensions = listOf("lnx", "lyx"),
        accentHex = 0xFF00B4D8,
        defaultAspectRatio = 160f / 102f
    ),
    ATARI_JAGUAR(
        displayName = "Atari Jaguar",
        shortName = "JAGUAR",
        libretroCore = "Virtual Jaguar (libretro)",
        coreDescriptionAr = "نواة Virtual Jaguar لتشغيل ألعاب الجيل 64-بت",
        extensions = listOf("j64", "jag"),
        accentHex = 0xFFD62828,
        defaultAspectRatio = 4f / 3f
    );

    val accentColor: Color get() = Color(accentHex)

    companion object {
        val supportedExtensions = setOf("a26", "bin", "a52", "atr", "xex", "car", "rom", "a78", "lnx", "lyx", "j64", "jag")

        fun fromExtension(ext: String): AtariSystem {
            val clean = ext.lowercase().trimStart('.')
            return entries.firstOrNull { clean in it.extensions } ?: ATARI_2600
        }

        fun fromCode(code: String): AtariSystem =
            entries.firstOrNull { it.name.equals(code, ignoreCase = true) } ?: ATARI_2600
    }
}
