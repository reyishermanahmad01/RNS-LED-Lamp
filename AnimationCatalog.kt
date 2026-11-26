package com.rns.controller.domain.catalog

import com.rns.controller.domain.model.AnimationSlot
import com.rns.controller.domain.model.DmxModeSlot

object AnimationCatalog {

    val categories = listOf(
        "Semua",
        "Statis",
        "Spektrum",
        "Chase & Comet",
        "Strobo Full White",
        "Napas & Pulse",
        "Zona Kanan-Kiri",
        "Prosedural FX"
    )

    val slots: List<AnimationSlot> = listOf(
        AnimationSlot(1, "Solid Color", "Statis", 50),
        AnimationSlot(2, "Tricolor Jump", "Spektrum", 55),
        AnimationSlot(3, "Seven-Color Jump", "Spektrum", 58),
        AnimationSlot(4, "Tricolor Gradient", "Spektrum", 48),
        AnimationSlot(5, "Seven-Color Gradient", "Spektrum", 48),
        AnimationSlot(6, "Rainbow Flow", "Spektrum", 55),
        AnimationSlot(7, "Reverse Rainbow", "Spektrum", 55),
        AnimationSlot(8, "Prism Wave", "Spektrum", 52),
        AnimationSlot(9, "Aurora Borealis", "Spektrum", 42),
        AnimationSlot(10, "Sunset Horizon", "Spektrum", 38),
        AnimationSlot(11, "Ocean Tide", "Spektrum", 45),
        AnimationSlot(12, "Forest Canopy", "Spektrum", 40),
        AnimationSlot(13, "Cyber Pulse", "Spektrum", 70),
        AnimationSlot(14, "Pastel Drift", "Spektrum", 44),
        AnimationSlot(15, "Solar Flare", "Spektrum", 62),

        AnimationSlot(16, "Comet Tail", "Chase & Comet", 65),
        AnimationSlot(17, "Dual Comet", "Chase & Comet", 68),
        AnimationSlot(18, "Larson Scanner", "Chase & Comet", 72),
        AnimationSlot(19, "Theater Chase", "Chase & Comet", 60),
        AnimationSlot(20, "Theater Rainbow", "Chase & Comet", 60),
        AnimationSlot(21, "Running Lights", "Chase & Comet", 55),
        AnimationSlot(22, "Meteor Shower", "Chase & Comet", 75),
        AnimationSlot(23, "Shooting Star", "Chase & Comet", 80),
        AnimationSlot(24, "Sparkle Comet", "Chase & Comet", 70),
        AnimationSlot(25, "Patrol Red Blue", "Chase & Comet", 85),
        AnimationSlot(26, "Hazard Amber", "Chase & Comet", 65),
        AnimationSlot(27, "Emergency Flash", "Chase & Comet", 82),
        AnimationSlot(28, "Racing Wipe", "Chase & Comet", 74),
        AnimationSlot(29, "Bidirectional Chase", "Chase & Comet", 66),
        AnimationSlot(30, "Pixel Train", "Chase & Comet", 60),
        AnimationSlot(31, "Bounce Ball", "Chase & Comet", 68),
        AnimationSlot(32, "Multi Bounce", "Chase & Comet", 72),
        AnimationSlot(33, "Laser Sweep", "Chase & Comet", 78),
        AnimationSlot(34, "Energy Beam", "Chase & Comet", 76),
        AnimationSlot(35, "Warp Speed", "Chase & Comet", 90),

        // Variasi Strobo Full White (36..47)
        AnimationSlot(36, "Strobe Full White Classic", "Strobo Full White", 80, true),
        AnimationSlot(37, "Strobe Full White Rapid", "Strobo Full White", 92, true),
        AnimationSlot(38, "Strobe Full White Double-Flash", "Strobo Full White", 84, true),
        AnimationSlot(39, "Strobe Full White Triple-Burst", "Strobo Full White", 86, true),
        AnimationSlot(40, "Strobe Full White Quad-Strobe", "Strobo Full White", 88, true),
        AnimationSlot(41, "Strobe Full White Alternate L/R", "Strobo Full White", 82, true),
        AnimationSlot(42, "Strobe Full White Split Sweep", "Strobo Full White", 78, true),
        AnimationSlot(43, "Strobe Full White Hyper Blitz", "Strobo Full White", 96, true),
        AnimationSlot(44, "Strobe Full White Random Pop", "Strobo Full White", 85, true),
        AnimationSlot(45, "Strobe Full White Airplane Wig-Wag", "Strobo Full White", 76, true),
        AnimationSlot(46, "Strobe Full White Soft Pulse", "Strobo Full White", 65, true),
        AnimationSlot(47, "Strobe Full White Thunder Strike", "Strobo Full White", 90, true),

        AnimationSlot(48, "Smooth Breathe", "Napas & Pulse", 40),
        AnimationSlot(49, "Red Gradient Pulse", "Napas & Pulse", 46),
        AnimationSlot(50, "Green Gradient Pulse", "Napas & Pulse", 46),
        AnimationSlot(51, "Blue Gradient Pulse", "Napas & Pulse", 46),
        AnimationSlot(52, "Yellow Gradient Pulse", "Napas & Pulse", 46),
        AnimationSlot(53, "Cyan Gradient Pulse", "Napas & Pulse", 46),
        AnimationSlot(54, "Purple Gradient Pulse", "Napas & Pulse", 46),
        AnimationSlot(55, "Strobe RGB Flash", "Napas & Pulse", 85),

        AnimationSlot(56, "Kiri-Kanan Bergantian", "Zona Kanan-Kiri", 60),
        AnimationSlot(57, "Mirror Outward", "Zona Kanan-Kiri", 64),
        AnimationSlot(58, "Converge Inward", "Zona Kanan-Kiri", 64),
        AnimationSlot(59, "Dual Split Static", "Zona Kanan-Kiri", 50),
        AnimationSlot(60, "Hazard Dual Blink", "Zona Kanan-Kiri", 62),
        AnimationSlot(61, "Sein Kiri Sequential", "Zona Kanan-Kiri", 72),
        AnimationSlot(62, "Sein Kanan Sequential", "Zona Kanan-Kiri", 72),
        AnimationSlot(63, "Welcome Sweep", "Zona Kanan-Kiri", 55),
        AnimationSlot(64, "Brake Flash Pulse", "Zona Kanan-Kiri", 80),
        AnimationSlot(65, "Drift Side Accent", "Zona Kanan-Kiri", 66),
        AnimationSlot(66, "Stereo VU Meter", "Zona Kanan-Kiri", 70),
        AnimationSlot(67, "Crossfade Duel", "Zona Kanan-Kiri", 48),
        AnimationSlot(68, "Ping-Pong Bridge", "Zona Kanan-Kiri", 72),
        AnimationSlot(69, "Wingbeat", "Zona Kanan-Kiri", 58),
        AnimationSlot(70, "Dual Aurora", "Zona Kanan-Kiri", 46),
        AnimationSlot(71, "Split Fire & Ice", "Zona Kanan-Kiri", 54),
        AnimationSlot(72, "Split Cyan & Amber", "Zona Kanan-Kiri", 52),
        AnimationSlot(73, "Split Warm & Cool", "Zona Kanan-Kiri", 42),
        AnimationSlot(74, "Alternating White-Color", "Zona Kanan-Kiri", 78),
        AnimationSlot(75, "Twin Vortex", "Zona Kanan-Kiri", 68),

        AnimationSlot(76, "Fire 2012 Heat", "Prosedural FX", 65),
        AnimationSlot(77, "Pacifica Deep Blue", "Prosedural FX", 44),
        AnimationSlot(78, "Twinkle Stars", "Prosedural FX", 55),
        AnimationSlot(79, "Confetti Pop", "Prosedural FX", 68),
        AnimationSlot(80, "Juggle 8-Dots", "Prosedural FX", 62),
        AnimationSlot(81, "Sinelon Trail", "Prosedural FX", 60),
        AnimationSlot(82, "BPM Rhythm", "Prosedural FX", 70),
        AnimationSlot(83, "Color Waves", "Prosedural FX", 50),
        AnimationSlot(84, "Noise Cloud", "Prosedural FX", 46),
        AnimationSlot(85, "Matrix Rain", "Prosedural FX", 72),
        AnimationSlot(86, "Lava Lamp", "Prosedural FX", 38),
        AnimationSlot(87, "Fairy Dust", "Prosedural FX", 58),
        AnimationSlot(88, "Snowfall", "Prosedural FX", 45),
        AnimationSlot(89, "Ember Rise", "Prosedural FX", 52),
        AnimationSlot(90, "Electric Arc", "Prosedural FX", 82),
        AnimationSlot(91, "Prism Shatter", "Prosedural FX", 66),
        AnimationSlot(92, "Kaleidoscope", "Prosedural FX", 58),
        AnimationSlot(93, "Disco Fever", "Prosedural FX", 78),
        AnimationSlot(94, "Retro Arcade", "Prosedural FX", 72),
        AnimationSlot(95, "Vaporwave Grid", "Prosedural FX", 54),
        AnimationSlot(96, "Bioluminescence", "Prosedural FX", 46),
        AnimationSlot(97, "Galaxy Spiral", "Prosedural FX", 50),
        AnimationSlot(98, "Magma Flow", "Prosedural FX", 56),
        AnimationSlot(99, "Ice Crystal", "Prosedural FX", 48),
        AnimationSlot(100, "Spectrum Finale", "Prosedural FX", 75)
    )

