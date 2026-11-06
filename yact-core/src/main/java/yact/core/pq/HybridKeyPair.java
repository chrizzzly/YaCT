/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.pq;

import java.security.SecureRandom;

import javax.security.auth.Destroyable;

import org.bouncycastle.crypto.params.MLKEMPrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;

import yact.core.crypto.Passwords;

/**
 * A user's hybrid key pair: an ML-KEM key and an X25519 key.
 *
 * <p>The private part is held as the 64-byte ML-KEM seed (d ‖ z, the form
 * FIPS 203 recommends for storage) and the 32-byte X25519 scalar. The public
 * key is recomputed from them.
 *
 * <p>Call {@link #close()} when done; it overwrites the private key bytes we
 * hold. BouncyCastle's internal copies are short-lived and released to the
 * garbage collector.
 */
public final class HybridKeyPair implements AutoCloseable, Destroyable
{
	private final HybridKem kem;
	private final byte[] mlKemSeed;
	private final byte[] x25519Private;
	private final HybridPublicKey publicKey;
	private boolean destroyed;

	HybridKeyPair(HybridKem kem, byte[] mlKemSeed, byte[] x25519Private)
	{
		if (mlKemSeed.length != HybridKem.MLKEM_SEED_LENGTH || x25519Private.length != HybridKem.X25519_KEY_LENGTH)
			throw new IllegalArgumentException("invalid private key length");
		this.kem = kem;
		this.mlKemSeed = mlKemSeed.clone();
		this.x25519Private = x25519Private.clone();

		MLKEMPrivateKeyParameters mk = new MLKEMPrivateKeyParameters(kem.mlKemParameters(), this.mlKemSeed);
		byte[] x25519Public = new X25519PrivateKeyParameters(this.x25519Private).generatePublicKey().getEncoded();
		this.publicKey = new HybridPublicKey(kem, x25519Public, mk.getPublicKey());
	}

	/** Generates a new key pair. */
	public static HybridKeyPair generate(HybridKem kem, SecureRandom random)
	{
		byte[] seed = new byte[HybridKem.MLKEM_SEED_LENGTH];
		byte[] x = new byte[HybridKem.X25519_KEY_LENGTH];
		random.nextBytes(seed);
		// X25519 clamps the scalar itself (RFC 7748, section 5), so any 32 random bytes are a valid key.
		random.nextBytes(x);
		try
		{
			return new HybridKeyPair(kem, seed, x);
		}
		finally
		{
			Passwords.wipe(seed, x);
		}
	}

	public HybridKem kem()
	{
		return kem;
	}

	public HybridPublicKey publicKey()
	{
		return publicKey;
	}

	MLKEMPrivateKeyParameters mlKemPrivate()
	{
		checkAlive();
		return new MLKEMPrivateKeyParameters(kem.mlKemParameters(), mlKemSeed);
	}

	X25519PrivateKeyParameters x25519Private()
	{
		checkAlive();
		return new X25519PrivateKeyParameters(x25519Private);
	}

	/** Private key material for the key file: ML-KEM seed ‖ X25519 scalar. Caller wipes. */
	byte[] privateBytes()
	{
		checkAlive();
		byte[] out = new byte[mlKemSeed.length + x25519Private.length];
		System.arraycopy(mlKemSeed, 0, out, 0, mlKemSeed.length);
		System.arraycopy(x25519Private, 0, out, mlKemSeed.length, x25519Private.length);
		return out;
	}

	static HybridKeyPair fromPrivateBytes(HybridKem kem, byte[] b)
	{
		if (b.length != HybridKem.MLKEM_SEED_LENGTH + HybridKem.X25519_KEY_LENGTH)
			throw new IllegalArgumentException("invalid private key length");
		byte[] seed = java.util.Arrays.copyOfRange(b, 0, HybridKem.MLKEM_SEED_LENGTH);
		byte[] x = java.util.Arrays.copyOfRange(b, HybridKem.MLKEM_SEED_LENGTH, b.length);
		try
		{
			return new HybridKeyPair(kem, seed, x);
		}
		finally
		{
			Passwords.wipe(seed, x);
		}
	}

	private void checkAlive()
	{
		if (destroyed)
			throw new IllegalStateException("key has been destroyed");
	}

	@Override
	public void destroy()
	{
		Passwords.wipe(mlKemSeed, x25519Private);
		destroyed = true;
	}

	@Override
	public boolean isDestroyed()
	{
		return destroyed;
	}

	@Override
	public void close()
	{
		destroy();
	}
}
