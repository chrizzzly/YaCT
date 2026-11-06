/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.format;

/**
 * One way to unlock a container. Every slot holds the same container master
 * key, wrapped (encrypted) under a key that only its credential can produce.
 *
 * <p>The numeric type ids are part of the file format and must never change.
 */
public sealed interface KeySlot permits PasswordKeySlot, UnknownKeySlot
{
	/** Type id written to the header. */
	int type();

	/** The slot's serialised body, without type and length prefix. */
	byte[] encodeBody();
}
