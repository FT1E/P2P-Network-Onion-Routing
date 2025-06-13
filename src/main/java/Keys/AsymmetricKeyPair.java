package Keys;

import Util.LogLevel;
import Util.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class AsymmetricKeyPair {


    // variables
    private PublicKey publicKey;
    private PrivateKey privateKey;


    // Constructors

    // general one for generating a new keyPair
    public AsymmetricKeyPair(){
        KeyPairGenerator kp_gen;

        try {
            kp_gen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            Logger.log("Invalid algorithm in KeyPairGenerator");
            return;
        }

        kp_gen.initialize(1024);
        KeyPair keyPair = kp_gen.generateKeyPair();

        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }


    // todo - from a public key encoded to string
    public AsymmetricKeyPair(String publicKeyEncoding) throws InvalidKeySpecException{
        byte[] decodedPublicKey = Base64.getDecoder().decode(publicKeyEncoding);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decodedPublicKey);
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            Logger.log("Error in KeyFactory.getInstance:" + e.getMessage(), LogLevel.ERROR);
            return;
        }
        try {
            publicKey = keyFactory.generatePublic(publicKeySpec);
        } catch (InvalidKeySpecException e) {
            Logger.log("InvalidKeySpec for a public key:" + e.getMessage(), LogLevel.ERROR);
            throw e;
        }
        privateKey = null;
    }

    // end Constructors


    // Encrypt with public key
    public String encrypt(String body){
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Logger.log("Error in Cipher.getInstance:" + e.getMessage(), LogLevel.ERROR);
            return null;
        }

        try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        } catch (InvalidKeyException e) {
            Logger.log("Error in cipher.init:" + e.getMessage() + e.getMessage(), LogLevel.ERROR);
            return null;
        }

        byte[] encryptedBytes;
        try {
            encryptedBytes = cipher.doFinal(body.getBytes());
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            Logger.log("Error in cipher.doFinal while encrypting with public key:" + e.getMessage());
            return null;
        }
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    // end Encrypt with public key


    // Decrypt with private key
    public String decrypt(String body){
        if(privateKey == null){
            return null;
        }
        byte[] encryptedBytes = Base64.getDecoder().decode(body.getBytes());

        Cipher cipher;

        try {
            cipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Logger.log("Error in Cipher.getInstance:" + e.getMessage(), LogLevel.ERROR);
            return null;
        }

        try {
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
        } catch (InvalidKeyException e) {
            Logger.log("Error in cipher.init:" + e.getMessage(), LogLevel.ERROR);
            return null;
        }

        byte[] decryptedBytes;

        try {
            decryptedBytes = cipher.doFinal(encryptedBytes);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            Logger.log("Error in cipher.doFinal while decrypting with private key:" + e.getMessage(), LogLevel.ERROR);
            return null;
        }
        return new String(decryptedBytes);
    }
    //  end Decrypt with private key


    // Encode public key to string
    public String encodePublicKey(){
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    // end Encode public key to string

}
