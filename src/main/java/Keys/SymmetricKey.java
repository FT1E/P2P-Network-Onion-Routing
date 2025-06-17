package Keys;

import Util.LogLevel;
import Util.Logger;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class SymmetricKey {

    // functionalities:
    //      - encrypt / decrypt a string
    //      - convert key to string and vice versa

    private SecretKey symmetricKey;
    private byte[] ivBytes;
    private IvParameterSpec ivParameterSpec;

    // Constructors

    // default one for generating a new key
    public SymmetricKey(){
        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            Logger.log("Invalid algorithm specified for SymmetricKey!", LogLevel.DEBUG);
            return;
        }
        keyGenerator.init(128);
        symmetricKey = keyGenerator.generateKey();

        SecureRandom secureRandom = null;
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            Logger.log("Invalid alg for SecureRandom" + e.getMessage(), LogLevel.DEBUG);
            return;
        }

        ivBytes = new byte[16];
        secureRandom.nextBytes(ivBytes);
        ivParameterSpec = new IvParameterSpec(ivBytes);
    }

    // - creating a key based on a given key encoding
    public SymmetricKey(String encoding){
        String[] tokens = encoding.split(" ", 2);
        ivBytes = Base64.getDecoder().decode(tokens[0]);
        ivParameterSpec = new IvParameterSpec(ivBytes);

        byte[] keyBytes = Base64.getDecoder().decode(tokens[1]);
        symmetricKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
    }

    // end Constructors



    // Encrypt
    public String encrypt(String body){
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e){
            Logger.log("Check code:" + e.getMessage(), LogLevel.ERROR);
            return null;
        }


        try {
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey, ivParameterSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            Logger.log("cipher.init in SymKey.encrypt():" + e.getMessage(), LogLevel.ERROR);
            return null;
        }

        byte[] encryptedBytes;
        try {
            encryptedBytes = cipher.doFinal(body.getBytes());
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            Logger.log("Error while encrypting: " + e.getMessage());
            return null;
        }

        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    // end Encrypt


    // Decrypt
    public String decrypt(String body){
        byte[] encryptedBytes = Base64.getDecoder().decode(body);

        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e){
            Logger.log("Check code:" + e.getMessage(), LogLevel.ERROR);
            return null;
        }


        try {
            cipher.init(Cipher.DECRYPT_MODE, symmetricKey, ivParameterSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            Logger.log("cipher.init in SymKey.decrypt():" + e.getMessage(), LogLevel.ERROR);
            return null;
        }

        byte[] decryptedBytes;
        try {
            decryptedBytes = cipher.doFinal(encryptedBytes);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            Logger.log("Error while decrypting: " + e.getMessage());
            return null;
        }
        return new String(decryptedBytes);
    }
    // end Decrypt

    // Encode to String
    public String encodeToString(){
        String encodedKey = Base64.getEncoder().encodeToString(symmetricKey.getEncoded());
        String encodedIv = Base64.getEncoder().encodeToString(ivBytes);
        return encodedIv + " " + encodedKey;
    }
    // end Encode to String
}
