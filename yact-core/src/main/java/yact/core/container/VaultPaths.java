/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.container;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * Rules for paths inside a container.
 *
 * <p>A path is relative, uses {@code /} as separator, is in Unicode NFC, and
 * has no empty, {@code .} or {@code ..} segments and no control characters.
 * These rules make it impossible for a container to write outside the
 * target directory on extraction ("zip slip").
 */
public final class VaultPaths
{
	public static final int MAX_LENGTH = 4096; // UTF-8 bytes

	private VaultPaths()
	{
	}

	/**
	 * Normalises a user-supplied path: backslashes become slashes, leading and
	 * trailing slashes are removed, and the result is checked against the rules.
	 *
	 * @throws IllegalArgumentException if the path is not acceptable
	 */
	public static String normalize(String path)
	{
		String p = Normalizer.normalize(path, Normalizer.Form.NFC).replace('\\', '/');
		while (p.startsWith("/"))
			p = p.substring(1);
		while (p.endsWith("/"))
			p = p.substring(0, p.length() - 1);
		check(p);
		return p;
	}

	/** Normalises a directory prefix; the empty string means the root. */
	static String normalizeDirectory(String dir)
	{
		String d = Normalizer.normalize(dir, Normalizer.Form.NFC).replace('\\', '/');
		while (d.startsWith("/"))
			d = d.substring(1);
		while (d.endsWith("/"))
			d = d.substring(0, d.length() - 1);
		if (!d.isEmpty())
			check(d);
		return d;
	}

	/** True if {@code path} is {@code dir} itself or lies below it; every path lies below the root "". */
	static boolean isWithin(String path, String dir)
	{
		return dir.isEmpty() || path.equals(dir) || path.startsWith(dir + "/");
	}

	static String join(String dir, String name)
	{
		return dir.isEmpty() ? name : dir + "/" + name;
	}

	/** Throws unless {@code p} is already a valid, normalised path. */
	static void check(String p)
	{
		if (p.isEmpty())
			throw new IllegalArgumentException("path is empty");
		if (p.getBytes(StandardCharsets.UTF_8).length > MAX_LENGTH)
			throw new IllegalArgumentException("path is too long");
		if (!Normalizer.isNormalized(p, Normalizer.Form.NFC))
			throw new IllegalArgumentException("path is not in Unicode NFC");
		for (String segment : p.split("/", -1))
		{
			if (segment.isEmpty() || segment.equals(".") || segment.equals(".."))
				throw new IllegalArgumentException("invalid path segment in: " + p);
			for (int i = 0; i < segment.length(); i++)
				if (Character.isISOControl(segment.charAt(i)) || segment.charAt(i) == '\\')
					throw new IllegalArgumentException("invalid character in path: " + p);
		}
	}
}
