/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.crypto;

/**
 * Cost parameters for Argon2id.
 *
 * <p>The limits checked here are hard limits: values outside them are
 * rejected when a container is read, so that a manipulated header cannot make
 * YaCT allocate unbounded memory or loop for hours. Whether parameters are
 * <em>strong enough</em> is a separate question, see {@link #isBelowRecommended()}.
 *
 * @param memoryKiB   memory cost in KiB
 * @param iterations  number of passes
 * @param parallelism number of lanes
 */
public record Argon2Params(int memoryKiB, int iterations, int parallelism)
{
	/** RFC 9106, section 4, second recommended option: 64 MiB, 3 passes, 4 lanes. */
	public static final Argon2Params DEFAULT = new Argon2Params(64 * 1024, 3, 4);

	/** Higher cost for users who accept a slower unlock: 256 MiB, 4 passes, 4 lanes. */
	public static final Argon2Params STRONG = new Argon2Params(256 * 1024, 4, 4);

	public static final int MAX_MEMORY_KIB = 4 * 1024 * 1024; // 4 GiB
	public static final int MAX_ITERATIONS = 64;
	public static final int MAX_PARALLELISM = 16;

	/** OWASP's minimum for Argon2id: 19 MiB, 2 passes. */
	private static final int RECOMMENDED_MIN_MEMORY_KIB = 19 * 1024;
	private static final int RECOMMENDED_MIN_ITERATIONS = 2;

	public Argon2Params
	{
		if (parallelism < 1 || parallelism > MAX_PARALLELISM)
			throw new IllegalArgumentException("parallelism must be 1.." + MAX_PARALLELISM);
		if (iterations < 1 || iterations > MAX_ITERATIONS)
			throw new IllegalArgumentException("iterations must be 1.." + MAX_ITERATIONS);
		// Argon2 itself requires at least 8 KiB per lane.
		if (memoryKiB < 8 * parallelism || memoryKiB > MAX_MEMORY_KIB)
			throw new IllegalArgumentException("memory must be " + (8 * parallelism) + ".." + MAX_MEMORY_KIB + " KiB");
	}

	/** True if these parameters are too weak to protect a password; only acceptable in tests. */
	public boolean isBelowRecommended()
	{
		return memoryKiB < RECOMMENDED_MIN_MEMORY_KIB || iterations < RECOMMENDED_MIN_ITERATIONS;
	}
}
