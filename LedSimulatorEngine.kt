package com.rns.controller.data.simulator

import com.rns.controller.domain.model.CustomDirection
import com.rns.controller.domain.model.CustomTransitionStyle
import com.rns.controller.domain.model.EffectEngineMode
import com.rns.controller.domain.model.LedDriverType
import com.rns.controller.domain.model.LedModule
import com.rns.controller.domain.model.RgbwColor
import com.rns.controller.domain.model.RnsColorPresets
import com.rns.controller.domain.model.ZoneConfig
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class SimLedPixel(
    val r: Int,
    val g: Int,
    val b: Int,
    val w: Int = 0
)

/**
 * Software LED simulator engine matching ESP8266/ESP32 animation & 210 DMX math so users
 * can test all 100 animations, 210 DMX modes, 12 Full White Strobe variations,
 * Full Custom Color palettes, left/right independent zones, and serpentine custom segments.
 */
object LedSimulatorEngine {

    fun renderFrame(module: LedModule, timeMs: Long): Pair<List<SimLedPixel>, List<SimLedPixel>> {
        val leftZone = module.leftZone
        val rightZone = if (module.splitZonesIndependent) module.rightZone else module.leftZone
        val isDmx = module.effectEngine == EffectEngineMode.DMX210

        val leftPixels = renderZone(
            zone = leftZone,
            pixelCount = module.leftZone.pixelCount,
            masterPower = module.powerOn,
            masterBrightness = module.masterBrightness,
            speed = module.speed,
            driver = module.driver,
            isRightSide = false,
            timeMs = timeMs,
            isDmx = isDmx,
            dmxModeId = module.dmxModeId
        )
        val rightPixels = renderZone(
            zone = rightZone,
            pixelCount = module.rightZone.pixelCount,
            masterPower = module.powerOn,
            masterBrightness = module.masterBrightness,
            speed = module.speed,
            driver = module.driver,
            isRightSide = true,
            timeMs = timeMs,
            isDmx = isDmx,
            dmxModeId = module.dmxModeId
        )
        return leftPixels to rightPixels
    }

    fun renderCustomSerpentineSegments(
        module: LedModule,
        timeMs: Long,
        segmentColors: List<RgbwColor?>,
        totalSegments: Int = 24
    ): List<SimLedPixel?> {
        if (!module.powerOn) return List(totalSegments) { null }
        val br = module.masterBrightness / 255f
        val t = (timeMs / 1000f) * (0.5f + (module.speed / 100f) * 3.0f)

        return List(totalSegments) { i ->
            val dirOffset = when (module.customDirection) {
                CustomDirection.FORWARD -> -floor(t * 2f).toInt()
                CustomDirection.REVERSE -> floor(t * 2f).toInt()
                CustomDirection.CYCLE -> floor(t * 3f).toInt()
                CustomDirection.CHANGE -> 0
            }
            val srcIdx = (((i + dirOffset) % totalSegments) + totalSegments) % totalSegments
            val base = segmentColors.getOrNull(srcIdx) ?: return@List null

            val factor = when (module.customStyle) {
                CustomTransitionStyle.BREATHE -> 0.2f + 0.8f * (sin(t * 2.5f) * 0.5f + 0.5f)
                CustomTransitionStyle.FD, CustomTransitionStyle.FS -> {
                    val phase = floor(t * 5f + if (module.customStyle == CustomTransitionStyle.FS) i.toFloat() else 0f).toInt()
                    if (phase % 2 == 0) 1f else 0.15f
                }
                CustomTransitionStyle.PU -> max(0.15f, sin(t * 4f - i * 0.45f) * 0.5f + 0.5f)
                else -> 1f
            }

            val scale = br * factor
            SimLedPixel(
                r = (base.r * scale).toInt().coerceIn(0, 255),
                g = (base.g * scale).toInt().coerceIn(0, 255),
                b = (base.b * scale).toInt().coerceIn(0, 255),
                w = (base.w * scale).toInt().coerceIn(0, 255)
            )
        }
    }

    private fun lerpPixel(a: SimLedPixel, b: SimLedPixel, t: Float): SimLedPixel {
        val k = t.coerceIn(0f, 1f)
        return SimLedPixel(
            r = (a.r + (b.r - a.r) * k).toInt().coerceIn(0, 255),
            g = (a.g + (b.g - a.g) * k).toInt().coerceIn(0, 255),
            b = (a.b + (b.b - a.b) * k).toInt().coerceIn(0, 255)
        )
    }

