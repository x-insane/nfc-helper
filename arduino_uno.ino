#define FASTLED_INTERNAL
#include <FastLED.h>
#include <SPI.h>
#include <MFRC522.h>

#define PT_USE_TIMER
#define PT_USE_SEM

#include "pt.h"
static struct pt thread_led, thread_rc522;
static struct pt_sem sem_LED;

// LED config
#define LED_PIN     6
#define NUM_LEDS    64
#define BRIGHTNESS  12
#define LED_TYPE    WS2812B
#define COLOR_ORDER GRB
#define UPDATES_PER_SECOND 200
CRGB leds[NUM_LEDS];
CRGBPalette16 currentPalette;
TBlendType     currentBlending;
extern CRGBPalette16 myRedWhiteBluePalette;
extern const TProgmemPalette16 myRedWhiteBluePalette_p PROGMEM;

// RC522 config
#define RST_PIN          9          // Configurable, see typical pin layout above
#define SS_PIN          10          // Configurable, see typical pin layout above
MFRC522 mfrc522(SS_PIN, RST_PIN);  // Create MFRC522 instance.
MFRC522::MIFARE_Key key;

void setup() {
    delay(3000);
    
    // LED init
    FastLED.addLeds<LED_TYPE, LED_PIN, COLOR_ORDER>(leds, NUM_LEDS).setCorrection(TypicalLEDStrip);
    FastLED.addLeds<LED_TYPE, LED_PIN, COLOR_ORDER>(leds, NUM_LEDS).setCorrection( TypicalSMD5050);
    FastLED.setBrightness(BRIGHTNESS);
    currentPalette = RainbowColors_p;
    currentBlending = LINEARBLEND;

    // RC522 init
    Serial.begin(9600); // Initialize serial communications with the PC
    while (!Serial);    // Do nothing if no serial port is opened (added for Arduinos based on ATMEGA32U4)
    SPI.begin();        // Init SPI bus
    mfrc522.PCD_Init(); // Init MFRC522 card
    // Prepare the key (used both as key A and as key B)
    // using FFFFFFFFFFFFh which is the default at chip delivery from the factory
    for (byte i = 0; i < 6; i++)
        key.keyByte[i] = 0xFF;

    // Threads
    PT_SEM_INIT(&sem_LED, 1);
    PT_INIT(&thread_led);
    PT_INIT(&thread_rc522);
}

void loop() {
    thread_led_entry(&thread_led);
    thread_rc522_entry(&thread_rc522);
}

void draw(byte* list, CRGB color, bool cover = true) {
    for (int i = 0; i < 64; ++i) {
        if (list[i/8] & (0x80 >> (i%8)))
            leds[i] = color;
        else if (!cover)
            leds[i] = 0;
    }
}

byte data[722];
int total = 0;
int maps = 0;
int offset = -1;

