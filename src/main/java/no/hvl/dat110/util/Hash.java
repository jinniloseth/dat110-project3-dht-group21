package no.hvl.dat110.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash { 
	
	
	public static BigInteger hashOf(String entity) {	
		
		try {
			//oppretter en MD5 MessageDigest 
			MessageDigest md= MessageDigest.getInstance("MD5");
			
			//beregn hash-verdien av input-strengen
			byte[] digest= md.digest(entity.getBytes());
			
			//konverterer byte-array til BigInteger (pos verdi)
			return new BigInteger(1, digest);
		}catch(NoSuchAlgorithmException e){
			throw new RuntimeException("MD5 algoritmen er ikke tilgjengelig ", e);
		}
	}
	
	public static BigInteger addressSize() {
		
		//MD5 gir en 128-bits adresseplass, altså 2^128
		// Metoden .pow(int exponent) i BigInteger brukes til å beregne en potens
		return BigInteger.valueOf(2).pow(128);
	}
	
	public static int bitSize() {
		//md5 genererer en 128-bit hash
		return 128;
	}
	
	
	public static String toHex(byte[] digest) {
		StringBuilder strbuilder = new StringBuilder();
		for(byte b : digest) {
			strbuilder.append(String.format("%02x", b&0xff));
		}
		return strbuilder.toString();
	}

}