    private val dmxNamed = listOf(
        "Forward Dreaming" to "Dreaming DMX",
        "Backward Dreaming" to "Dreaming DMX",
        "Forward 7 Colors" to "Dreaming DMX",
        "Backward 7 Colors" to "Dreaming DMX",
        "Forward 3 Colors" to "Dreaming DMX",
        "Backward 3 Colors" to "Dreaming DMX",
        "Forward Rainbow Chase" to "Chase DMX",
        "Backward Rainbow Chase" to "Chase DMX",
        "Center Outward Curtain" to "Curtain DMX",
        "Edge Inward Curtain" to "Curtain DMX",
        "Full White Strobe 1Hz" to "Strobo Full White DMX",
        "Full White Strobe 4Hz" to "Strobo Full White DMX",
        "Full White Strobe Double Flash" to "Strobo Full White DMX",
        "Full White Strobe Triple Burst" to "Strobo Full White DMX",
        "Full White Strobe Alternating L/R" to "Strobo Full White DMX",
        "Full White Strobe Blitz 12Hz" to "Strobo Full White DMX",
        "Full White Wing-Tip Strobe" to "Strobo Full White DMX",
        "Dual Meteor Cross" to "Meteor DMX",
        "Tricolor Jump Matrix" to "Jump DMX",
        "Seven-Color Jump Matrix" to "Jump DMX"
    )

