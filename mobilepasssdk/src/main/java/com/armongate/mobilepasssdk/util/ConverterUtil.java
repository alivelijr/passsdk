package com.armongate.mobilepasssdk.util;

import java.math.BigInteger;

public class ConverterUtil {

    private static String DIGITS = "0123456789ABCDEF";

    public static String dataToString(byte[] data) {
        return new String(data);
    }

    public static long cardDataToDecimal(byte[] data) {
        long result = 0;
        long factor = 1;

        for (int i=0; i< data.length; i++) {
            long value = data[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }

        return result;
    }

    public static int dataToInt(byte[] data) {
        return new BigInteger(data).intValue();
    }

    public static int dataToInt(byte data) {
        return Integer.decode(String.format("0x%02X", data));
    }

    public static boolean dataToBool(byte[] data) {
        int value = new BigInteger(data).intValue();
        return value != 0;
    }

    public static String dataToIpAddress(byte[] data) {
        String result = "";

        for (int i = 0; i < data.length; i++) {
            if (result.length() > 0) {
                result += ".";
            }

            result += dataToInt(data[i]);
        }

        return result;
    }

    public static byte[] ipAddressToData(String ipAddress) {
        String[]    parts   = ipAddress.split("\\.");
        byte[]      result  = new byte[] {};

        for (String part: parts) {
            try {
                result = ArrayUtil.add(result, (byte)Integer.parseInt(part));
            } catch (Exception ex) {}
        }

        return result;
    }

    public static String bytesToHexString(byte[] data) {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i != data.length; i++) {
            int v = data[i] & 0xff;

            buffer.append(DIGITS.charAt(v >> 4));
            buffer.append(DIGITS.charAt(v & 0xf));
        }

        return buffer.toString();
    }

    public static byte[] hexStringToBytes(String data) {
        int length = data.length();
        byte[] result = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            result[i / 2] = (byte) ((Character.digit(data.charAt(i), 16) << 4) + Character
                    .digit(data.charAt(i + 1), 16));
        }
        return result;
    }

}