    private fun samplePaletteSmooth(
        palette: List<SimLedPixel>,
        phase: Float,
        brightness: Float = 1f,
        pastelMix: Float = 0f
    ): SimLedPixel {
        val len = palette.size
        if (len == 0) return SimLedPixel(0, 0, 0)
        if (len == 1) {
            val c = palette[0]
            return SimLedPixel((c.r * brightness).toInt(), (c.g * brightness).toInt(), (c.b * brightness).toInt())
        }
        val p = ((phase % 1f) + 1f) % 1f
        val scaled = p * len
        val idx = floor(scaled).toInt() % len
        val nextIdx = (idx + 1) % len
        val frac = scaled - floor(scaled)
        var col = lerpPixel(palette[idx], palette[nextIdx], frac)
        if (pastelMix > 0f) {
            col = SimLedPixel(
                r = (col.r + (255 - col.r) * pastelMix).toInt(),
                g = (col.g + (255 - col.g) * pastelMix).toInt(),
                b = (col.b + (255 - col.b) * pastelMix).toInt()
            )
        }
        return SimLedPixel(
            r = (col.r * brightness).toInt().coerceIn(0, 255),
            g = (col.g * brightness).toInt().coerceIn(0, 255),
            b = (col.b * brightness).toInt().coerceIn(0, 255)
        )
    }

    private fun samplePaletteDiscrete(palette: List<SimLedPixel>, stepIndex: Int): SimLedPixel {
        if (palette.isEmpty()) return SimLedPixel(255, 255, 255)
        val idx = ((stepIndex % palette.size) + palette.size) % palette.size
        return palette[idx]
    }

    private fun renderZone(
        zone: ZoneConfig,
        pixelCount: Int,
        masterPower: Boolean,
        masterBrightness: Int,
        speed: Int,
        driver: LedDriverType,
        isRightSide: Boolean,
        timeMs: Long,
        isDmx: Boolean,
        dmxModeId: Int
    ): List<SimLedPixel> {
        val count = pixelCount.coerceIn(1, 120)
        if (!masterPower || !zone.enabled) {
            return List(count) { SimLedPixel(0, 0, 0, 0) }
        }

        val scale = (masterBrightness / 255f) * (zone.brightness / 255f)
        val spd = (0.35f + (speed / 100f) * 2.6f)
        val t = (timeMs / 1000f) * spd
        val anim = if (isDmx) dmxModeId.coerceIn(1, 210) else zone.animationId.coerceIn(1, 100)
        val wVal = if (driver.supportsWhiteChannel) (zone.color.w * scale).toInt().coerceIn(0, 255) else 0

        val normPal = RnsColorPresets.normalizePalette(zone).map { SimLedPixel(it.r, it.g, it.b) }
        val c1 = normPal[0]
        val c2 = normPal[1]
        val customEnabled = zone.customColorEnabled

        val raw = List(count) { i ->
            val pos = if (count > 1) i.toFloat() / (count - 1) else 0f
            val effectivePos = if (isRightSide && !isDmx && anim in listOf(57, 58, 62, 69)) 1f - pos else pos
            computePixel(anim, i, effectivePos, t, isRightSide, c1, c2, normPal, customEnabled, isDmx)
        }

        val finalRaw = if (!driver.isAddressable && raw.isNotEmpty()) {
            val avgR = raw.map { it.r }.average().toInt()
            val avgG = raw.map { it.g }.average().toInt()
            val avgB = raw.map { it.b }.average().toInt()
            List(count) { SimLedPixel(avgR, avgG, avgB, 0) }
        } else {
            raw
        }

        return finalRaw.map { px ->
            SimLedPixel(
                r = (px.r * scale).toInt().coerceIn(0, 255),
                g = (px.g * scale).toInt().coerceIn(0, 255),
                b = (px.b * scale).toInt().coerceIn(0, 255),
                w = wVal
            )
        }
    }