static int thread_rc522_entry(struct pt *pt)
{
    PT_BEGIN(pt);
    while (true) {
        // Look for new cards & Select one of the cards
        if (mfrc522.PICC_IsNewCardPresent() && mfrc522.PICC_ReadCardSerial()) {
            PT_SEM_WAIT(pt, &sem_LED);
            bool error_flag = false;

            static byte download[8] = {0x00, 0x10, 0x10, 0x54, 0x38, 0x10, 0x00, 0x00};
            draw(download, CRGB::Green, false);
            FastLED.show();
            
            // Show some details of the PICC (that is: the tag/card)
            Serial.print(F("Card UID:"));
            dump_byte_array(mfrc522.uid.uidByte, mfrc522.uid.size);
            Serial.println();
            Serial.print(F("PICC type: "));
            MFRC522::PICC_Type piccType = mfrc522.PICC_GetType(mfrc522.uid.sak);
            Serial.println(mfrc522.PICC_GetTypeName(piccType));
            
            // Check for compatibility
            if (piccType != MFRC522::PICC_TYPE_MIFARE_1K) {
                Serial.println(F("This sample only works with MIFARE Classic 1K cards."));
                // Halt PICC
                mfrc522.PICC_HaltA();
                error_flag = true;
                goto final_clear;
            }
    
            total = 0;
            offset = -1;
            MFRC522::StatusCode status;
            // byte buffer[18];
            // byte size = sizeof(buffer);   
            
            for (int8_t i = 1; i < 16; i++) {
                // Serial.println(F("Authenticating using key A..."));
                status = (MFRC522::StatusCode) mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, i*4+3, &key, &(mfrc522.uid));
                int error_times = 0;
                while (status != MFRC522::STATUS_OK) {
                    error_times ++;
                    if (error_times > 5) {
                        Serial.print(F("PCD_Authenticate() failed: "));
                        Serial.println(mfrc522.GetStatusCodeName(status));
                        error_flag = true;
                        goto final_clear;
                    } else
                        status = (MFRC522::StatusCode) mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, i*4+3, &key, &(mfrc522.uid));
                }
                for (int j = i*4; j < i*4+3; ++j) {
                    // status = (MFRC522::StatusCode) mfrc522.MIFARE_Read(j, buffer, &size);
                    byte size = 18;
                    status = (MFRC522::StatusCode) mfrc522.MIFARE_Read(j, data + total, &size);
                    Serial.print(j); Serial.print("\t");
                    int error_times = 0;
                    while (status != MFRC522::STATUS_OK) {
                        error_times ++;
                        if (error_times > 5) {
                            Serial.print(F("MIFARE_Read() failed: "));
                            Serial.println(mfrc522.GetStatusCodeName(status));
                            error_flag = true;
                            goto final_clear;
                        } else
                            status = (MFRC522::StatusCode) mfrc522.MIFARE_Read(j, data + total, &size);
                    }
                    dump_byte_array(data + total, 16); Serial.println();
                    total += 16;
                    if (j == 4)
                        maps = data[0];
                    if (total >= maps * 8 + 16)
                        goto loop_end;
                }
            }
loop_end:
            byte success_map[8] = {0x00, 0x00, 0x02, 0x44, 0x28, 0x10, 0x00, 0x00};
            draw(success_map, CRGB::Green, false);
            FastLED.show();
            FastLED.delay(1000);
            
            offset = 0;
final_clear:
            if (error_flag) {
                static byte error_map[8] = {0x00, 0x44, 0x28, 0x10, 0x28, 0x44, 0x00, 0x00};
                draw(error_map, CRGB::Red, false);
                FastLED.show();
                FastLED.delay(1000);
                // PT_TIMER_DELAY(pt, 100);
            }
            // Halt PICC
            mfrc522.PICC_HaltA();
            // Stop encryption on PCD
            mfrc522.PCD_StopCrypto1();
            PT_SEM_SIGNAL(pt, &sem_LED);
        }
        PT_YIELD(pt);
    }
    Serial.println("RC522 END!!!");
    PT_END(pt);
}

static int thread_led_entry(struct pt *pt) {
    PT_BEGIN(pt);
    while (true) {
        PT_SEM_WAIT(pt, &sem_LED);
        if (offset >= 0) {
            static uint8_t starthue = 0;
            starthue += 2;
            CHSV hsv;
            hsv.hue = starthue;
            hsv.val = 255;
            hsv.sat = 240;
            for(int i = 0; i < 64; i++) {
                leds[i] = hsv;
                hsv.hue += 3;
            }
            static int INIT_TIME = 40;
            static int SHOW_TIME = 40;
            static int DELEY_TIME = 20;
            static int LOOP_TIME = SHOW_TIME + DELEY_TIME;
            static char str[17] = "HAPPY BIRTHDAY  ";
            if (offset > INIT_TIME && (offset-INIT_TIME) % LOOP_TIME <= SHOW_TIME) {
                int index = (offset-INIT_TIME) / LOOP_TIME;
                index %= maps;
                draw(data + 16 + index * 8, CRGB(0,0,0), true);
            }
            offset ++;
        } else {
            ChangePalettePeriodically();
            static uint8_t startIndex = 0;
            startIndex = startIndex + 1; /* motion speed */
            FillLEDsFromPaletteColors(startIndex);
        }
        FastLED.show();
        PT_SEM_SIGNAL(pt, &sem_LED);
        PT_TIMER_DELAY(pt, 1000 / UPDATES_PER_SECOND);
    }
    PT_END(pt);
}

