package com.armongate.mobilepasssdk.model;

public class CryptoKeyPair {

    public String publicKey;
    public String privateKey;

    public CryptoKeyPair(String publicKey, String privateKey) {
        this.publicKey  = publicKey;
        this.privateKey = privateKey;
    }

}

