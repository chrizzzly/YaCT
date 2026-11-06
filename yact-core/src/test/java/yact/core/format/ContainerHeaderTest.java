/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.format;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import yact.core.crypto.Argon2Params;
import yact.core.crypto.CipherSuite;

class ContainerHeaderTest
{
	/** Far too weak for real use; keeps the tests fast. */
	private static final Argon2Params FAST = new Argon2Params(64, 1, 1);
	private static final char[] PASSWORD = "correct horse battery staple".toCharArray();

	// Offsets into the header area, see the layout in ContainerHeader.
	private static final int OFFSET_VERSION = 4;
	private static final int OFFSET_SUITE = 6;
	private static final int OFFSET_CHUNK = 7;
	private static final int OFFSET_SLOT_COUNT = 8 + 16;
	private static final int OFFSET_FIRST_SLOT_BODY = OFFSET_SLOT_COUNT + 1 + 3;
	private static final int PASSWORD_SLOT_BODY_LENGTH = 4 + 4 + 1 + 16 + 12 + 48;
	private static final int OFFSET_MAC_ONE_SLOT = OFFSET_FIRST_SLOT_BODY + PASSWORD_SLOT_BODY_LENGTH;

	private static UnlockedHeader create()
	{
		return UnlockedHeader.create(CipherSuite.AES_256_GCM, ContainerHeader.DEFAULT_CHUNK_EXPONENT, PASSWORD, FAST);
	}

	@ParameterizedTest
	@EnumSource(CipherSuite.class)
	void roundTrip(CipherSuite suite) throws Exception
	{
		try (UnlockedHeader created = UnlockedHeader.create(suite, 20, PASSWORD, FAST))
		{
			byte[] area = created.header().encode();
			assertEquals(ContainerHeader.AREA_SIZE, area.length);

			ContainerHeader read = ContainerHeader.read(new ByteArrayInputStream(area));
			assertEquals(suite, read.cipherSuite());
			assertEquals(1 << 20, read.chunkSize());
			assertArrayEquals(created.header().containerId(), read.containerId());
			assertEquals(FAST, ((PasswordKeySlot) read.keySlots().get(0)).params());

			try (UnlockedHeader opened = read.unlock(PASSWORD))
			{
				byte[] salt = new byte[16];
				assertArrayEquals(created.streamKey(salt), opened.streamKey(salt), "same master key");
			}
		}
	}

	@Test
	void wrongPasswordIsRejected() throws Exception
	{
		ContainerHeader header = ContainerHeader.parse(create().header().encode());
		assertThrows(InvalidCredentialsException.class, () -> header.unlock("wrong".toCharArray()));
	}

	@Test
	void unicodePasswordIsNormalized() throws Exception
	{
		byte[] area = UnlockedHeader.create(CipherSuite.AES_256_GCM, 16, "Käse".toCharArray(), FAST).header().encode();
		assertNotNull(ContainerHeader.parse(area).unlock("Käse".toCharArray()));
	}

	@Test
	void modifiedHeaderFieldIsDetectedAfterUnlock() throws Exception
	{
		byte[] area = create().header().encode();
		area[OFFSET_CHUNK] = 17; // still a valid chunk size, but not the one that was signed
		ContainerHeader header = ContainerHeader.parse(area);
		assertThrows(IntegrityException.class, () -> header.unlock(PASSWORD));
	}

	@Test
	void modifiedMacIsDetected() throws Exception
	{
		byte[] area = create().header().encode();
		area[OFFSET_MAC_ONE_SLOT + 31] ^= 1;
		ContainerHeader header = ContainerHeader.parse(area);
		assertThrows(IntegrityException.class, () -> header.unlock(PASSWORD));
	}

	@Test
	void modifiedKeySlotDoesNotOpen() throws Exception
	{
		byte[] area = create().header().encode();
		area[OFFSET_FIRST_SLOT_BODY + 9 + 3] ^= 1; // inside the salt
		ContainerHeader header = ContainerHeader.parse(area);
		assertThrows(InvalidCredentialsException.class, () -> header.unlock(PASSWORD));
	}

	@Test
	void swappedCipherSuiteDoesNotOpen() throws Exception
	{
		byte[] area = create().header().encode();
		area[OFFSET_SUITE] = (byte) CipherSuite.CHACHA20_POLY1305.id();
		ContainerHeader header = ContainerHeader.parse(area);
		assertThrows(InvalidCredentialsException.class, () -> header.unlock(PASSWORD));
	}

