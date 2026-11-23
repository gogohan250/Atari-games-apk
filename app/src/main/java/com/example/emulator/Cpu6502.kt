package com.example.emulator

import java.nio.ByteBuffer

/**
 * Complete MOS 6502 / 6507 CPU Core for Atari 2600 / 5200 / 7800 / 800 emulation,
 * including common undocumented/illegal opcodes used by commercial Atari 2600 kernels.
 */
class Cpu6502(
    private val readMem: (Int) -> Int,
    private val writeMem: (Int, Int) -> Unit
) {
    var a: Int = 0
    var x: Int = 0
    var y: Int = 0
    var sp: Int = 0xFD
    var pc: Int = 0
    var status: Int = 0x24 // N V 1 B D I Z C
    var totalCycles: Long = 0L

    private val FLAG_C = 0x01
    private val FLAG_Z = 0x02
    private val FLAG_I = 0x04
    private val FLAG_D = 0x08
    private val FLAG_B = 0x10
    private val FLAG_U = 0x20
    private val FLAG_V = 0x40
    private val FLAG_N = 0x80

    fun reset() {
        a = 0
        x = 0
        y = 0
        sp = 0xFD
        status = 0x24
        pc = readWord(0xFFFC)
        totalCycles = 0L
    }

    private fun read(addr: Int): Int = readMem(addr and 0xFFFF) and 0xFF
    private fun write(addr: Int, value: Int) = writeMem(addr and 0xFFFF, value and 0xFF)
    private fun readWord(addr: Int): Int = read(addr) or (read(addr + 1) shl 8)

    private fun fetchByte(): Int {
        val b = read(pc)
        pc = (pc + 1) and 0xFFFF
        return b
    }

    private fun fetchWord(): Int {
        val lo = fetchByte()
        val hi = fetchByte()
        return lo or (hi shl 8)
    }

    private fun push(v: Int) {
        write(0x0100 + sp, v)
        sp = (sp - 1) and 0xFF
    }

    private fun pop(): Int {
        sp = (sp + 1) and 0xFF
        return read(0x0100 + sp)
    }

    private fun setZN(value: Int): Int {
        val v = value and 0xFF
        status = if (v == 0) status or FLAG_Z else status and FLAG_Z.inv()
        status = if ((v and 0x80) != 0) status or FLAG_N else status and FLAG_N.inv()
        return v
    }

    private fun setFlag(flag: Int, cond: Boolean) {
        status = if (cond) status or flag else status and flag.inv()
    }

    private fun hasFlag(flag: Int): Boolean = (status and flag) != 0

    // Addressing modes
    private fun zp() = fetchByte()
    private fun zpX() = (fetchByte() + x) and 0xFF
    private fun zpY() = (fetchByte() + y) and 0xFF
    private fun abs() = fetchWord()
    private fun absX() = (fetchWord() + x) and 0xFFFF
    private fun absY() = (fetchWord() + y) and 0xFFFF
    private fun indX(): Int {
        val zpAddr = (fetchByte() + x) and 0xFF
        return read(zpAddr) or (read((zpAddr + 1) and 0xFF) shl 8)
    }
    private fun indY(): Int {
        val zpAddr = fetchByte()
        val base = read(zpAddr) or (read((zpAddr + 1) and 0xFF) shl 8)
        return (base + y) and 0xFFFF
    }

    private fun branch(cond: Boolean): Int {
        val offset = fetchByte().toByte().toInt()
        if (!cond) return 2
        val oldPc = pc
        pc = (pc + offset) and 0xFFFF
        return if ((oldPc and 0xFF00) != (pc and 0xFF00)) 4 else 3
    }

    private fun adc(value: Int) {
        val carry = if (hasFlag(FLAG_C)) 1 else 0
        if (hasFlag(FLAG_D)) {
            var lo = (a and 0x0F) + (value and 0x0F) + carry
            if (lo > 9) lo += 6
            var hi = (a shr 4) + (value shr 4) + if (lo > 0x0F) 1 else 0
            if (hi > 9) hi += 6
            setFlag(FLAG_C, hi > 0x0F)
            a = setZN(((hi and 0x0F) shl 4) or (lo and 0x0F))
        } else {
            val sum = a + value + carry
            setFlag(FLAG_C, sum > 0xFF)
            setFlag(FLAG_V, ((a xor sum) and (value xor sum) and 0x80) != 0)
            a = setZN(sum)
        }
    }

    private fun sbc(value: Int) {
        val borrow = if (hasFlag(FLAG_C)) 0 else 1
        if (hasFlag(FLAG_D)) {
            var lo = (a and 0x0F) - (value and 0x0F) - borrow
            var borrowHi = 0
            if (lo < 0) {
                lo -= 6
                borrowHi = 1
            }
            var hi = (a shr 4) - (value shr 4) - borrowHi
            if (hi < 0) {
                hi -= 6
                setFlag(FLAG_C, false)
            } else {
                setFlag(FLAG_C, true)
            }
            a = setZN(((hi and 0x0F) shl 4) or (lo and 0x0F))
        } else {
            adc(value xor 0xFF)
        }
    }

    private fun cmp(reg: Int, value: Int) {
        val diff = reg - value
        setFlag(FLAG_C, reg >= value)
        setZN(diff)
    }

    private fun aslVal(v: Int): Int {
        setFlag(FLAG_C, (v and 0x80) != 0)
        return setZN(v shl 1)
    }

    private fun lsrVal(v: Int): Int {
        setFlag(FLAG_C, (v and 0x01) != 0)
        return setZN(v ushr 1)
    }

    private fun rolVal(v: Int): Int {
        val c = if (hasFlag(FLAG_C)) 1 else 0
        setFlag(FLAG_C, (v and 0x80) != 0)
        return setZN((v shl 1) or c)
    }

    private fun rorVal(v: Int): Int {
        val c = if (hasFlag(FLAG_C)) 0x80 else 0
        setFlag(FLAG_C, (v and 0x01) != 0)
        return setZN((v ushr 1) or c)
    }

    fun step(): Int {
        val opcode = fetchByte()
        val cycles = when (opcode) {
            // LDA
            0xA9 -> { a = setZN(fetchByte()); 2 }
            0xA5 -> { a = setZN(read(zp())); 3 }
            0xB5 -> { a = setZN(read(zpX())); 4 }
            0xAD -> { a = setZN(read(abs())); 4 }
            0xBD -> { a = setZN(read(absX())); 4 }
            0xB9 -> { a = setZN(read(absY())); 4 }
            0xA1 -> { a = setZN(read(indX())); 6 }
            0xB1 -> { a = setZN(read(indY())); 5 }
            // LDX
            0xA2 -> { x = setZN(fetchByte()); 2 }
            0xA6 -> { x = setZN(read(zp())); 3 }
            0xB6 -> { x = setZN(read(zpY())); 4 }
            0xAE -> { x = setZN(read(abs())); 4 }
            0xBE -> { x = setZN(read(absY())); 4 }
            // LDY
            0xA0 -> { y = setZN(fetchByte()); 2 }
            0xA4 -> { y = setZN(read(zp())); 3 }
            0xB4 -> { y = setZN(read(zpX())); 4 }
            0xAC -> { y = setZN(read(abs())); 4 }
            0xBC -> { y = setZN(read(absX())); 4 }
            // STA
            0x85 -> { write(zp(), a); 3 }
            0x95 -> { write(zpX(), a); 4 }
            0x8D -> { write(abs(), a); 4 }
            0x9D -> { write(absX(), a); 5 }
            0x99 -> { write(absY(), a); 5 }
            0x81 -> { write(indX(), a); 6 }
            0x91 -> { write(indY(), a); 6 }
            // STX
            0x86 -> { write(zp(), x); 3 }
            0x96 -> { write(zpY(), x); 4 }
            0x8E -> { write(abs(), x); 4 }
            // STY
            0x84 -> { write(zp(), y); 3 }
            0x94 -> { write(zpX(), y); 4 }
            0x8C -> { write(abs(), y); 4 }
            // Transfers
            0xAA -> { x = setZN(a); 2 }
            0x8A -> { a = setZN(x); 2 }
            0xA8 -> { y = setZN(a); 2 }
            0x98 -> { a = setZN(y); 2 }
            0xBA -> { x = setZN(sp); 2 }
            0x9A -> { sp = x; 2 }
            // Stack
            0x48 -> { push(a); 3 }
            0x68 -> { a = setZN(pop()); 4 }
            0x08 -> { push(status or FLAG_B or FLAG_U); 3 }
            0x28 -> { status = (pop() and FLAG_B.inv()) or FLAG_U; 4 }
            // Logical AND
            0x29 -> { a = setZN(a and fetchByte()); 2 }
            0x25 -> { a = setZN(a and read(zp())); 3 }
            0x35 -> { a = setZN(a and read(zpX())); 4 }
            0x2D -> { a = setZN(a and read(abs())); 4 }
            0x3D -> { a = setZN(a and read(absX())); 4 }
            0x39 -> { a = setZN(a and read(absY())); 4 }
            0x21 -> { a = setZN(a and read(indX())); 6 }
            0x31 -> { a = setZN(a and read(indY())); 5 }
            // Logical ORA
            0x09 -> { a = setZN(a or fetchByte()); 2 }
            0x05 -> { a = setZN(a or read(zp())); 3 }
            0x15 -> { a = setZN(a or read(zpX())); 4 }
            0x0D -> { a = setZN(a or read(abs())); 4 }
            0x1D -> { a = setZN(a or read(absX())); 4 }
            0x19 -> { a = setZN(a or read(absY())); 4 }
            0x01 -> { a = setZN(a or read(indX())); 6 }
            0x11 -> { a = setZN(a or read(indY())); 5 }
            // Logical EOR
            0x49 -> { a = setZN(a xor fetchByte()); 2 }
            0x45 -> { a = setZN(a xor read(zp())); 3 }
            0x55 -> { a = setZN(a xor read(zpX())); 4 }
            0x4D -> { a = setZN(a xor read(abs())); 4 }
            0x5D -> { a = setZN(a xor read(absX())); 4 }
            0x59 -> { a = setZN(a xor read(absY())); 4 }
            0x41 -> { a = setZN(a xor read(indX())); 6 }
            0x51 -> { a = setZN(a xor read(indY())); 5 }
            // BIT
            0x24 -> {
                val v = read(zp())
                setFlag(FLAG_Z, (a and v) == 0)
                setFlag(FLAG_V, (v and 0x40) != 0)
                setFlag(FLAG_N, (v and 0x80) != 0)
                3
            }
            0x2C -> {
                val v = read(abs())
                setFlag(FLAG_Z, (a and v) == 0)
                setFlag(FLAG_V, (v and 0x40) != 0)
                setFlag(FLAG_N, (v and 0x80) != 0)
                4
            }
            // ADC
            0x69 -> { adc(fetchByte()); 2 }
            0x65 -> { adc(read(zp())); 3 }
            0x75 -> { adc(read(zpX())); 4 }
            0x6D -> { adc(read(abs())); 4 }
            0x7D -> { adc(read(absX())); 4 }
            0x79 -> { adc(read(absY())); 4 }
            0x61 -> { adc(read(indX())); 6 }
            0x71 -> { adc(read(indY())); 5 }
            // SBC
            0xE9, 0xEB -> { sbc(fetchByte()); 2 }
            0xE5 -> { sbc(read(zp())); 3 }
            0xF5 -> { sbc(read(zpX())); 4 }
            0xED -> { sbc(read(abs())); 4 }
            0xFD -> { sbc(read(absX())); 4 }
            0xF9 -> { sbc(read(absY())); 4 }
            0xE1 -> { sbc(read(indX())); 6 }
            0xF1 -> { sbc(read(indY())); 5 }
            // CMP / CPX / CPY
            0xC9 -> { cmp(a, fetchByte()); 2 }
            0xC5 -> { cmp(a, read(zp())); 3 }
            0xD5 -> { cmp(a, read(zpX())); 4 }
            0xCD -> { cmp(a, read(abs())); 4 }
            0xDD -> { cmp(a, read(absX())); 4 }
            0xD9 -> { cmp(a, read(absY())); 4 }
            0xC1 -> { cmp(a, read(indX())); 6 }
            0xD1 -> { cmp(a, read(indY())); 5 }
            0xE0 -> { cmp(x, fetchByte()); 2 }
            0xE4 -> { cmp(x, read(zp())); 3 }
            0xEC -> { cmp(x, read(abs())); 4 }
            0xC0 -> { cmp(y, fetchByte()); 2 }
            0xC4 -> { cmp(y, read(zp())); 3 }
            0xCC -> { cmp(y, read(abs())); 4 }
            // INC / DEC / INX / DEX / INY / DEY
            0xE6 -> { val ad = zp(); write(ad, setZN(read(ad) + 1)); 5 }
            0xF6 -> { val ad = zpX(); write(ad, setZN(read(ad) + 1)); 6 }
            0xEE -> { val ad = abs(); write(ad, setZN(read(ad) + 1)); 6 }
            0xFE -> { val ad = absX(); write(ad, setZN(read(ad) + 1)); 7 }
            0xC6 -> { val ad = zp(); write(ad, setZN(read(ad) - 1)); 5 }
            0xD6 -> { val ad = zpX(); write(ad, setZN(read(ad) - 1)); 6 }
            0xCE -> { val ad = abs(); write(ad, setZN(read(ad) - 1)); 6 }
            0xDE -> { val ad = absX(); write(ad, setZN(read(ad) - 1)); 7 }
            0xE8 -> { x = setZN(x + 1); 2 }
            0xCA -> { x = setZN(x - 1); 2 }
            0xC8 -> { y = setZN(y + 1); 2 }
            0x88 -> { y = setZN(y - 1); 2 }
            // Shifts & Rotates
            0x0A -> { a = aslVal(a); 2 }
            0x06 -> { val ad = zp(); write(ad, aslVal(read(ad))); 5 }
            0x16 -> { val ad = zpX(); write(ad, aslVal(read(ad))); 6 }
            0x0E -> { val ad = abs(); write(ad, aslVal(read(ad))); 6 }
            0x1E -> { val ad = absX(); write(ad, aslVal(read(ad))); 7 }
            0x4A -> { a = lsrVal(a); 2 }
            0x46 -> { val ad = zp(); write(ad, lsrVal(read(ad))); 5 }
            0x56 -> { val ad = zpX(); write(ad, lsrVal(read(ad))); 6 }
            0x4E -> { val ad = abs(); write(ad, lsrVal(read(ad))); 6 }
            0x5E -> { val ad = absX(); write(ad, lsrVal(read(ad))); 7 }
            0x2A -> { a = rolVal(a); 2 }
            0x26 -> { val ad = zp(); write(ad, rolVal(read(ad))); 5 }
            0x36 -> { val ad = zpX(); write(ad, rolVal(read(ad))); 6 }
            0x2E -> { val ad = abs(); write(ad, rolVal(read(ad))); 6 }
            0x3E -> { val ad = absX(); write(ad, rolVal(read(ad))); 7 }
            0x6A -> { a = rorVal(a); 2 }
            0x66 -> { val ad = zp(); write(ad, rorVal(read(ad))); 5 }
            0x76 -> { val ad = zpX(); write(ad, rorVal(read(ad))); 6 }
            0x6E -> { val ad = abs(); write(ad, rorVal(read(ad))); 6 }
            0x7E -> { val ad = absX(); write(ad, rorVal(read(ad))); 7 }
            // Jumps & Subroutines
            0x4C -> { pc = fetchWord(); 3 }
            0x6C -> {
                val ptr = fetchWord()
                val lo = read(ptr)
                val hiAddr = (ptr and 0xFF00) or ((ptr + 1) and 0xFF)
                pc = lo or (read(hiAddr) shl 8)
                5
            }
            0x20 -> {
                val target = fetchWord()
                val ret = (pc - 1) and 0xFFFF
                push((ret shr 8) and 0xFF)
                push(ret and 0xFF)
                pc = target
                6
            }
            0x60 -> {
                val lo = pop()
                val hi = pop()
                pc = ((lo or (hi shl 8)) + 1) and 0xFFFF
                6
            }
            0x40 -> {
                status = (pop() and FLAG_B.inv()) or FLAG_U
                val lo = pop()
                val hi = pop()
                pc = lo or (hi shl 8)
                6
            }
            0x00 -> {
                pc = (pc + 1) and 0xFFFF
                push((pc shr 8) and 0xFF)
                push(pc and 0xFF)
                push(status or FLAG_B or FLAG_U)
                setFlag(FLAG_I, true)
                pc = readWord(0xFFFE)
                7
            }
            // Branches
            0x90 -> branch(!hasFlag(FLAG_C)) // BCC
            0xB0 -> branch(hasFlag(FLAG_C))  // BCS
            0xF0 -> branch(hasFlag(FLAG_Z))  // BEQ
            0xD0 -> branch(!hasFlag(FLAG_Z)) // BNE
            0x30 -> branch(hasFlag(FLAG_N))  // BMI
            0x10 -> branch(!hasFlag(FLAG_N)) // BPL
            0x50 -> branch(!hasFlag(FLAG_V)) // BVC
            0x70 -> branch(hasFlag(FLAG_V))  // BVS
            // Flags
            0x18 -> { setFlag(FLAG_C, false); 2 }
            0x38 -> { setFlag(FLAG_C, true); 2 }
            0x58 -> { setFlag(FLAG_I, false); 2 }
            0x78 -> { setFlag(FLAG_I, true); 2 }
            0xB8 -> { setFlag(FLAG_V, false); 2 }
            0xD8 -> { setFlag(FLAG_D, false); 2 }
            0xF8 -> { setFlag(FLAG_D, true); 2 }

            // --- Common Undocumented 6502 Opcodes (Atari 2600 kernels) ---
            // LAX
            0xA7 -> { val v = read(zp()); a = setZN(v); x = v; 3 }
            0xB7 -> { val v = read(zpY()); a = setZN(v); x = v; 4 }
            0xAF -> { val v = read(abs()); a = setZN(v); x = v; 4 }
            0xBF -> { val v = read(absY()); a = setZN(v); x = v; 4 }
            0xA3 -> { val v = read(indX()); a = setZN(v); x = v; 6 }
            0xB3 -> { val v = read(indY()); a = setZN(v); x = v; 5 }
            // SAX
            0x87 -> { write(zp(), a and x); 3 }
            0x97 -> { write(zpY(), a and x); 4 }
            0x8F -> { write(abs(), a and x); 4 }
            0x83 -> { write(indX(), a and x); 6 }
            // DCP
            0xC7 -> { val ad = zp(); val v = setZN(read(ad) - 1); write(ad, v); cmp(a, v); 5 }
            0xD7 -> { val ad = zpX(); val v = setZN(read(ad) - 1); write(ad, v); cmp(a, v); 6 }
            0xCF -> { val ad = abs(); val v = setZN(read(ad) - 1); write(ad, v); cmp(a, v); 6 }
            0xDF -> { val ad = absX(); val v = setZN(read(ad) - 1); write(ad, v); cmp(a, v); 7 }
            0xDB -> { val ad = absY(); val v = setZN(read(ad) - 1); write(ad, v); cmp(a, v); 7 }
            0xC3 -> { val ad = indX(); val v = setZN(read(ad) - 1); write(ad, v); cmp(a, v); 8 }
            0xD3 -> { val ad = indY(); val v = setZN(read(ad) - 1); write(ad, v); cmp(a, v); 8 }
            // ISB / ISC
            0xE7 -> { val ad = zp(); val v = setZN(read(ad) + 1); write(ad, v); sbc(v); 5 }
            0xF7 -> { val ad = zpX(); val v = setZN(read(ad) + 1); write(ad, v); sbc(v); 6 }
            0xEF -> { val ad = abs(); val v = setZN(read(ad) + 1); write(ad, v); sbc(v); 6 }
            0xFF -> { val ad = absX(); val v = setZN(read(ad) + 1); write(ad, v); sbc(v); 7 }
            0xFB -> { val ad = absY(); val v = setZN(read(ad) + 1); write(ad, v); sbc(v); 7 }
            0xE3 -> { val ad = indX(); val v = setZN(read(ad) + 1); write(ad, v); sbc(v); 8 }
            0xF3 -> { val ad = indY(); val v = setZN(read(ad) + 1); write(ad, v); sbc(v); 8 }
            // SLO
            0x07 -> { val ad = zp(); val v = aslVal(read(ad)); write(ad, v); a = setZN(a or v); 5 }
            0x17 -> { val ad = zpX(); val v = aslVal(read(ad)); write(ad, v); a = setZN(a or v); 6 }
            0x0F -> { val ad = abs(); val v = aslVal(read(ad)); write(ad, v); a = setZN(a or v); 6 }
            0x1F -> { val ad = absX(); val v = aslVal(read(ad)); write(ad, v); a = setZN(a or v); 7 }
            0x1B -> { val ad = absY(); val v = aslVal(read(ad)); write(ad, v); a = setZN(a or v); 7 }
            0x03 -> { val ad = indX(); val v = aslVal(read(ad)); write(ad, v); a = setZN(a or v); 8 }
            0x13 -> { val ad = indY(); val v = aslVal(read(ad)); write(ad, v); a = setZN(a or v); 8 }
            // RLA
            0x27 -> { val ad = zp(); val v = rolVal(read(ad)); write(ad, v); a = setZN(a and v); 5 }
            0x37 -> { val ad = zpX(); val v = rolVal(read(ad)); write(ad, v); a = setZN(a and v); 6 }
            0x2F -> { val ad = abs(); val v = rolVal(read(ad)); write(ad, v); a = setZN(a and v); 6 }
            0x3F -> { val ad = absX(); val v = rolVal(read(ad)); write(ad, v); a = setZN(a and v); 7 }
            0x3B -> { val ad = absY(); val v = rolVal(read(ad)); write(ad, v); a = setZN(a and v); 7 }
            0x23 -> { val ad = indX(); val v = rolVal(read(ad)); write(ad, v); a = setZN(a and v); 8 }
            0x33 -> { val ad = indY(); val v = rolVal(read(ad)); write(ad, v); a = setZN(a and v); 8 }
            // SRE
            0x47 -> { val ad = zp(); val v = lsrVal(read(ad)); write(ad, v); a = setZN(a xor v); 5 }
            0x57 -> { val ad = zpX(); val v = lsrVal(read(ad)); write(ad, v); a = setZN(a xor v); 6 }
            0x4F -> { val ad = abs(); val v = lsrVal(read(ad)); write(ad, v); a = setZN(a xor v); 6 }
            0x5F -> { val ad = absX(); val v = lsrVal(read(ad)); write(ad, v); a = setZN(a xor v); 7 }
            0x5B -> { val ad = absY(); val v = lsrVal(read(ad)); write(ad, v); a = setZN(a xor v); 7 }
            0x43 -> { val ad = indX(); val v = lsrVal(read(ad)); write(ad, v); a = setZN(a xor v); 8 }
            0x53 -> { val ad = indY(); val v = lsrVal(read(ad)); write(ad, v); a = setZN(a xor v); 8 }
            // RRA
            0x67 -> { val ad = zp(); val v = rorVal(read(ad)); write(ad, v); adc(v); 5 }
            0x77 -> { val ad = zpX(); val v = rorVal(read(ad)); write(ad, v); adc(v); 6 }
            0x6F -> { val ad = abs(); val v = rorVal(read(ad)); write(ad, v); adc(v); 6 }
            0x7F -> { val ad = absX(); val v = rorVal(read(ad)); write(ad, v); adc(v); 7 }
            0x7B -> { val ad = absY(); val v = rorVal(read(ad)); write(ad, v); adc(v); 7 }
            0x63 -> { val ad = indX(); val v = rorVal(read(ad)); write(ad, v); adc(v); 8 }
            0x73 -> { val ad = indY(); val v = rorVal(read(ad)); write(ad, v); adc(v); 8 }
            // ANC, ALR, ARR, SBX
            0x0B, 0x2B -> { a = setZN(a and fetchByte()); setFlag(FLAG_C, (a and 0x80) != 0); 2 }
            0x4B -> { a = lsrVal(a and fetchByte()); 2 }
            0x6B -> {
                a = rorVal(a and fetchByte())
                setFlag(FLAG_C, (a and 0x40) != 0)
                setFlag(FLAG_V, ((a and 0x40) shr 6) xor ((a and 0x20) shr 5) != 0)
                2
            }
            0xCB -> {
                val imm = fetchByte()
                val ax = a and x
                setFlag(FLAG_C, ax >= imm)
                x = setZN((ax - imm) and 0xFF)
                2
            }
            // NOP & 1/2/3-byte undocumented NOPs
            0xEA, 0x1A, 0x3A, 0x5A, 0x7A, 0xDA, 0xFA -> 2
            0x04, 0x14, 0x34, 0x44, 0x54, 0x64, 0x74,
            0x80, 0x82, 0x89, 0xC2, 0xD4, 0xE2, 0xF4 -> { fetchByte(); 3 }
            0x0C, 0x1C, 0x3C, 0x5C, 0x7C, 0xDC, 0xFC -> { fetchWord(); 4 }
            else -> 2
        }
        totalCycles += cycles
        return cycles
    }

    fun writeState(buf: ByteBuffer) {
        buf.putInt(a)
        buf.putInt(x)
        buf.putInt(y)
        buf.putInt(sp)
        buf.putInt(pc)
        buf.putInt(status)
        buf.putLong(totalCycles)
    }

    fun readState(buf: ByteBuffer) {
        a = buf.int
        x = buf.int
        y = buf.int
        sp = buf.int
        pc = buf.int
        status = buf.int
        totalCycles = buf.long
    }
}
