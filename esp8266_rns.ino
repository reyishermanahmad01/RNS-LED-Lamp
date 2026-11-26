/*
 * RNS (RGB LED Multi Controller) — Firmware ESP8266 (Wemos D1 Mini / NodeMCU)
 * Fitur:
 *  - Wi-Fi AP (RNS-ESP8266-XXXX) + Wi-Fi Router STA (EEPROM persisted)
 *  - UDP Port 4210 (JSON & 24-byte binary + RNS_DISCOVER) & HTTP Port 80 (/api/status, /api/control, /api/wifi, /api/pair, /api/security/*)
 *  - RNS Security Lock: Pairing resmi, token otorisasi controller, pencabutan akses & recovery key
 *  - Zona Kiri (D4 / GPIO2) & Zona Kanan (D2 / GPIO4) independen
 *  - Driver: WS2812B, WS2811, SK6812 RGB, SK6812 RGBW, Analog RGB (PWM)
 *  - 100 Slot Animasi + Mode Manual & Auto-Cycle
 */

#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <WiFiUdp.h>
#include <EEPROM.h>
#include <ArduinoJson.h>
#include <Adafruit_NeoPixel.h>

#define EEPROM_SIZE 1024
#define EEPROM_SEC_OFFSET 128
#define MAX_TRUSTED 5
#define SEC_MAGIC 0x52534532
#define UDP_PORT 4210
#define MAX_PIXELS 150

// Default pins Wemos D1 Mini
#define DEFAULT_LEFT_PIN   2   // D4
#define DEFAULT_RIGHT_PIN  4   // D2
#define DEFAULT_PWM_R      14  // D5
#define DEFAULT_PWM_G      12  // D6
#define DEFAULT_PWM_B      13  // D7

enum DriverType : uint8_t {
  DRV_WS2812B = 0,
  DRV_WS2811 = 1,
  DRV_SK6812_RGB = 2,
  DRV_SK6812_RGBW = 3,
  DRV_ANALOG_RGB = 4
};

struct ZoneState {
  bool enabled = true;
  bool customColor = false;
  uint8_t r = 0;
  uint8_t g = 180;
  uint8_t b = 255;
  uint8_t w = 0;
  uint8_t pal[4][3] = {
    {0, 180, 255},
    {255, 60, 180},
    {0, 255, 160},
    {255, 200, 0}
  };
  uint8_t brightness = 200;
  uint8_t animId = 2; // 1..100
  uint16_t pixelCount = 24;
};

struct WifiCreds {
  uint32_t magic = 0x524E5331;
  char mode[8] = "AP";
  char ssid[33] = "";
  char pass[65] = "";
};

struct TrustedController {
  bool active = false;
  bool revoked = false;
  char cid[44] = "";
  char tok[36] = "";
};

struct SecurityStore {
  uint32_t magic = SEC_MAGIC;
  bool secEnabled = false;
  char ownerPin[16] = "1234";
  char recoveryKey[24] = "";
  TrustedController trusted[MAX_TRUSTED];
};

struct RnsState {
  uint8_t moduleSlot = 1;
  bool powerOn = true;
  uint8_t masterBrightness = 220;
  uint8_t speed = 60;
  bool isDmx = false;
  uint16_t dmxModeId = 1; // 1..210
  bool autoCycle = false;
  uint16_t autoCycleSec = 10;
  bool splitZones = false;
  DriverType driver = DRV_WS2812B;
  uint8_t leftPin = DEFAULT_LEFT_PIN;
  uint8_t rightPin = DEFAULT_RIGHT_PIN;
  uint8_t pwmR = DEFAULT_PWM_R;
  uint8_t pwmG = DEFAULT_PWM_G;
  uint8_t pwmB = DEFAULT_PWM_B;
  ZoneState left;
  ZoneState right;
};

RnsState state;
WifiCreds wifiCreds;
SecurityStore secStore;
ESP8266WebServer server(80);
WiFiUDP udp;

Adafruit_NeoPixel stripLeft(MAX_PIXELS, DEFAULT_LEFT_PIN, NEO_GRB + NEO_KHZ800);
Adafruit_NeoPixel stripRight(MAX_PIXELS, DEFAULT_RIGHT_PIN, NEO_GRB + NEO_KHZ800);

unsigned long lastFrameMs = 0;
unsigned long lastAutoCycleMs = 0;
char udpBuffer[2048];

void generateRecoveryKeyStr(char* out24) {
  uint32_t chip = ESP.getChipId();
  uint32_t r1 = (os_random() ^ (chip << 5) ^ millis()) & 0xFFFF;
  uint32_t r2 = (os_random() ^ chip ^ micros()) & 0xFFFF;
  snprintf(out24, 24, "RNS-REC-%04X-%04X", (unsigned)r1, (unsigned)r2);
}

void generateHexToken(char* out36) {
  static const char hex[] = "0123456789ABCDEF";
  for (int i = 0; i < 32; i++) {
    uint32_t v = os_random() ^ micros() ^ (uint32_t)(i * 2654435761UL);
    out36[i] = hex[v & 0x0F];
  }
  out36[32] = '\0';
}

void saveSecurityStore() {
  EEPROM.put(EEPROM_SEC_OFFSET, secStore);
  EEPROM.commit();
}

void loadSecurityStore() {
  EEPROM.get(EEPROM_SEC_OFFSET, secStore);
  if (secStore.magic != SEC_MAGIC) {
    secStore = SecurityStore();
    secStore.magic = SEC_MAGIC;
    secStore.secEnabled = false;
    strlcpy(secStore.ownerPin, "1234", sizeof(secStore.ownerPin));
    generateRecoveryKeyStr(secStore.recoveryKey);
    saveSecurityStore();
    Serial.print(F("[RNS-SEC] Recovery Key inisial: "));
    Serial.println(secStore.recoveryKey);
  }
}

