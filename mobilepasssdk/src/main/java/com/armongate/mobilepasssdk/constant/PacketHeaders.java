package com.armongate.mobilepasssdk.constant;

public class PacketHeaders {

    public static final byte PLATFORM_ANDROID = (byte)0xF0;

    public interface PROTOCOLV2 {

        interface COMMON {
            byte FAILURE = 0x20;
            byte SUCCESS = 0x21;
        }

        interface GROUP {
            byte AUTH = 0x01;
        }

        interface AUTH {
            byte PUBLICKEY_CHALLENGE    = 0x01;
            byte CHALLENGE_RESULT       = 0x03;
            byte DIRECTION_CHALLENGE    = 0x05;
        }

        interface FAILURE_REASON {
            byte NOACCESSRIGHT                  = 0x01;
            byte UNKNOWN_CREDENTIAL_OWNER       = 0x02;
            byte INVALID_CONFIGURATION          = 0x03;
            byte UNHANDLED_FAILURE              = 0x04;
            byte ANTIPASSBACK_REJECT            = 0x05;
            byte ANTIPASSBACK_TIMEOUT           = 0x06;
            byte CREDENTIAL_EXPIRED             = 0x08;
            byte INSUFFICIENT_FUND              = 0x09;
            byte RULE_REJECT                    = 0x0A;
            byte STATE_OPENED                   = 0x0C;
            byte STATE_DISABLED                 = 0x0D;
            byte MULTIFACTOR_REQUIRED           = 0x0E;
            byte USER_FORBIDDEN                 = 0x0F;
            byte USER_DISABLED                  = 0x10;
            byte RELAY_NOT_AVAILABLE            = 0x11;
            byte CHALLENGE_FAIL                 = 0x12;
            byte INVALID_CHALLENGE              = 0x13;
            byte MIFARE_FINGERPRINT_NOT_MATCH   = 0x14;
            byte MIFARE_FINGERPRINT_TIMEOUT     = 0x15;
            byte REGION_CAPACITY_FULL           = 0x16;
        }
    }
}
