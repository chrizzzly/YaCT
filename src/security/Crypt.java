/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/

package security;

import java.security.*;
import java.util.Arrays;

import javax.crypto.*;

import utils.Algorithms;
import utils.HashAlgorithms;


public class Crypt 
{
	/**
	 * Encrypts a block of raw data
	 * @param input is the unencrypted block of data as byte[]
	 * @param key is the key to encrypt with as byte[]
	 * @param cryptoalg is the Cryptoalgorithm as a String
	 * @param hashalg is the Hashingalgoritm as a String
	 * @return returns an encrypted block
	 */
	public static byte[] encrypt(byte[] input, byte[] key, Algorithms cryptoalg, HashAlgorithms hashalg) throws GeneralSecurityException
	{
		
		byte[] retVal = null;
		byte[] hash = null;
		
		Cipher.getInstance(cryptoalg.toString(),"BC" ).getProvider();
		int maxKeyLength = Cipher.getMaxAllowedKeyLength(cryptoalg.toString());
		System.out.println("Key: " + Arrays.toString(key));
		if(key.length > maxKeyLength)
		{
			//TODO wenn der Key länger ist als er darf -> trim
		}
		else
		{
			MessageDigest md = MessageDigest.getInstance(hashalg.toString());
			md.update(key);
			hash = md.digest();
			System.out.println("Key: " + Arrays.toString(key));
            System.out.println("Hash: " + Arrays.toString(hash));
		}
		
		return retVal;
	}
	
	/**
	 * Decrypts a block of encrypted data
	 * @param input is the encrypted block of data as byte[]
	 * @param key is the key to decrypt with as byte[]
	 * @param cryptoalg is the Cryptoalgorithm as a String
	 * @param hashalg is the Hashingalgoritm as a String
	 * @return returns a decrypted block
	 */
	public byte[] decrypt(byte[] input, byte[] key, String cryptoalg, String hashalg) throws GeneralSecurityException
	{
		//TODO
		return null;
	}
	
	
}

