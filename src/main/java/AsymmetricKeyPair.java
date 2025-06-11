import Util.LogLevel;
import Util.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class AsymmetricKeyPair {

    // todo - think of which encrypting/decrypting algs to use

    // variables
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    // end variables

    // Constructors

    // default one
    public AsymmetricKeyPair() throws NoSuchAlgorithmException{
        KeyPairGenerator kp_gen;
        try {
            kp_gen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            Logger.log("KP_gen.getInstance(), noSuchAlgorithm Exception! Fix code!", LogLevel.WARN);
            throw e;
            // throwing so I know I don't work with useless object
        }
        KeyPair keyPair = kp_gen.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }

    // When you only have a public one
    public AsymmetricKeyPair(PublicKey publicKey){
        this.publicKey = publicKey;
        this.privateKey = null;
    }

    // when you have a public key encoded to a string
    public AsymmetricKeyPair(String publicKey_string) throws NoSuchAlgorithmException, InvalidKeySpecException{
        privateKey = null;

        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey_string));

        KeyFactory keyFactory;
        try {
            // note: same algorithm name as in default constructor
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            Logger.log("Check code! In AsymmetricKeyPair constructor KeyFactory.getInstance():" + e.getMessage(), LogLevel.WARN);
            throw e;
        }

        try {
            this.publicKey = keyFactory.generatePublic(publicKeySpec);
        } catch (InvalidKeySpecException e) {
            Logger.log("Check code! In AsymmetricKeyPair constructor at keyFactory.generatePublic():" + e.getMessage(), LogLevel.WARN);
            throw e;
        }

    }

    // end constructors

    // Encrypting and decrypting

    // encrypt with public key
    public String encryptPublic(String body){
        if (publicKey == null) return null;
        return encrypt(publicKey, body);
    }

    // decrypt with private key
    public String decryptPrivate(String body){
        if (privateKey == null) return null;
        return decrypt(privateKey, body);
    }

    // encrypt with private key
    public String encryptPrivate(String body){
        if (privateKey == null) return null;
        return encrypt(privateKey, body);
    }

    // decrypt with public key
    public String decryptPublic(String body){
        if (publicKey == null) return null;
        return decrypt(publicKey, body);
    }


    // Helper methods for less code-repetition
    private String encrypt(Key key, String body){
        Cipher cipher;
        try{
            cipher = Cipher.getInstance("RSA");
        }catch (NoSuchAlgorithmException | NoSuchPaddingException e){
            Logger.log("Error in AsymmetricKeyPair.encrypt()! Fix code!", LogLevel.WARN);
            return null;
        }
        try{
            cipher.init(Cipher.ENCRYPT_MODE, key);
        }catch (InvalidKeyException e){
            Logger.log("InvalidKeyException in AsymmetricKeyPair.encrypt()! Check code!", LogLevel.WARN);
            return null;
        }
        return doFinal(cipher, body);
    }
    private String decrypt(Key key, String body){
        Cipher cipher;
        try{
            cipher = Cipher.getInstance("RSA");
        }catch (NoSuchAlgorithmException | NoSuchPaddingException e){
            Logger.log("Error in AsymmetricKeyPair.decrypt()! Fix code!", LogLevel.WARN);
            return null;
        }
        try{
            cipher.init(Cipher.DECRYPT_MODE, key);
        }catch (InvalidKeyException e){
            Logger.log("InvalidKeyException in AsymmetricKeyPair.decrypt()! Check code!", LogLevel.WARN);
            return null;
        }
        return doFinal(cipher, body);
    }
    private String doFinal(Cipher cipher, String body){
        String res = null;
        try {
            res = Base64.getEncoder().encodeToString(cipher.doFinal(body.getBytes()));
        } catch (IllegalBlockSizeException e) {
            Logger.log("Illegal Block Size exception:" + e.getMessage());
        } catch (BadPaddingException e) {
            Logger.log("Bad Padding exception:" + e.getMessage());
        }
        return res;
    }

    // end encrypting and decrypting methods


    // getters
    // 1 getter for public key encoded to string
    public String encodePublicKey_toString(){
        // encoding of public key
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    // end getters
}