	@Test
	void malformedHeadersAreRejectedWhileParsing()
	{
		byte[] good = create().header().encode();

		assertFormatError(new byte[ContainerHeader.AREA_SIZE]);                    // no magic
		assertFormatError(patch(good, OFFSET_VERSION + 1, 2));                     // version 2
		assertFormatError(patch(good, OFFSET_SUITE, 99));                          // unknown cipher
		assertFormatError(patch(good, OFFSET_CHUNK, 40));                          // chunk too large
		assertFormatError(patch(good, OFFSET_SLOT_COUNT, 0));                      // zero key slots
		assertFormatError(patch(good, OFFSET_SLOT_COUNT, 17));                     // too many key slots
		assertFormatError(patch(good, OFFSET_FIRST_SLOT_BODY, 0x7F));              // Argon2 memory > 4 GiB
		assertThrows(ContainerFormatException.class,
				() -> ContainerHeader.read(new ByteArrayInputStream(good, 0, ContainerHeader.AREA_SIZE - 1)));
	}

	@Test
	void secondPasswordAndPasswordChange() throws Exception
	{
		char[] second = "second password".toCharArray();
		byte[] area;
		try (UnlockedHeader h = create())
		{
			h.addPasswordSlot(second, FAST);
			area = h.header().encode();
		}
		ContainerHeader both = ContainerHeader.parse(area);
		assertEquals(2, both.keySlots().size());
		assertNotNull(both.unlock(PASSWORD));

		// Password change: remove the original slot, keep the second.
		try (UnlockedHeader h = both.unlock(second))
		{
			h.removeKeySlot(0);
			area = h.header().encode();
		}
		ContainerHeader changed = ContainerHeader.parse(area);
		assertThrows(InvalidCredentialsException.class, () -> changed.unlock(PASSWORD));
		assertNotNull(changed.unlock(second));
	}

	@Test
	void lastKeySlotCannotBeRemoved()
	{
		assertThrows(IllegalStateException.class, () -> create().removeKeySlot(0));
	}

	@Test
	void sixteenSlotsFitAndNoMore()
	{
		UnlockedHeader h = create();
		for (int i = 1; i < ContainerHeader.MAX_KEY_SLOTS; i++)
			h.addPasswordSlot(("pw" + i).toCharArray(), FAST);
		assertEquals(ContainerHeader.MAX_KEY_SLOTS, h.header().keySlots().size());
		assertThrows(IllegalStateException.class, () -> h.addPasswordSlot("one more".toCharArray(), FAST));
	}

	@Test
	void unknownKeySlotsSurviveAHeaderRewrite() throws Exception
	{
		UnlockedHeader h = create();
		UnknownKeySlot future = new UnknownKeySlot(200, new byte[] { 1, 2, 3 });
		ContainerHeader withFuture = ContainerHeader.sign(h.header().cipherSuite(), h.header().chunkExponent(),
				h.header().containerId(), List.of(h.header().keySlots().get(0), future), testMasterKey(h));

		try (UnlockedHeader opened = ContainerHeader.parse(withFuture.encode()).unlock(PASSWORD))
		{
			opened.addPasswordSlot("another".toCharArray(), FAST);
			KeySlot kept = ContainerHeader.parse(opened.header().encode()).keySlots().get(1);
			assertInstanceOf(UnknownKeySlot.class, kept);
			assertEquals(200, kept.type());
			assertArrayEquals(new byte[] { 1, 2, 3 }, kept.encodeBody());
		}
	}

	@Test
	void streamKeysDependOnSaltAndContainer()
	{
		UnlockedHeader a = create();
		UnlockedHeader b = create();
		byte[] s1 = new byte[16];
		byte[] s2 = new byte[16];
		s2[0] = 1;
		assertArrayEquals(a.streamKey(s1), a.streamKey(s1));
		assertFalse(java.util.Arrays.equals(a.streamKey(s1), a.streamKey(s2)));
		assertFalse(java.util.Arrays.equals(a.streamKey(s1), b.streamKey(s1)));
		assertThrows(IllegalArgumentException.class, () -> a.streamKey(new byte[15]));
	}

	@Test
	void closedHeaderCannotBeUsed()
	{
		UnlockedHeader h = create();
		h.close();
		assertTrue(h.isDestroyed());
		assertThrows(IllegalStateException.class, () -> h.streamKey(new byte[16]));
		assertThrows(IllegalStateException.class, h::header);
	}

	// ------------------------------------------------------------------ helpers

	/** Reads the master key; only needed to sign a header containing a hand-made slot. */
	private static byte[] testMasterKey(UnlockedHeader h) throws Exception
	{
		java.lang.reflect.Field f = UnlockedHeader.class.getDeclaredField("masterKey");
		f.setAccessible(true);
		return ((byte[]) f.get(h)).clone();
	}

	private static byte[] patch(byte[] area, int offset, int value)
	{
		byte[] copy = area.clone();
		copy[offset] = (byte) value;
		return copy;
	}

	private static void assertFormatError(byte[] area)
	{
		assertThrows(ContainerFormatException.class, () -> ContainerHeader.parse(area));
	}
}
