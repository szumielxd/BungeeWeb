package io.github.dead_i.bungeeweb;

import java.security.SecureRandom;
import java.util.Base64;

import org.eclipse.jetty.util.security.Credential;

public class SecureUtils {
	
	private static SecureRandom RANDOM = new SecureRandom();

    public static String encrypt(String pass) {
        return Credential.MD5.digest(pass).split(":")[1];
    }

    public static String encrypt(String pass, String salt) {
        return encrypt(pass + salt);
    }

    public static String salt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt).substring(0, 16);
    }

}