bool constantTimeEquals(const char* a, const char* b) {
  if (!a || !b) return false;
  size_t la = strlen(a);
  size_t lb = strlen(b);
  if (la == 0 || la != lb) return false;
  uint8_t diff = 0;
  for (size_t i = 0; i < la; i++) {
    diff |= (uint8_t)(a[i] ^ b[i]);
  }
  return diff == 0;
}

bool isAuthorized(const char* cid, const char* tok) {
  if (!secStore.secEnabled) return true;
  if (!cid || !tok || strlen(cid) == 0 || strlen(tok) == 0) return false;
  for (int i = 0; i < MAX_TRUSTED; i++) {
    if (secStore.trusted[i].active && !secStore.trusted[i].revoked) {
      if (strcmp(secStore.trusted[i].cid, cid) == 0 && constantTimeEquals(secStore.trusted[i].tok, tok)) {
        return true;
      }
    }
  }
  return false;
}

int countActiveTrusted() {
  int cnt = 0;
  for (int i = 0; i < MAX_TRUSTED; i++) {
    if (secStore.trusted[i].active && !secStore.trusted[i].revoked) cnt++;
  }
  return cnt;
}

void applyDriverConfig() {
  if (state.driver == DRV_ANALOG_RGB) {
    pinMode(state.pwmR, OUTPUT);
    pinMode(state.pwmG, OUTPUT);
    pinMode(state.pwmB, OUTPUT);
    analogWriteRange(255);
    return;
  }

  neoPixelType pixelFormat = NEO_GRB + NEO_KHZ800;
  switch (state.driver) {
    case DRV_WS2812B:    pixelFormat = NEO_GRB + NEO_KHZ800; break;
    case DRV_WS2811:     pixelFormat = NEO_BRG + NEO_KHZ400; break;
    case DRV_SK6812_RGB: pixelFormat = NEO_GRB + NEO_KHZ800; break;
    case DRV_SK6812_RGBW:pixelFormat = NEO_GRBW + NEO_KHZ800; break;
    default: break;
  }

  stripLeft.setPin(state.leftPin);
  stripLeft.updateType(pixelFormat);
  stripLeft.updateLength(constrain(state.left.pixelCount, 1, MAX_PIXELS));

  stripRight.setPin(state.rightPin);
  stripRight.updateType(pixelFormat);
  stripRight.updateLength(constrain(state.right.pixelCount, 1, MAX_PIXELS));
}

DriverType parseDriverName(const char* name) {
  if (!name) return DRV_WS2812B;
  if (strcmp(name, "WS2811") == 0) return DRV_WS2811;
  if (strcmp(name, "SK6812_RGB") == 0) return DRV_SK6812_RGB;
  if (strcmp(name, "SK6812_RGBW") == 0) return DRV_SK6812_RGBW;
  if (strcmp(name, "ANALOG_RGB") == 0) return DRV_ANALOG_RGB;
  return DRV_WS2812B;
}

const char* driverToName(DriverType d) {
  switch (d) {
    case DRV_WS2811: return "WS2811";
    case DRV_SK6812_RGB: return "SK6812_RGB";
    case DRV_SK6812_RGBW: return "SK6812_RGBW";
    case DRV_ANALOG_RGB: return "ANALOG_RGB";
    default: return "WS2812B";
  }
}

void hsvToRgb(float h, float s, float v, uint8_t& outR, uint8_t& outG, uint8_t& outB) {
  int i = int(h * 6.0f);
  float f = h * 6.0f - i;
  float p = v * (1.0f - s);
  float q = v * (1.0f - f * s);
  float t = v * (1.0f - (1.0f - f) * s);
  float r = 0, g = 0, b = 0;
  switch (((i % 6) + 6) % 6) {
    case 0: r = v; g = t; b = p; break;
    case 1: r = q; g = v; b = p; break;
    case 2: r = p; g = v; b = t; break;
    case 3: r = p; g = q; b = v; break;
    case 4: r = t; g = p; b = v; break;
    case 5: r = v; g = p; b = q; break;
  }
  outR = (uint8_t)constrain((int)(r * 255.0f), 0, 255);
  outG = (uint8_t)constrain((int)(g * 255.0f), 0, 255);
  outB = (uint8_t)constrain((int)(b * 255.0f), 0, 255);
}

void lerpRgbTuple(
  uint8_t ar, uint8_t ag, uint8_t ab,
  uint8_t br, uint8_t bg, uint8_t bb,
  float k,
  uint8_t& outR, uint8_t& outG, uint8_t& outB
) {
  float kk = constrain(k, 0.0f, 1.0f);
  outR = (uint8_t)(ar + (br - ar) * kk);
  outG = (uint8_t)(ag + (bg - ag) * kk);
  outB = (uint8_t)(ab + (bb - ab) * kk);
}

void sampleZonePaletteSmooth(
  const ZoneState& z, float phase, float lum, float pastelMix,
  uint8_t& outR, uint8_t& outG, uint8_t& outB
) {
  float p = fmodf(phase, 1.0f);
  if (p < 0) p += 1.0f;
  float scaled = p * 4.0f;
  int idx = ((int)scaled) % 4;
  int nextIdx = (idx + 1) % 4;
  float frac = scaled - floorf(scaled);

  uint8_t mr, mg, mb;
  lerpRgbTuple(
    z.pal[idx][0], z.pal[idx][1], z.pal[idx][2],
    z.pal[nextIdx][0], z.pal[nextIdx][1], z.pal[nextIdx][2],
    frac, mr, mg, mb
  );
  if (pastelMix > 0.0f) {
    mr = (uint8_t)(mr + (255 - mr) * pastelMix);
    mg = (uint8_t)(mg + (255 - mg) * pastelMix);
    mb = (uint8_t)(mb + (255 - mb) * pastelMix);
  }
  outR = (uint8_t)constrain((int)(mr * lum), 0, 255);
  outG = (uint8_t)constrain((int)(mg * lum), 0, 255);
  outB = (uint8_t)constrain((int)(mb * lum), 0, 255);
}

