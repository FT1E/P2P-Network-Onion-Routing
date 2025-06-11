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

    // todo - think of which encrypting/decrypting algs to use

    // variables
    private SecretKey symmetricKey;
    private byte[] iv;
    private IvParameterSpec ivSpec;
    // end variables

    // Constructors
    public SymmetricKey() {

        KeyGenerator key_gen = null;
        try {
            key_gen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            Logger.log("Check code in SymmetricKey constructor at KeyGenerator.getInstance():" + e.getMessage(), LogLevel.WARN);
            return;
        }
        symmetricKey = key_gen.generateKey();

        SecureRandom secureRandom = null;
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            Logger.log("Check code in SymmetricKey constructor at SecureRandom.getInstance():" + e.getMessage(), LogLevel.WARN);
            return;
        }

        iv = new byte[16];
        secureRandom.nextBytes(iv);
        ivSpec = new IvParameterSpec(iv);
    }

    public SymmetricKey(SecretKey key, byte[] randomIvSpec){
        this.symmetricKey = key;
        ivSpec = new IvParameterSpec(randomIvSpec);
    }

    // when you have a string encoding of a key
    public SymmetricKey(String encoding){
        String[] tokens = encoding.split(" ", 2);

        byte[] decoded_key_bytes = Base64.getDecoder().decode(tokens[0]);
        symmetricKey = new SecretKeySpec(decoded_key_bytes, 0, decoded_key_bytes.length, "AES");
        ivSpec = new IvParameterSpec(Base64.getDecoder().decode(tokens[1]));
    }

    // end constructors

    // encrypting and decrypting
    public String encrypt(String body){
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Logger.log("Fix code in SymmetricKey.encrypt():" + e.getMessage(), LogLevel.WARN);
            return null;
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey, ivSpec);
        } catch (InvalidKeyException e) {
            Logger.log("Invalid Key exception in SymmetricKey.encrypt():" + e.getMessage(), LogLevel.WARN);
            return null;
        } catch (InvalidAlgorithmParameterException e) {
            Logger.log("Invalid algo parameter exception in SymmetricKey.encrypt():" + e.getMessage(), LogLevel.WARN);
            return null;
        }
        return doFinal(cipher, body);
    }

    public String decrypt(String body){
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Logger.log("Fix code in SymmetricKey.decrypt():" + e.getMessage(), LogLevel.WARN);
            return null;
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, symmetricKey, ivSpec);
        } catch (InvalidKeyException e) {
            Logger.log("Invalid Key exception in SymmetricKey.decrypt():" + e.getMessage(), LogLevel.WARN);
            return null;
        } catch (InvalidAlgorithmParameterException e) {
            Logger.log("Invalid algo parameter exception in SymmetricKey.decrypt():" + e.getMessage(), LogLevel.WARN);
            return null;
        }
        return doFinal(cipher, body);
    }

    // Helper method
    private String doFinal(Cipher cipher, String body){
        String res = null;
        try {
            res = Base64.getEncoder().encodeToString(cipher.doFinal(body.getBytes()));
        } catch (IllegalBlockSizeException e) {
            Logger.log("Illegal Block Size exception in SymmetricKey:" + e.getMessage(), LogLevel.WARN);
        } catch (BadPaddingException e) {
            Logger.log("Bad Padding exception in SymmetricKey:" + e.getMessage(), LogLevel.WARN);
        }
        return res;
    }

    // end encrypting and decrypting


    // getters
    public String encodeKey_toString(){
        // todo - encode by joining the key and the ivSpec together
        //  with a separator char which doesn't appear
        return Base64.getEncoder().encodeToString(symmetricKey.getEncoded()) + " " + Base64.getEncoder().encodeToString(iv);
    }
    // end getters
}
