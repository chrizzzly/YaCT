/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import yact.core.crypto.CipherSuite;
import yact.core.format.IntegrityException;

class ChunkedAeadStreamTest
{
	private static final int CHUNK = 64;
	private static final int SEALED = CHUNK + CipherSuite.TAG_LENGTH;
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final byte[] KEY = random(32);
	private static final byte[] AAD = "file-42".getBytes();

	static Stream<Arguments> sizes()
	{
		return Arrays.stream(CipherSuite.values()).flatMap(suite -> Stream.of(
				0, 1, CHUNK - 1, CHUNK, CHUNK + 1, 3 * CHUNK, 3 * CHUNK + 7).map(n -> Arguments.of(suite, n)));
	}

	@ParameterizedTest(name = "{0}, {1} bytes")
	@MethodSource("sizes")
	void roundTrip(CipherSuite suite, int size) throws IOException
	{
		byte[] plain = random(size);
		byte[] sealed = encrypt(suite, plain);

		int chunks = Math.max(1, (size + CHUNK - 1) / CHUNK);
		assertEquals(size + chunks * CipherSuite.TAG_LENGTH, sealed.length, "overhead is one tag per chunk");
		assertArrayEquals(plain, decrypt(suite, sealed));
	}

	@Test
	void writesInOddPiecesGiveTheSameResult() throws IOException
	{
		byte[] plain = random(5 * CHUNK + 3);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ChunkedAeadOutputStream out = new ChunkedAeadOutputStream(bytes, CipherSuite.AES_256_GCM, KEY, AAD, CHUNK))
		{
			int pos = 0;
			for (int step = 1; pos < plain.length; step = step * 3 % 97 + 1)
			{
				int n = Math.min(step, plain.length - pos);
				if (n == 1)
					out.write(plain[pos]);
				else
					out.write(plain, pos, n);
				pos += n;
			}
		}
		assertArrayEquals(plain, decrypt(CipherSuite.AES_256_GCM, bytes.toByteArray()));
	}

	@Test
	void bitFlipIsDetected()
	{
		byte[] sealed = encrypt(CipherSuite.AES_256_GCM, random(3 * CHUNK));
		sealed[SEALED + 5] ^= 1;
		assertIntegrityError(sealed);
	}

	@Test
	void missingFinalChunkAtChunkBoundaryIsDetected()
	{
		byte[] sealed = encrypt(CipherSuite.AES_256_GCM, random(3 * CHUNK + 10));
		assertIntegrityError(Arrays.copyOf(sealed, 3 * SEALED));
	}

	@Test
	void truncationInsideAChunkIsDetected()
	{
		byte[] sealed = encrypt(CipherSuite.AES_256_GCM, random(3 * CHUNK));
		assertIntegrityError(Arrays.copyOf(sealed, sealed.length - 1));
		assertIntegrityError(Arrays.copyOf(sealed, 5));
	}

	@Test
	void emptyInputIsNotAValidStream()
	{
		assertIntegrityError(new byte[0]);
	}

	@Test
	void appendedDataIsDetected()
	{
		byte[] sealed = encrypt(CipherSuite.AES_256_GCM, random(2 * CHUNK));
		assertIntegrityError(Arrays.copyOf(sealed, sealed.length + 1));
		// Appending a genuine chunk from another stream under the same key.
		byte[] other = encrypt(CipherSuite.AES_256_GCM, random(CHUNK));
		byte[] joined = Arrays.copyOf(sealed, sealed.length + other.length);
		System.arraycopy(other, 0, joined, sealed.length, other.length);
		assertIntegrityError(joined);
	}

	@Test
	void reorderedChunksAreDetected()
	{
		byte[] sealed = encrypt(CipherSuite.AES_256_GCM, random(3 * CHUNK));
		byte[] swapped = sealed.clone();
		System.arraycopy(sealed, 0, swapped, SEALED, SEALED);
		System.arraycopy(sealed, SEALED, swapped, 0, SEALED);
		assertIntegrityError(swapped);
	}

	@Test
	void wrongKeyOrAssociatedDataIsDetected()
	{
		byte[] sealed = encrypt(CipherSuite.AES_256_GCM, random(10));
		assertThrows(IntegrityException.class, () -> readAll(
				new ChunkedAeadInputStream(new ByteArrayInputStream(sealed), CipherSuite.AES_256_GCM, random(32), AAD, CHUNK)));
		assertThrows(IntegrityException.class, () -> readAll(
				new ChunkedAeadInputStream(new ByteArrayInputStream(sealed), CipherSuite.AES_256_GCM, KEY, "file-43".getBytes(), CHUNK)));
	}

	@Test
	void verifiedDataBeforeTheErrorIsReleasedButReadingStopsAtTheError() throws IOException
	{
		byte[] plain = random(3 * CHUNK);
		byte[] sealed = encrypt(CipherSuite.AES_256_GCM, plain);
		sealed[2 * SEALED] ^= 1; // third chunk

		try (InputStream in = new ChunkedAeadInputStream(new ByteArrayInputStream(sealed), CipherSuite.AES_256_GCM, KEY, AAD, CHUNK))
		{
			assertArrayEquals(Arrays.copyOf(plain, 2 * CHUNK), in.readNBytes(2 * CHUNK));
			assertThrows(IntegrityException.class, in::read);
			assertThrows(IntegrityException.class, in::read, "stays failed");
		}
	}

	@Test
	void writingAfterFinishFails() throws IOException
	{
		ChunkedAeadOutputStream out = new ChunkedAeadOutputStream(new ByteArrayOutputStream(), CipherSuite.AES_256_GCM, KEY, AAD, CHUNK);
		out.finish();
		assertThrows(IOException.class, () -> out.write(1));
	}

	// ------------------------------------------------------------------ helpers

	private static byte[] encrypt(CipherSuite suite, byte[] plain)
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ChunkedAeadOutputStream out = new ChunkedAeadOutputStream(bytes, suite, KEY, AAD, CHUNK))
		{
			out.write(plain);
		}
		catch (IOException e)
		{
			throw new AssertionError(e);
		}
		return bytes.toByteArray();
	}

	private static byte[] decrypt(CipherSuite suite, byte[] sealed) throws IOException
	{
		return readAll(new ChunkedAeadInputStream(new ByteArrayInputStream(sealed), suite, KEY, AAD, CHUNK));
	}

	private static byte[] readAll(InputStream in) throws IOException
	{
		try (in)
		{
			return in.readAllBytes();
		}
	}

	private static void assertIntegrityError(byte[] sealed)
	{
		assertThrows(IntegrityException.class, () -> decrypt(CipherSuite.AES_256_GCM, sealed));
	}

	private static byte[] random(int n)
	{
		byte[] b = new byte[n];
		RANDOM.nextBytes(b);
		return b;
	}
}
