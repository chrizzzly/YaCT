/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.pq;

import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator;
import org.bouncycastle.crypto.kems.MLKEMExtractor;
import org.bouncycastle.crypto.kems.MLKEMGenerator;
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;
import org.bouncycastle.util.Arrays;

import yact.core.crypto.Kdf;
import yact.core.crypto.Passwords;

/**
 * Hybrid key encapsulation: ML-KEM and ephemeral-static X25519, combined
 * into one 32-byte key.
 *
 * <pre>
 * key = HKDF-SHA3-256(
 *         ikm  = ss_mlkem ‖ ss_x25519,
 *         salt = context,
 *         info = "YaCT v1 hybrid kem", scheme id, ct_mlkem, ephemeral_x25519,
 *                recipient_x25519, recipient_mlkem)
 * </pre>
 *
 * <p>Both shared secrets and every ciphertext and public key enter the KDF,
 * the generic combiner that stays secure if either component is broken
 * (Giacon, Heuer, Poettering, "KEM Combiners", PKC 2018; see also NIST SP 800-227).
 * An attacker must break both ML-KEM and X25519 to learn the key.
 */
public final class Hybrid
{
	private static final String LABEL = "YaCT v1 hybrid kem";

	private Hybrid()
	{
	}

	/** Output of {@link #encapsulate}: the key, and the two values the recipient needs. */
	public record Encapsulation(byte[] key, byte[] x25519Ephemeral, byte[] mlKemCiphertext)
	{
	}

	/**
	 * Creates a fresh key for {@code recipient}.
	 *
	 * @param context bound into the key, e.g. the container id
	 */
	public static Encapsulation encapsulate(HybridPublicKey recipient, byte[] context, SecureRandom random)
	{
		SecretWithEncapsulation mlKem = new MLKEMGenerator(random).generateEncapsulated(recipient.mlKemParameters());

		X25519KeyPairGenerator gen = new X25519KeyPairGenerator();
		gen.init(new X25519KeyGenerationParameters(random));
		AsymmetricCipherKeyPair eph = gen.generateKeyPair();
		byte[] ephPublic = ((X25519PublicKeyParameters) eph.getPublic()).getEncoded();
		byte[] ssX = x25519((X25519PrivateKeyParameters) eph.getPrivate(), recipient.x25519Parameters());
		if (ssX == null)
			throw new IllegalArgumentException("invalid recipient X25519 key"); // excluded by HybridPublicKey

		byte[] ssM = mlKem.getSecret();
		byte[] ct = mlKem.getEncapsulation();
		try
		{
			return new Encapsulation(combine(recipient, ssM, ssX, ct, ephPublic, context), ephPublic, ct);
		}
		finally
		{
			Passwords.wipe(ssM, ssX);
		}
	}

	/**
	 * Recovers the key.
	 *
	 * <p>ML-KEM uses implicit rejection: for a ciphertext not made for this key
	 * the result is a random-looking key rather than an error. Callers detect
	 * the mismatch when the key fails to authenticate data.
	 *
	 * @return the key, or {@code null} if {@code x25519Ephemeral} is invalid
	 */
	public static byte[] decapsulate(HybridKeyPair key, byte[] x25519Ephemeral, byte[] mlKemCiphertext, byte[] context)
	{
		HybridKem kem = key.kem();
		if (x25519Ephemeral.length != HybridKem.X25519_KEY_LENGTH || mlKemCiphertext.length != kem.mlKemCiphertextLength())
			throw new IllegalArgumentException("encapsulation has the wrong length for " + kem);

		byte[] ssX = x25519(key.x25519Private(), new X25519PublicKeyParameters(x25519Ephemeral));
		if (ssX == null)
			return null;
		byte[] ssM = new MLKEMExtractor(key.mlKemPrivate()).extractSecret(mlKemCiphertext);
		try
		{
			return combine(key.publicKey(), ssM, ssX, mlKemCiphertext, x25519Ephemeral, context);
		}
		finally
		{
			Passwords.wipe(ssM, ssX);
		}
	}

	static byte[] combine(HybridPublicKey recipient, byte[] ssM, byte[] ssX, byte[] ct, byte[] ephPublic,
			byte[] context)
	{
		byte[] ikm = Arrays.concatenate(ssM, ssX);
		try
		{
			return Kdf.hkdf(ikm, context, LABEL, new byte[] { (byte) recipient.kem().id() }, ct, ephPublic,
					recipient.x25519(), recipient.mlKem());
		}
		finally
		{
			Passwords.wipe(ikm);
		}
	}

	/** X25519, or {@code null} if the result is all zero (low-order peer key). */
	private static byte[] x25519(X25519PrivateKeyParameters priv, X25519PublicKeyParameters pub)
	{
		X25519Agreement agreement = new X25519Agreement();
		agreement.init(priv);
		byte[] out = new byte[agreement.getAgreementSize()];
		try
		{
			agreement.calculateAgreement(pub, out, 0);
			return out;
		}
		catch (IllegalStateException e)
		{
			return null;
		}
	}
}