void computeWhiteStrobe(
  int variant, float pos, float t, bool isRight, const ZoneState& z,
  uint8_t& outR, uint8_t& outG, uint8_t& outB
) {
  float cyc = fmodf(t * 2.2f, 1.0f);
  if (cyc < 0) cyc += 1.0f;
  bool lit = false;
  bool useAlt = false;
  int v = ((variant % 12) + 12) % 12;

  switch (v) {
    case 0: lit = (((int)(t * 6.0f) % 2) == 0); break;
    case 1: lit = (((int)(t * 14.0f) % 2) == 0); break;
    case 2: lit = cyc < 0.12f || (cyc >= 0.22f && cyc < 0.34f); useAlt = cyc >= 0.22f; break;
    case 3: lit = cyc < 0.09f || (cyc >= 0.17f && cyc < 0.26f) || (cyc >= 0.34f && cyc < 0.43f); useAlt = (cyc >= 0.17f && cyc < 0.26f); break;
    case 4: lit = cyc < 0.65f && (((int)(cyc * 12.0f) % 2) == 0); useAlt = (((int)(cyc * 6.0f) % 2) == 1); break;
    case 5: lit = ((((int)(t * 5.0f) % 2) == 0) != isRight); useAlt = isRight; break;
    case 6: {
      float head = fmodf(t * 1.4f, 1.0f);
      lit = fabsf(pos - head) < 0.18f;
      useAlt = head > 0.5f;
      break;
    }
    case 7: lit = (((int)(t * 22.0f) % 2) == 0); break;
    case 8: {
      float seed = sinf(pos * 41.7f + floorf(t * 9.0f) * 19.3f) * 43758.5453f;
      float r = seed - floorf(seed);
      lit = r > 0.65f;
      useAlt = r > 0.82f;
      break;
    }
    case 9:
      lit = isRight ? (cyc < 0.14f || (cyc >= 0.2f && cyc < 0.32f)) : (cyc >= 0.5f && (cyc < 0.64f || (cyc >= 0.7f && cyc < 0.82f)));
      useAlt = isRight;
      break;
    case 10: {
      float p = sinf(t * 4.0f) * 0.5f + 0.5f;
      if (z.customColor) {
        lerpRgbTuple(z.pal[1][0], z.pal[1][1], z.pal[1][2], z.r, z.g, z.b, p, outR, outG, outB);
        outR = (uint8_t)(outR * p); outG = (uint8_t)(outG * p); outB = (uint8_t)(outB * p);
      } else {
        uint8_t val = (uint8_t)(p * 255.0f);
        outR = val; outG = val; outB = val;
      }
      return;
    }
    default:
      lit = cyc < 0.06f || (cyc >= 0.1f && cyc < 0.18f) || (cyc >= 0.25f && cyc < 0.29f);
      useAlt = (cyc >= 0.1f && cyc < 0.18f);
      break;
  }

  if (!lit) { outR = 0; outG = 0; outB = 0; return; }
  if (!z.customColor) { outR = 255; outG = 255; outB = 255; return; }
  if (useAlt) { outR = z.pal[1][0]; outG = z.pal[1][1]; outB = z.pal[1][2]; }
  else { outR = z.r; outG = z.g; outB = z.b; }
}

