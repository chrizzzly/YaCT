/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.container;

import java.time.Instant;

/**
 * A file stored in a container, as shown to the user.
 *
 * @param path     normalised path, see {@link VaultPaths}
 * @param size     plaintext size in bytes
 * @param modified last-modified time of the original file
 */
public record ContainerEntry(String path, long size, Instant modified)
{
	/** The last path segment. */
	public String name()
	{
		return path.substring(path.lastIndexOf('/') + 1);
	}
}