    private fun computeWhiteStrobe(
        variant: Int,
        pos: Float,
        t: Float,
        isRight: Boolean,
        customColorEnabled: Boolean,
        c1: SimLedPixel,
        c2: SimLedPixel
    ): SimLedPixel {
        val cyc = (t * 2.2f) % 1f
        var lit = false
        var useAlt = false
        when (((variant % 12) + 12) % 12) {
            0 -> lit = floor(t * 6f).toInt() % 2 == 0
            1 -> lit = floor(t * 14f).toInt() % 2 == 0
            2 -> {
                lit = cyc < 0.12f || (cyc >= 0.22f && cyc < 0.34f)
                useAlt = cyc >= 0.22f
            }
            3 -> {
                lit = cyc < 0.09f || (cyc >= 0.17f && cyc < 0.26f) || (cyc >= 0.34f && cyc < 0.43f)
                useAlt = cyc >= 0.17f && cyc < 0.26f
            }
            4 -> {
                lit = (cyc < 0.65f) && (floor(cyc * 12f).toInt() % 2 == 0)
                useAlt = floor(cyc * 6f).toInt() % 2 == 1
            }
            5 -> {
                lit = (floor(t * 5f).toInt() % 2 == 0) xor isRight
                useAlt = isRight
            }
            6 -> {
                val head = (t * 1.4f) % 1f
                lit = abs(pos - head) < 0.18f
                useAlt = head > 0.5f
            }
            7 -> lit = floor(t * 22f).toInt() % 2 == 0
            8 -> {
                val seed = sin(pos * 41.7f + floor(t * 9f) * 19.3f) * 43758.5453f
                val r = seed - floor(seed)
                lit = r > 0.65f
                useAlt = r > 0.82f
            }
            9 -> {
                lit = if (isRight) cyc < 0.14f || (cyc >= 0.2f && cyc < 0.32f)
                else cyc >= 0.5f && (cyc < 0.64f || (cyc >= 0.7f && cyc < 0.82f))
                useAlt = isRight
            }
            10 -> {
                val p = sin(t * 4f) * 0.5f + 0.5f
                val base = if (customColorEnabled) lerpPixel(c2, c1, p) else SimLedPixel(255, 255, 255)
                return SimLedPixel((base.r * p).toInt(), (base.g * p).toInt(), (base.b * p).toInt())
            }
            else -> {
                lit = cyc < 0.06f || (cyc >= 0.1f && cyc < 0.18f) || (cyc >= 0.25f && cyc < 0.29f)
                useAlt = cyc >= 0.1f && cyc < 0.18f
            }
        }
        if (!lit) return SimLedPixel(0, 0, 0)
        if (!customColorEnabled) return SimLedPixel(255, 255, 255)
        return if (useAlt) c2 else c1
    }