void computeAnimationPixel(
  const ZoneState& z, uint16_t idx, float pos, float t, bool isRight,
  bool isDmx, uint16_t dmxModeId,
  uint8_t& outR, uint8_t& outG, uint8_t& outB
) {
  uint8_t baseR = z.r, baseG = z.g, baseB = z.b;
  uint8_t c2R = z.pal[1][0], c2G = z.pal[1][1], c2B = z.pal[1][2];

  if (isDmx) {
    uint16_t anim = constrain((int)dmxModeId, 1, 210);
    if (anim >= 11 && anim <= 17) {
      computeWhiteStrobe(anim - 11, pos, t, isRight, z, outR, outG, outB);
      return;
    }
    if (anim >= 105 && anim <= 132) {
      computeWhiteStrobe(anim - 105, pos, t, isRight, z, outR, outG, outB);
      return;
    }
    float dir = (anim % 2 == 0) ? -1.0f : 1.0f;
    float hue = fmodf(pos * (1 + (anim % 3)) + dir * t * 0.4f + anim * 0.037f, 1.0f);
    if (hue < 0) hue += 1.0f;
    float wave = (anim % 5 == 0)
      ? (((int)floorf(fmodf(t * 3.0f + pos * 4.0f, 2.0f)) == 0) ? 1.0f : 0.1f)
      : (0.45f + 0.55f * (sinf(t * 2.8f * dir + pos * 6.28318f) * 0.5f + 0.5f));
    if (z.customColor) {
      sampleZonePaletteSmooth(z, hue, wave, (anim % 7 == 0) ? 0.25f : 0.0f, outR, outG, outB);
    } else {
      hsvToRgb(hue, (anim % 7 == 0) ? 0.65f : 1.0f, wave, outR, outG, outB);
    }
    return;
  }

  uint8_t anim = z.animId;
  if (anim == 1) {
    outR = baseR; outG = baseG; outB = baseB;
  } else if (anim >= 2 && anim <= 15) {
    if (anim == 2) {
      int step = ((int)floorf(t * 2.4f)) % 3;
      if (z.customColor) {
        outR = z.pal[step][0]; outG = z.pal[step][1]; outB = z.pal[step][2];
      } else {
        if (step == 0) { outR = 255; outG = 0; outB = 0; }
        else if (step == 1) { outR = 0; outG = 255; outB = 0; }
        else { outR = 0; outG = 80; outB = 255; }
      }
      return;
    }
    if (anim == 3) {
      int step = ((int)floorf(t * 2.4f)) % 7;
      if (z.customColor) {
        sampleZonePaletteSmooth(z, step / 7.0f, 1.0f, 0.0f, outR, outG, outB);
      } else {
        hsvToRgb(step / 7.0f, 1.0f, 1.0f, outR, outG, outB);
      }
      return;
    }
    float dir = (anim == 7) ? -t : t;
    float phase = fmodf(pos * (anim == 6 ? 2.0f : 1.0f) + dir * 0.35f + (anim - 2) * 0.08f, 1.0f);
    if (phase < 0) phase += 1.0f;
    if (z.customColor) {
      sampleZonePaletteSmooth(z, phase, 1.0f, (anim == 14) ? 0.35f : 0.0f, outR, outG, outB);
    } else {
      float sat = (anim == 14) ? 0.55f : 1.0f;
      hsvToRgb(phase, sat, 1.0f, outR, outG, outB);
    }
  } else if (anim >= 16 && anim <= 35) {
    if (anim == 18 || anim == 33) {
      float head = sinf(t * 2.4f) * 0.5f + 0.5f;
      float dist = fabsf(pos - head);
      float intensity = fmaxf(0.0f, 1.0f - dist * 5.5f);
      if (z.customColor) {
        lerpRgbTuple(c2R, c2G, c2B, baseR, baseG, baseB, fminf(1.0f, intensity * 1.4f), outR, outG, outB);
        outR = (uint8_t)(outR * intensity); outG = (uint8_t)(outG * intensity); outB = (uint8_t)(outB * intensity);
      } else {
        outR = (uint8_t)(baseR * intensity); outG = (uint8_t)(baseG * intensity); outB = (uint8_t)(baseB * intensity);
      }
    } else if (anim == 25) {
      bool flash = (((int)(t * 5.0f) % 2) == 0) != isRight;
      if (z.customColor) {
        if (flash) { outR = baseR; outG = baseG; outB = baseB; }
        else { outR = c2R; outG = c2G; outB = c2B; }
      } else {
        if (flash) { outR = 255; outG = 15; outB = 25; }
        else { outR = 15; outG = 90; outB = 255; }
      }
    } else if (anim == 26) {
      bool on = sinf(t * 6.0f) > 0;
      if (z.customColor) {
        if (on) { outR = baseR; outG = baseG; outB = baseB; }
        else { outR = (uint8_t)(c2R * 0.08f); outG = (uint8_t)(c2G * 0.08f); outB = (uint8_t)(c2B * 0.08f); }
      } else {
        if (on) { outR = 255; outG = 155; outB = 0; }
        else { outR = 15; outG = 8; outB = 0; }
      }
    } else {
      float head = fmodf(t * 0.65f + (anim % 5) * 0.1f, 1.0f);
      float dist = fmodf(pos - head + 1.0f, 1.0f);
      float tail = fmaxf(0.0f, 1.0f - dist * ((anim == 19 || anim == 20) ? 3.0f : 4.2f));
      if (anim == 20) {
        if (z.customColor) sampleZonePaletteSmooth(z, pos + t * 0.3f, tail, 0.0f, outR, outG, outB);
        else hsvToRgb(fmodf(pos + t * 0.3f, 1.0f), 1.0f, tail, outR, outG, outB);
      } else if (z.customColor && (anim % 2 == 1)) {
        lerpRgbTuple(c2R, c2G, c2B, baseR, baseG, baseB, tail, outR, outG, outB);
        outR = (uint8_t)(outR * tail); outG = (uint8_t)(outG * tail); outB = (uint8_t)(outB * tail);
      } else {
        outR = (uint8_t)(baseR * tail); outG = (uint8_t)(baseG * tail); outB = (uint8_t)(baseB * tail);
      }
    }
  } else if (anim >= 36 && anim <= 47) {
    computeWhiteStrobe(anim - 36, pos, t, isRight, z, outR, outG, outB);
  } else if (anim >= 48 && anim <= 55) {
    float wave = sinf(t * 2.2f) * 0.5f + 0.5f;
    if (anim == 55) {
      float wv = (((int)(t * 7.0f) % 2) == 0) ? 1.0f : 0.04f;
      if (z.customColor) {
        int pi = ((int)floorf(t * 2.5f)) % 4;
        outR = (uint8_t)(z.pal[pi][0] * wv); outG = (uint8_t)(z.pal[pi][1] * wv); outB = (uint8_t)(z.pal[pi][2] * wv);
      } else {
        hsvToRgb(fmodf((int)(t * 0.5f) * 0.17f, 1.0f), 1.0f, wv, outR, outG, outB);
      }
    } else if (z.customColor) {
      lerpRgbTuple(c2R, c2G, c2B, baseR, baseG, baseB, wave, outR, outG, outB);
      outR = (uint8_t)(outR * wave); outG = (uint8_t)(outG * wave); outB = (uint8_t)(outB * wave);
    } else {
      if (anim == 49) { outR = (uint8_t)(255 * wave); outG = 0; outB = 0; }
      else if (anim == 50) { outR = 0; outG = (uint8_t)(255 * wave); outB = 0; }
      else if (anim == 51) { outR = 0; outG = 40; outB = (uint8_t)(255 * wave); }
      else if (anim == 52) { outR = (uint8_t)(255 * wave); outG = (uint8_t)(215 * wave); outB = 0; }
      else if (anim == 53) { outR = 0; outG = (uint8_t)(230 * wave); outB = (uint8_t)(255 * wave); }
      else if (anim == 54) { outR = (uint8_t)(190 * wave); outG = 0; outB = (uint8_t)(255 * wave); }
      else { outR = (uint8_t)(baseR * wave); outG = (uint8_t)(baseG * wave); outB = (uint8_t)(baseB * wave); }
    }
  } else if (anim >= 56 && anim <= 75) {
    if (anim == 56) {
      bool active = (((int)(t * 2.0f) % 2) == 0) != isRight;
      if (active) { outR = baseR; outG = baseG; outB = baseB; }
      else { outR = 0; outG = 0; outB = 0; }
    } else if (anim == 61) {
      if (!isRight && fmodf(t * 1.8f, 1.0f) >= pos) {
        if (z.customColor) { outR = baseR; outG = baseG; outB = baseB; }
        else { outR = 255; outG = 165; outB = 0; }
      } else { outR = 0; outG = 0; outB = 0; }
    } else if (anim == 62) {
      if (isRight && fmodf(t * 1.8f, 1.0f) >= pos) {
        if (z.customColor) { outR = baseR; outG = baseG; outB = baseB; }
        else { outR = 255; outG = 165; outB = 0; }
      } else { outR = 0; outG = 0; outB = 0; }
    } else if (anim == 71) {
      if (z.customColor) {
        float wv = 0.55f + 0.45f * sinf(t * 2.2f + pos * 3.0f);
        if (!isRight) lerpRgbTuple(baseR, baseG, baseB, c2R, c2G, c2B, wv, outR, outG, outB);
        else lerpRgbTuple(c2R, c2G, c2B, baseR, baseG, baseB, wv, outR, outG, outB);
      } else {
        if (!isRight) hsvToRgb(0.04f + sinf(t + pos) * 0.04f, 1.0f, 1.0f, outR, outG, outB);
        else hsvToRgb(0.55f + cosf(t + pos) * 0.05f, 1.0f, 1.0f, outR, outG, outB);
      }
    } else if (anim == 74) {
      bool flash = (((int)(t * 6.0f) % 2) == 0) != isRight;
      if (flash) {
        if (z.customColor) { outR = c2R; outG = c2G; outB = c2B; }
        else { outR = 255; outG = 255; outB = 255; }
      } else { outR = baseR; outG = baseG; outB = baseB; }
    } else {
      float phase = isRight ? t + 0.5f : t;
      float pulse = sinf(phase * 3.0f + pos * 4.0f) * 0.5f + 0.5f;
      if (z.customColor) {
        lerpRgbTuple(c2R, c2G, c2B, baseR, baseG, baseB, pulse, outR, outG, outB);
        outR = (uint8_t)(outR * pulse); outG = (uint8_t)(outG * pulse); outB = (uint8_t)(outB * pulse);
      } else {
        outR = (uint8_t)(baseR * pulse); outG = (uint8_t)(baseG * pulse); outB = (uint8_t)(baseB * pulse);
      }
    }
  } else {
    if (anim == 76 || anim == 89 || anim == 98) {
      float heat = constrain((1.0f - pos * 0.7f) * (0.65f + 0.35f * sinf(t * 5.0f + idx)), 0.0f, 1.0f);
      if (z.customColor) {
        lerpRgbTuple(baseR, baseG, baseB, c2R, c2G, c2B, heat, outR, outG, outB);
        float k = 0.25f + 0.75f * heat;
        outR = (uint8_t)(outR * k); outG = (uint8_t)(outG * k); outB = (uint8_t)(outB * k);
      } else {
        outR = 255; outG = (uint8_t)(heat * 145.0f); outB = (uint8_t)(heat * 20.0f);
      }
    } else if (anim == 77 || anim == 96) {
      float w = sinf(t * 1.6f + pos * 5.0f) * 0.5f + 0.5f;
      float v = 0.4f + 0.6f * w;
      if (z.customColor) {
        lerpRgbTuple(baseR, baseG, baseB, c2R, c2G, c2B, w, outR, outG, outB);
        outR = (uint8_t)(outR * v); outG = (uint8_t)(outG * v); outB = (uint8_t)(outB * v);
      } else {
        outR = 0; outG = (uint8_t)(140 * v); outB = (uint8_t)(255 * v);
      }
    } else if (anim == 78 || anim == 87 || anim == 88) {
      float seed = sinf(idx * 12.9898f + floorf(t * 4.0f) * 78.233f) * 43758.5453f;
      float rnd = seed - floorf(seed);
      float sp = (rnd > 0.78f) ? 1.0f : 0.08f;
      bool alt = z.customColor && rnd > 0.88f;
      outR = (uint8_t)((alt ? c2R : baseR) * sp);
      outG = (uint8_t)((alt ? c2G : baseG) * sp);
      outB = (uint8_t)((alt ? c2B : baseB) * sp);
    } else {
      float hue = fmodf(pos + t * 0.45f + (anim - 75) * 0.04f, 1.0f);
      if (hue < 0) hue += 1.0f;
      float lum = 0.45f + 0.55f * (cosf(t * 3.1f + idx * 0.5f) * 0.5f + 0.5f);
      if (z.customColor) sampleZonePaletteSmooth(z, hue, lum, 0.0f, outR, outG, outB);
      else hsvToRgb(hue, 0.92f, lum, outR, outG, outB);
    }
  }
}