    private val dmxGroups = listOf(
        "Dreaming DMX",
        "Flow & Chase DMX",
        "Curtain & Wipe DMX",
        "Strobo Full White DMX",
        "Gradient Breathe DMX",
        "Dual Zone L/R DMX",
        "Stage & Matrix DMX"
    )

    val dmxModes: List<DmxModeSlot> = List(210) { idx ->
        val id = idx + 1
        if (id <= dmxNamed.size) {
            DmxModeSlot(id, dmxNamed[id - 1].first, dmxNamed[id - 1].second)
        } else {
            val groupIdx = ((idx - dmxNamed.size) / 28) % dmxGroups.size
            val group = dmxGroups[groupIdx]
            val variant = ((idx - dmxNamed.size) % 28) + 1
            val dir = if (variant % 2 == 0) "Reverse" else "Forward"
            if (group == "Strobo Full White DMX") {
                DmxModeSlot(id, "Full White Strobe Pattern #$variant", group)
            } else {
                DmxModeSlot(id, "${group.removeSuffix(" DMX")} $dir #${variant.toString().padStart(2, '0')}", group)
            }
        }
    }

    fun byId(id: Int): AnimationSlot =
        slots.firstOrNull { it.id == id.coerceIn(1, 100) } ?: slots.first()

    fun dmxById(id: Int): DmxModeSlot =
        dmxModes.firstOrNull { it.id == id.coerceIn(1, 210) } ?: dmxModes.first()
}