    private fun computePixel(
        anim: Int,
        idx: Int,
        pos: Float,
        t: Float,
        isRight: Boolean,
        c1: SimLedPixel,
        c2: SimLedPixel,
        palette: List<SimLedPixel>,
        customColorEnabled: Boolean,
        isDmx: Boolean
    ): SimLedPixel {
        val baseR = c1.r
        val baseG = c1.g
        val baseB = c1.b

        if (isDmx) {
            if (anim in 11..17) return computeWhiteStrobe(anim - 11, pos, t, isRight, customColorEnabled, c1, c2)
            if (anim in 105..132) return computeWhiteStrobe(anim - 105, pos, t, isRight, customColorEnabled, c1, c2)
            val dir = if (anim % 2 == 0) -1f else 1f
            val hue = (((pos * (1 + (anim % 3)) + dir * t * 0.4f + anim * 0.037f) % 1f) + 1f) % 1f
            val wave = if (anim % 5 == 0) {
                if (floor((t * 3f + pos * 4f) % 2f).toInt() == 0) 1f else 0.1f
            } else {
                0.45f + 0.55f * (sin(t * 2.8f * dir + pos * PI.toFloat() * 2f) * 0.5f + 0.5f)
            }
            return if (customColorEnabled) {
                samplePaletteSmooth(palette, hue, wave, if (anim % 7 == 0) 0.25f else 0f)
            } else {
                hsvToRgb(hue, if (anim % 7 == 0) 0.65f else 1f, wave)
            }
        }

        return when {
            anim == 1 -> SimLedPixel(baseR, baseG, baseB)
            anim in 2..15 -> {
                if (anim == 2) {
                    val triPal = if (customColorEnabled) palette.take(3) else listOf(
                        SimLedPixel(255, 0, 0),
                        SimLedPixel(0, 255, 0),
                        SimLedPixel(0, 80, 255)
                    )
                    return samplePaletteDiscrete(triPal, floor(t * 2.4f).toInt())
                }
                if (anim == 3) {
                    val sevenPal = if (customColorEnabled) {
                        val p0 = palette.getOrElse(0) { c1 }
                        val p1 = palette.getOrElse(1) { c2 }
                        val p2 = palette.getOrElse(2) { c1 }
                        val p3 = palette.getOrElse(3) { c2 }
                        listOf(p0, lerpPixel(p0, p1, 0.5f), p1, lerpPixel(p1, p2, 0.5f), p2, p3, lerpPixel(p3, p0, 0.5f))
                    } else {
                        listOf(
                            SimLedPixel(255, 0, 0),
                            SimLedPixel(255, 165, 0),
                            SimLedPixel(255, 255, 0),
                            SimLedPixel(0, 255, 0),
                            SimLedPixel(0, 220, 255),
                            SimLedPixel(0, 80, 255),
                            SimLedPixel(200, 0, 255)
                        )
                    }
                    return samplePaletteDiscrete(sevenPal, floor(t * 2.4f).toInt())
                }
                val offset = (anim - 2) * 0.08f
                val dir = if (anim == 7) -t else t
                val phase = ((pos * (if (anim == 6) 2f else 1f) + dir * 0.35f + offset) % 1f + 1f) % 1f
                if (customColorEnabled) {
                    samplePaletteSmooth(palette, phase, 1f, if (anim == 14) 0.35f else 0f)
                } else {
                    hsvToRgb(phase, if (anim == 14) 0.55f else 1f, 1f)
                }
            }
            anim in 16..35 -> {
                when (anim) {
                    18, 33 -> {
                        val head = (sin(t * 2.4f) * 0.5f + 0.5f)
                        val dist = abs(pos - head)
                        val intensity = max(0f, 1f - dist * 5.5f)
                        if (customColorEnabled) {
                            val blended = lerpPixel(c2, c1, min(1f, intensity * 1.4f))
                            SimLedPixel((blended.r * intensity).toInt(), (blended.g * intensity).toInt(), (blended.b * intensity).toInt())
                        } else {
                            SimLedPixel((baseR * intensity).toInt(), (baseG * intensity).toInt(), (baseB * intensity).toInt())
                        }
                    }
                    25 -> {
                        val flash = ((floor(t * 5f).toInt() % 2 == 0) xor isRight)
                        if (customColorEnabled) {
                            if (flash) c1 else c2
                        } else {
                            if (flash) SimLedPixel(255, 15, 25) else SimLedPixel(15, 90, 255)
                        }
                    }
                    26 -> {
                        val on = sin(t * 6f) > 0f
                        if (customColorEnabled) {
                            if (on) c1 else SimLedPixel((c2.r * 0.08f).toInt(), (c2.g * 0.08f).toInt(), (c2.b * 0.08f).toInt())
                        } else {
                            if (on) SimLedPixel(255, 155, 0) else SimLedPixel(15, 8, 0)
                        }
                    }
                    else -> {
                        val head = ((t * 0.65f + (anim % 5) * 0.1f) % 1f)
                        val dist = ((pos - head) % 1f + 1f) % 1f
                        val tail = max(0f, 1f - dist * (if (anim in listOf(19, 20)) 3f else 4.2f))
                        when {
                            anim == 20 -> {
                                if (customColorEnabled) samplePaletteSmooth(palette, pos + t * 0.3f, tail)
                                else hsvToRgb((pos + t * 0.3f) % 1f, 1f, tail)
                            }
                            customColorEnabled && anim % 2 == 1 -> {
                                val cometCol = lerpPixel(c2, c1, tail)
                                SimLedPixel((cometCol.r * tail).toInt(), (cometCol.g * tail).toInt(), (cometCol.b * tail).toInt())
                            }
                            else -> SimLedPixel((baseR * tail).toInt(), (baseG * tail).toInt(), (baseB * tail).toInt())
                        }
                    }
                }
            }
            anim in 36..47 -> computeWhiteStrobe(anim - 36, pos, t, isRight, customColorEnabled, c1, c2)
            anim in 48..55 -> {
                val wave = sin(t * 2.2f) * 0.5f + 0.5f
                if (anim == 55) {
                    val w = if (floor(t * 7f).toInt() % 2 == 0) 1f else 0.04f
                    if (customColorEnabled) {
                        val col = samplePaletteDiscrete(palette, floor(t * 2.5f).toInt())
                        SimLedPixel((col.r * w).toInt(), (col.g * w).toInt(), (col.b * w).toInt())
                    } else {
                        hsvToRgb((floor(t * 0.5f) * 0.17f) % 1f, 1f, w)
                    }
                } else if (customColorEnabled) {
                    val mix = lerpPixel(c2, c1, wave)
                    SimLedPixel((mix.r * wave).toInt(), (mix.g * wave).toInt(), (mix.b * wave).toInt())
                } else {
                    when (anim) {
                        49 -> SimLedPixel((255 * wave).toInt(), 0, 0)
                        50 -> SimLedPixel(0, (255 * wave).toInt(), 0)
                        51 -> SimLedPixel(0, 40, (255 * wave).toInt())
                        52 -> SimLedPixel((255 * wave).toInt(), (215 * wave).toInt(), 0)
                        53 -> SimLedPixel(0, (230 * wave).toInt(), (255 * wave).toInt())
                        54 -> SimLedPixel((190 * wave).toInt(), 0, (255 * wave).toInt())
                        else -> SimLedPixel((baseR * wave).toInt(), (baseG * wave).toInt(), (baseB * wave).toInt())
                    }
                }
            }
            anim in 56..75 -> {
                when (anim) {
                    56 -> {
                        val leftActive = (floor(t * 2f).toInt() % 2 == 0)
                        if (leftActive xor isRight) SimLedPixel(baseR, baseG, baseB) else SimLedPixel(0, 0, 0)
                    }
                    61 -> {
                        val col = if (customColorEnabled) c1 else SimLedPixel(255, 165, 0)
                        if (!isRight && ((t * 1.8f) % 1f) >= pos) col else SimLedPixel(0, 0, 0)
                    }
                    62 -> {
                        val col = if (customColorEnabled) c1 else SimLedPixel(255, 165, 0)
                        if (isRight && ((t * 1.8f) % 1f) >= pos) col else SimLedPixel(0, 0, 0)
                    }
                    71 -> {
                        if (customColorEnabled) {
                            val wv = 0.55f + 0.45f * sin(t * 2.2f + pos * 3f)
                            if (!isRight) lerpPixel(c1, c2, wv) else lerpPixel(c2, c1, wv)
                        } else {
                            if (!isRight) hsvToRgb(0.04f + sin(t + pos) * 0.04f, 1f, 1f)
                            else hsvToRgb(0.55f + cos(t + pos) * 0.05f, 1f, 1f)
                        }
                    }
                    74 -> {
                        val flash = floor(t * 6f).toInt() % 2 == 0
                        val altCol = if (customColorEnabled) c2 else SimLedPixel(255, 255, 255)
                        if (flash xor isRight) altCol else SimLedPixel(baseR, baseG, baseB)
                    }
                    else -> {
                        val phase = if (isRight) t + 0.5f else t
                        val pulse = sin(phase * 3f + pos * 4f) * 0.5f + 0.5f
                        if (customColorEnabled) {
                            val col = lerpPixel(c2, c1, pulse)
                            SimLedPixel((col.r * pulse).toInt(), (col.g * pulse).toInt(), (col.b * pulse).toInt())
                        } else {
                            SimLedPixel((baseR * pulse).toInt(), (baseG * pulse).toInt(), (baseB * pulse).toInt())
                        }
                    }
                }
            }
            else -> {
                val seed = sin((idx * 12.9898f + floor(t * 4f) * 78.233f)) * 43758.5453f
                val rnd = seed - floor(seed)
                when (anim) {
                    76, 89, 98 -> {
                        val heat = max(0f, min(1f, (1f - pos * 0.7f) * (0.65f + 0.35f * sin(t * 5f + idx))))
                        if (customColorEnabled) {
                            val fc = lerpPixel(c1, c2, heat)
                            val k = 0.25f + 0.75f * heat
                            SimLedPixel((fc.r * k).toInt(), (fc.g * k).toInt(), (fc.b * k).toInt())
                        } else {
                            SimLedPixel(255, (heat * 145).toInt(), (heat * 20).toInt())
                        }
                    }
                    77, 96 -> {
                        val w = sin(t * 1.6f + pos * 5f) * 0.5f + 0.5f
                        val v = 0.4f + 0.6f * w
                        if (customColorEnabled) {
                            val col = lerpPixel(c1, c2, w)
                            SimLedPixel((col.r * v).toInt(), (col.g * v).toInt(), (col.b * v).toInt())
                        } else {
                            SimLedPixel(0, (140 * v).toInt(), (255 * v).toInt())
                        }
                    }
                    78, 87, 88 -> {
                        val sparkle = if (abs(rnd) > 0.78f) 1f else 0.08f
                        val col = if (customColorEnabled && abs(rnd) > 0.88f) c2 else c1
                        SimLedPixel((col.r * sparkle).toInt(), (col.g * sparkle).toInt(), (col.b * sparkle).toInt())
                    }
                    else -> {
                        val hue = ((pos + t * 0.45f + (anim - 75) * 0.04f) % 1f + 1f) % 1f
                        val lum = 0.45f + 0.55f * (cos(t * 3.1f + idx * 0.5f) * 0.5f + 0.5f)
                        if (customColorEnabled) samplePaletteSmooth(palette, hue, lum) else hsvToRgb(hue, 0.92f, lum)
                    }
                }
            }
        }
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): SimLedPixel {
        val i = floor(h * 6f).toInt()
        val f = h * 6f - i
        val p = v * (1f - s)
        val q = v * (1f - f * s)
        val t = v * (1f - (1f - f) * s)
        val (r, g, b) = when (((i % 6) + 6) % 6) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }
        return SimLedPixel((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }
}