void renderZoneToStrip(Adafruit_NeoPixel& strip, const ZoneState& z, bool isRight, float t) {
  uint16_t count = strip.numPixels();
  if (!state.powerOn || !z.enabled) {
    strip.clear();
    strip.show();
    return;
  }
  float scale = (state.masterBrightness / 255.0f) * (z.brightness / 255.0f);
  uint8_t wScaled = (state.driver == DRV_SK6812_RGBW) ? (uint8_t)(z.w * scale) : 0;
  for (uint16_t i = 0; i < count; i++) {
    float pos = (count > 1) ? (float)i / (count - 1) : 0.0f;
    float effPos = (isRight && !state.isDmx && (z.animId == 57 || z.animId == 58 || z.animId == 62 || z.animId == 69))
      ? (1.0f - pos) : pos;
    uint8_t r, g, b;
    computeAnimationPixel(z, i, effPos, t, isRight, state.isDmx, state.dmxModeId, r, g, b);
    strip.setPixelColor(i, (uint8_t)(r * scale), (uint8_t)(g * scale), (uint8_t)(b * scale), wScaled);
  }
  strip.show();
}

void renderAnalogRgb(float t) {
  const ZoneState& z = state.left;
  if (!state.powerOn || !z.enabled) {
    analogWrite(state.pwmR, 0);
    analogWrite(state.pwmG, 0);
    analogWrite(state.pwmB, 0);
    return;
  }
  uint8_t r, g, b;
  computeAnimationPixel(z, 0, 0.5f, t, false, state.isDmx, state.dmxModeId, r, g, b);
  float scale = (state.masterBrightness / 255.0f) * (z.brightness / 255.0f);
  analogWrite(state.pwmR, (int)(r * scale));
  analogWrite(state.pwmG, (int)(g * scale));
  analogWrite(state.pwmB, (int)(b * scale));
}

