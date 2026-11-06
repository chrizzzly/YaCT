/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.crypto.kems.MLKEMExtractor;
import org.bouncycastle.crypto.kems.MLKEMGenerator;
import org.bouncycastle.crypto.params.MLKEMParameters;
import org.bouncycastle.crypto.params.MLKEMPrivateKeyParameters;
import org.bouncycastle.crypto.params.MLKEMPublicKeyParameters;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * NIST ACVP known-answer tests for ML-KEM (FIPS 203), run against the
 * BouncyCastle implementation YaCT uses. Vectors: src/test/resources/kat.
 */
class MlKemKnownAnswerTest
{
	@ParameterizedTest(name = "{0}")
	@MethodSource("keyGen")
	void keyGeneration(String name, Map<String, String> v)
	{
		MLKEMParameters params = params(v);
		byte[] seed = Arrays.concatenate(hex(v, "d"), hex(v, "z"));
		MLKEMPrivateKeyParameters sk = new MLKEMPrivateKeyParameters(params, seed);

		assertArrayEquals(hex(v, "ek"), sk.getPublicKey(), "encapsulation key");
		assertArrayEquals(hex(v, "dk"),
				sk.withPreferredFormat(MLKEMPrivateKeyParameters.EXPANDED_KEY).getEncoded(), "decapsulation key");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("encap")
	void encapsulation(String name, Map<String, String> v)
	{
		MLKEMPublicKeyParameters pk = new MLKEMPublicKeyParameters(params(v), hex(v, "ek"));
		SecretWithEncapsulation result = MLKEMGenerator.internalGenerateEncapsulated(pk, hex(v, "m"));

		assertArrayEquals(hex(v, "c"), result.getEncapsulation(), "ciphertext");
		assertArrayEquals(hex(v, "k"), result.getSecret(), "shared secret");
	}

	/** Covers valid ciphertexts and modified ones, where FIPS 203 requires implicit rejection. */
	@ParameterizedTest(name = "{0}")
	@MethodSource("decap")
	void decapsulation(String name, Map<String, String> v)
	{
		MLKEMPrivateKeyParameters sk = new MLKEMPrivateKeyParameters(params(v), hex(v, "dk"));
		assertArrayEquals(hex(v, "k"), new MLKEMExtractor(sk).extractSecret(hex(v, "c")));
	}

	static Stream<Object[]> keyGen() throws IOException
	{
		return load("ml-kem-keygen.txt");
	}

	static Stream<Object[]> encap() throws IOException
	{
		return load("ml-kem-encap.txt");
	}

	static Stream<Object[]> decap() throws IOException
	{
		return load("ml-kem-decap.txt");
	}

	private static Stream<Object[]> load(String file) throws IOException
	{
		List<Map<String, String>> cases = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(
				MlKemKnownAnswerTest.class.getResourceAsStream("/kat/" + file), StandardCharsets.US_ASCII)))
		{
			Map<String, String> current = new LinkedHashMap<>();
			for (String line; (line = r.readLine()) != null;)
			{
				if (line.startsWith("#"))
					continue;
				if (line.isBlank())
				{
					if (!current.isEmpty())
						cases.add(current);
					current = new LinkedHashMap<>();
					continue;
				}
				int eq = line.indexOf(" = ");
				current.put(line.substring(0, eq), line.substring(eq + 3));
			}
			if (!current.isEmpty())
				cases.add(current);
		}
		assertEquals(false, cases.isEmpty(), "no vectors in " + file);
		return cases.stream().map(c -> new Object[] {
				c.get("parameterSet") + " tcId " + c.get("tcId") + (c.containsKey("reason") ? " (" + c.get("reason") + ")" : ""),
				c });
	}

	private static MLKEMParameters params(Map<String, String> v)
	{
		return switch (v.get("parameterSet"))
		{
			case "ML-KEM-768" -> MLKEMParameters.ml_kem_768;
			case "ML-KEM-1024" -> MLKEMParameters.ml_kem_1024;
			default -> throw new IllegalArgumentException(v.get("parameterSet"));
		};
	}

	private static byte[] hex(Map<String, String> v, String key)
	{
		return Hex.decode(v.get(key));
	}
}
