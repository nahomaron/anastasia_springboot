package external;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class HmacSHA256KeyGenerator {

    public static void main(String[] args){

        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("hmacSHA256");
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();
            String encoded = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            System.out.println(encoded);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