void parseZoneJson(JsonObject obj, ZoneState& z, bool& driverChanged) {
  if (obj.isNull()) return;
  z.enabled = obj["en"] | z.enabled;
  z.customColor = obj["ce"] | z.customColor;
  z.r = obj["r"] | z.r;
  z.g = obj["g"] | z.g;
  z.b = obj["b"] | z.b;
  z.w = obj["w"] | z.w;
  z.pal[0][0] = z.r; z.pal[0][1] = z.g; z.pal[0][2] = z.b;
  if (obj["r2"].is<int>()) z.pal[1][0] = constrain((int)obj["r2"], 0, 255);
  if (obj["g2"].is<int>()) z.pal[1][1] = constrain((int)obj["g2"], 0, 255);
  if (obj["b2"].is<int>()) z.pal[1][2] = constrain((int)obj["b2"], 0, 255);

  JsonArray palArr = obj["pal"];
  if (!palArr.isNull()) {
    int idx = 0;
    for (JsonArray c : palArr) {
      if (idx >= 4) break;
      if (c.size() >= 3) {
        z.pal[idx][0] = constrain((int)c[0], 0, 255);
        z.pal[idx][1] = constrain((int)c[1], 0, 255);
        z.pal[idx][2] = constrain((int)c[2], 0, 255);
      }
      idx++;
    }
  }

  z.brightness = obj["br"] | z.brightness;
  z.animId = constrain((int)(obj["anim"] | z.animId), 1, 100);
  uint16_t px = constrain((int)(obj["px"] | z.pixelCount), 1, MAX_PIXELS);
  if (px != z.pixelCount) { z.pixelCount = px; driverChanged = true; }
}

bool applyJsonCommand(const char* payload, size_t len) {
  JsonDocument doc;
  if (deserializeJson(doc, payload, len) != DeserializationError::Ok) return false;

  const char* cid = doc["cid"] | "";
  const char* tok = doc["tok"] | "";
  if (!isAuthorized(cid, tok)) {
    return false;
  }

  bool driverChanged = false;
  if (doc["pwr"].is<bool>()) state.powerOn = doc["pwr"];
  if (doc["br"].is<int>()) state.masterBrightness = constrain((int)doc["br"], 0, 255);
  if (doc["spd"].is<int>()) state.speed = constrain((int)doc["spd"], 1, 100);
  if (doc["engine"].is<const char*>()) {
    state.isDmx = (strcmp(doc["engine"], "DMX210") == 0);
  }
  if (doc["dmx"].is<int>()) state.dmxModeId = constrain((int)doc["dmx"], 1, 210);
  if (doc["auto"].is<bool>()) state.autoCycle = doc["auto"];
  if (doc["autoSec"].is<int>()) state.autoCycleSec = constrain((int)doc["autoSec"], 3, 120);
  if (doc["split"].is<bool>()) state.splitZones = doc["split"];

  if (doc["drv"].is<const char*>()) {
    DriverType nd = parseDriverName(doc["drv"]);
    if (nd != state.driver) { state.driver = nd; driverChanged = true; }
  }

  parseZoneJson(doc["left"], state.left, driverChanged);
  parseZoneJson(doc["right"], state.right, driverChanged);

  if (driverChanged) applyDriverConfig();
  return true;
}

