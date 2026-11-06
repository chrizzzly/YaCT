/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.pq;

import org.bouncycastle.crypto.params.MLKEMParameters;

/**
 * The hybrid key encapsulation schemes YaCT supports: ML-KEM (FIPS 203)
 * combined with X25519 (RFC 7748).
 *
 * <p>The numeric {@link #id()} is part of the file format and must never
 * change for an existing constant.
 */
public enum HybridKem
{
	/** NIST security category 3. */
	MLKEM768_X25519(1, "ML-KEM-768 + X25519", MLKEMParameters.ml_kem_768, 1184, 1088),
	/** NIST security category 5. */
	MLKEM1024_X25519(2, "ML-KEM-1024 + X25519", MLKEMParameters.ml_kem_1024, 1568, 1568);

	/** The strongest scheme; the size and speed difference to ML-KEM-768 is negligible here. */
	public static final HybridKem DEFAULT = MLKEM1024_X25519;

	public static final int X25519_KEY_LENGTH = 32;
	public static final int MLKEM_SEED_LENGTH = 64;

	private final int id;
	private final String displayName;
	private final MLKEMParameters mlKemParameters;
	private final int mlKemPublicKeyLength;
	private final int mlKemCiphertextLength;

	HybridKem(int id, String displayName, MLKEMParameters params, int publicKeyLength, int ciphertextLength)
	{
		this.id = id;
		this.displayName = displayName;
		this.mlKemParameters = params;
		this.mlKemPublicKeyLength = publicKeyLength;
		this.mlKemCiphertextLength = ciphertextLength;
	}

	public int id()
	{
		return id;
	}

	public String displayName()
	{
		return displayName;
	}

	MLKEMParameters mlKemParameters()
	{
		return mlKemParameters;
	}

	public int mlKemPublicKeyLength()
	{
		return mlKemPublicKeyLength;
	}

	public int mlKemCiphertextLength()
	{
		return mlKemCiphertextLength;
	}

	/** Looks up a scheme by its file-format id; {@code null} if unknown. */
	public static HybridKem byId(int id)
	{
		for (HybridKem k : values())
			if (k.id == id)
				return k;
		return null;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