/**
 * Helper routine to dump a byte array as hex values to Serial.
 */
void dump_byte_array(byte *buffer, byte bufferSize) {
    for (byte i = 0; i < bufferSize; i++) {
        Serial.print(buffer[i] < 0x10 ? " 0" : " ");
        Serial.print(buffer[i], HEX);
    }
}

void FillLEDsFromPaletteColors( uint8_t colorIndex)
{
    uint8_t brightness = 255;
    for( int i = 0; i < NUM_LEDS; i++) {
        leds[i] = ColorFromPalette( currentPalette, colorIndex, brightness, currentBlending);
        colorIndex += 3;
    }
}

void ChangePalettePeriodically()
{
    uint8_t secondHand = (millis() / 1000) % 60;
    static uint8_t lastSecond = 99;
    if( lastSecond != secondHand) {
        lastSecond = secondHand;
        if( secondHand ==  0)  { currentPalette = RainbowColors_p;         currentBlending = LINEARBLEND; }
        if( secondHand == 10)  { currentPalette = RainbowStripeColors_p;   currentBlending = NOBLEND;  }
        if( secondHand == 15)  { currentPalette = RainbowStripeColors_p;   currentBlending = LINEARBLEND; }
        if( secondHand == 20)  { SetupPurpleAndGreenPalette();             currentBlending = LINEARBLEND; }
        if( secondHand == 25)  { SetupTotallyRandomPalette();              currentBlending = LINEARBLEND; }
        if( secondHand == 30)  { SetupBlackAndWhiteStripedPalette();       currentBlending = NOBLEND; }
        if( secondHand == 35)  { SetupBlackAndWhiteStripedPalette();       currentBlending = LINEARBLEND; }
        if( secondHand == 40)  { currentPalette = CloudColors_p;           currentBlending = LINEARBLEND; }
        if( secondHand == 45)  { currentPalette = PartyColors_p;           currentBlending = LINEARBLEND; }
        if( secondHand == 50)  { currentPalette = myRedWhiteBluePalette_p; currentBlending = NOBLEND;  }
        if( secondHand == 55)  { currentPalette = myRedWhiteBluePalette_p; currentBlending = LINEARBLEND; }
    }
}

// This function fills the palette with totally random colors.
void SetupTotallyRandomPalette()
{
    for( int i = 0; i < 16; i++) {
        currentPalette[i] = CHSV( random8(), 255, random8());
    }
}

void SetupBlackAndWhiteStripedPalette()
{
    // 'black out' all 16 palette entries...
    fill_solid( currentPalette, 16, CRGB::Black);
    // and set every fourth one to white.
    currentPalette[0] = CRGB::White;
    currentPalette[4] = CRGB::White;
    currentPalette[8] = CRGB::White;
    currentPalette[12] = CRGB::White;
}

// This function sets up a palette of purple and green stripes.
void SetupPurpleAndGreenPalette()
{
    CRGB purple = CHSV( HUE_PURPLE, 255, 255);
    CRGB green  = CHSV( HUE_GREEN, 255, 255);
    CRGB black  = CRGB::Black;
    
    currentPalette = CRGBPalette16(
                                   green,  green,  black,  black,
                                   purple, purple, black,  black,
                                   green,  green,  black,  black,
                                   purple, purple, black,  black );
}

const TProgmemPalette16 myRedWhiteBluePalette_p PROGMEM =
{
    CRGB::Red,
    CRGB::Gray, // 'white' is too bright compared to red and blue
    CRGB::Blue,
    CRGB::Black,
    
    CRGB::Red,
    CRGB::Gray,
    CRGB::Blue,
    CRGB::Black,
    
    CRGB::Red,
    CRGB::Red,
    CRGB::Gray,
    CRGB::Gray,
    CRGB::Blue,
    CRGB::Blue,
    CRGB::Black,
    CRGB::Black
};