void applyBinaryCommand(const uint8_t* data, size_t len) {
  if (len < 24 || data[0] != 0x52 || data[1] != 0x4E || data[2] != 0x53) return;
  uint8_t flags = data[5];
  state.powerOn = (flags & 0x01) != 0;
  state.autoCycle = (flags & 0x02) != 0;
  state.splitZones = (flags & 0x04) != 0;
  state.left.enabled = (flags & 0x08) != 0;
  state.right.enabled = (flags & 0x10) != 0;
  state.isDmx = (flags & 0x20) != 0;
  bool customBit = (flags & 0x40) != 0;
  state.left.customColor = customBit;
  state.right.customColor = customBit;

  state.masterBrightness = data[6];
  state.speed = constrain((int)data[7], 1, 100);
  DriverType nd = (DriverType)constrain((int)data[8], 0, 4);
  bool changed = (nd != state.driver);
  state.driver = nd;

  if (state.isDmx) {
    state.dmxModeId = constrain((int)data[9], 1, 210);
  } else {
    state.left.animId = constrain((int)data[9], 1, 100);
  }
  state.left.r = data[10]; state.left.g = data[11]; state.left.b = data[12]; state.left.w = data[13];
  state.left.pal[0][0] = data[10]; state.left.pal[0][1] = data[11]; state.left.pal[0][2] = data[12];
  state.left.brightness = data[14];

  if (!state.isDmx) {
    state.right.animId = constrain((int)data[15], 1, 100);
  }
  state.right.r = data[16]; state.right.g = data[17]; state.right.b = data[18]; state.right.w = data[19];
  state.right.pal[0][0] = data[16]; state.right.pal[0][1] = data[17]; state.right.pal[0][2] = data[18];
  state.right.brightness = data[20];

  if (data[21] != state.left.pixelCount || data[22] != state.right.pixelCount) {
    state.left.pixelCount = constrain((int)data[21], 1, MAX_PIXELS);
    state.right.pixelCount = constrain((int)data[22], 1, MAX_PIXELS);
    changed = true;
  }
  state.autoCycleSec = constrain((int)data[23], 3, 120);
  if (changed) applyDriverConfig();
}

void setupWifi() {
  EEPROM.begin(EEPROM_SIZE);
  loadSecurityStore();
  EEPROM.get(0, wifiCreds);
  if (wifiCreds.magic != 0x524E5331) {
    wifiCreds = WifiCreds();
  }

  String apName = "RNS-ESP8266-" + String(ESP.getChipId() & 0xFFFF, HEX);
  if (strcmp(wifiCreds.mode, "STA") == 0 && strlen(wifiCreds.ssid) > 0) {
    WiFi.mode(WIFI_STA);
    WiFi.begin(wifiCreds.ssid, wifiCreds.pass);
    unsigned long start = millis();
    while (WiFi.status() != WL_CONNECTED && millis() - start < 10000) {
      delay(250);
    }
    if (WiFi.status() == WL_CONNECTED) return;
  }

  WiFi.mode(WIFI_AP);
  WiFi.softAP(apName.c_str(), "12345678");
}

void setupHttpEndpoints() {
  server.on("/api/status", HTTP_GET, []() {
    server.sendHeader("Access-Control-Allow-Origin", "*");
    JsonDocument doc;
    doc["board"] = "ESP8266";
    doc["ip"] = (WiFi.getMode() == WIFI_STA) ? WiFi.localIP().toString() : WiFi.softAPIP().toString();
    doc["driver"] = driverToName(state.driver);
    doc["pwr"] = state.powerOn;
    doc["br"] = state.masterBrightness;
    doc["engine"] = state.isDmx ? "DMX210" : "ANIM100";
    doc["dmx"] = state.dmxModeId;
    doc["animLeft"] = state.left.animId;
    doc["animRight"] = state.right.animId;
    doc["customColor"] = state.left.customColor || state.right.customColor;
    doc["secEnabled"] = secStore.secEnabled;
    doc["trustedCount"] = countActiveTrusted();
    String out;
    serializeJson(doc, out);
    server.send(200, "application/json", out);
  });

  server.on("/api/control", HTTP_POST, []() {
    server.sendHeader("Access-Control-Allow-Origin", "*");
    String body = server.arg("plain");
    if (!applyJsonCommand(body.c_str(), body.length())) {
      server.send(401, "application/json", "{\"ok\":false,\"error\":\"unauthorized\"}");
      return;
    }
    server.send(200, "application/json", "{\"ok\":true}");
  });

  server.on("/api/wifi", HTTP_POST, []() {
    server.sendHeader("Access-Control-Allow-Origin", "*");
    JsonDocument doc;
    if (deserializeJson(doc, server.arg("plain")) != DeserializationError::Ok) {
      server.send(400, "application/json", "{\"error\":\"invalid_json\"}");
      return;
    }
    const char* cid = doc["cid"] | "";
    const char* tok = doc["tok"] | "";
    if (!isAuthorized(cid, tok)) {
      server.send(401, "application/json", "{\"ok\":false,\"error\":\"unauthorized\"}");
      return;
    }
    const char* mode = doc["mode"] | "AP";
    const char* ssid = doc["ssid"] | "";
    const char* pass = doc["pass"] | "";
    wifiCreds.magic = 0x524E5331;
    strlcpy(wifiCreds.mode, mode, sizeof(wifiCreds.mode));
    strlcpy(wifiCreds.ssid, ssid, sizeof(wifiCreds.ssid));
    strlcpy(wifiCreds.pass, pass, sizeof(wifiCreds.pass));
    EEPROM.put(0, wifiCreds);
    EEPROM.commit();
    server.send(200, "application/json", "{\"ok\":true,\"restarting\":true}");
    delay(500);
    ESP.restart();
  });

  server.on("/api/pair", HTTP_POST, []() {
    server.sendHeader("Access-Control-Allow-Origin", "*");
    JsonDocument doc;
    if (deserializeJson(doc, server.arg("plain")) != DeserializationError::Ok) {
      server.send(400, "application/json", "{\"ok\":false,\"error\":\"invalid_json\"}");
      return;
    }
    const char* cid = doc["cid"] | "";
    const char* pin = doc["pin"] | "";
    if (strlen(cid) == 0 || !constantTimeEquals(pin, secStore.ownerPin)) {
      server.send(401, "application/json", "{\"ok\":false,\"token\":\"\",\"uid\":\"\",\"error\":\"PIN pairing salah\"}");
      return;
    }

    int targetSlot = -1;
    for (int i = 0; i < MAX_TRUSTED; i++) {
      if (secStore.trusted[i].active && strcmp(secStore.trusted[i].cid, cid) == 0) {
        targetSlot = i;
        break;
      }
    }
    if (targetSlot < 0) {
      for (int i = 0; i < MAX_TRUSTED; i++) {
        if (!secStore.trusted[i].active || secStore.trusted[i].revoked) {
          targetSlot = i;
          break;
        }
      }
    }
    if (targetSlot < 0) targetSlot = MAX_TRUSTED - 1;

    secStore.trusted[targetSlot].active = true;
    secStore.trusted[targetSlot].revoked = false;
    strlcpy(secStore.trusted[targetSlot].cid, cid, sizeof(secStore.trusted[targetSlot].cid));
    generateHexToken(secStore.trusted[targetSlot].tok);
    secStore.secEnabled = true;
    saveSecurityStore();

    JsonDocument resp;
    resp["ok"] = true;
    resp["token"] = secStore.trusted[targetSlot].tok;
    resp["uid"] = "ESP8266-" + String(ESP.getChipId(), HEX);
    String out;
    serializeJson(resp, out);
    server.send(200, "application/json", out);
  });

  server.on("/api/security/revoke", HTTP_POST, []() {
    server.sendHeader("Access-Control-Allow-Origin", "*");
    JsonDocument doc;
    if (deserializeJson(doc, server.arg("plain")) != DeserializationError::Ok) {
      server.send(400, "application/json", "{\"ok\":false,\"error\":\"invalid_json\"}");
      return;
    }
    const char* cid = doc["cid"] | "";
    const char* tok = doc["tok"] | "";
    const char* targetCid = doc["targetCid"] | "";
    if (!isAuthorized(cid, tok)) {
      server.send(401, "application/json", "{\"ok\":false,\"error\":\"unauthorized\"}");
      return;
    }
    for (int i = 0; i < MAX_TRUSTED; i++) {
      if (secStore.trusted[i].active && strcmp(secStore.trusted[i].cid, targetCid) == 0) {
        secStore.trusted[i].active = false;
        secStore.trusted[i].revoked = true;
        secStore.trusted[i].tok[0] = '\0';
      }
    }
    saveSecurityStore();
    server.send(200, "application/json", "{\"ok\":true}");
  });

  server.on("/api/security/recover", HTTP_POST, []() {
    server.sendHeader("Access-Control-Allow-Origin", "*");
    JsonDocument doc;
    if (deserializeJson(doc, server.arg("plain")) != DeserializationError::Ok) {
      server.send(400, "application/json", "{\"ok\":false,\"error\":\"invalid_json\"}");
      return;
    }
    String keyIn = doc["recoveryKey"] | "";
    keyIn.trim();
    keyIn.toUpperCase();
    const char* newPin = doc["newPin"] | "";
    if (!constantTimeEquals(keyIn.c_str(), secStore.recoveryKey) || strlen(newPin) < 4) {
      server.send(401, "application/json", "{\"ok\":false,\"error\":\"recovery_key_atau_pin_tidak_valid\"}");
      return;
    }
    strlcpy(secStore.ownerPin, newPin, sizeof(secStore.ownerPin));
    for (int i = 0; i < MAX_TRUSTED; i++) {
      secStore.trusted[i].active = false;
      secStore.trusted[i].revoked = true;
      secStore.trusted[i].tok[0] = '\0';
    }
    generateRecoveryKeyStr(secStore.recoveryKey);
    saveSecurityStore();

    JsonDocument resp;
    resp["ok"] = true;
    resp["newRecoveryKey"] = secStore.recoveryKey;
    String out;
    serializeJson(resp, out);
    server.send(200, "application/json", out);
  });

  server.begin();
}

void setup() {
  Serial.begin(115200);
  setupWifi();
  stripLeft.begin();
  stripRight.begin();
  applyDriverConfig();
  setupHttpEndpoints();
  udp.begin(UDP_PORT);
}

void loop() {
  server.handleClient();

  int packetSize = udp.parsePacket();
  if (packetSize > 0) {
    int len = udp.read(udpBuffer, sizeof(udpBuffer) - 1);
    if (len > 0) {
      udpBuffer[len] = '\0';
      if (strncmp(udpBuffer, "RNS_DISCOVER", 12) == 0) {
        String hello = "RNS_HELLO|ESP8266|RNS-ESP8266|" + String(driverToName(state.driver));
        udp.beginPacket(udp.remoteIP(), udp.remotePort());
        udp.write((const uint8_t*)hello.c_str(), hello.length());
        udp.endPacket();
      } else if (len >= 24 && udpBuffer[0] == 0x52 && udpBuffer[1] == 0x4E && udpBuffer[2] == 0x53) {
        if (!secStore.secEnabled) {
          applyBinaryCommand((const uint8_t*)udpBuffer, len);
        }
      } else {
        applyJsonCommand(udpBuffer, len);
      }
    }
  }

  unsigned long now = millis();
  if (state.autoCycle && state.powerOn && (now - lastAutoCycleMs >= (unsigned long)state.autoCycleSec * 1000UL)) {
    lastAutoCycleMs = now;
    if (state.isDmx) {
      state.dmxModeId = (state.dmxModeId % 210) + 1;
    } else {
      state.left.animId = (state.left.animId % 100) + 1;
      state.right.animId = state.splitZones ? ((state.right.animId % 100) + 1) : state.left.animId;
    }
  }

  if (now - lastFrameMs >= 20) { // ~50 FPS
    lastFrameMs = now;
    float spd = 0.35f + (state.speed / 100.0f) * 2.6f;
    float t = (now / 1000.0f) * spd;
    if (state.driver == DRV_ANALOG_RGB) {
      renderAnalogRgb(t);
    } else {
      renderZoneToStrip(stripLeft, state.left, false, t);
      const ZoneState& rightSrc = state.splitZones ? state.right : state.left;
      renderZoneToStrip(stripRight, rightSrc, true, t);
    }
  }
}
