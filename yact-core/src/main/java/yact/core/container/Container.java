/*******************************************************************************
 * YaCT | (c) 2015-2026 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package yact.core.container;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import yact.core.codec.FormatException;
import yact.core.crypto.Argon2Params;
import yact.core.crypto.CipherSuite;
import yact.core.crypto.Passwords;
import yact.core.format.ContainerFormatException;
import yact.core.format.ContainerHeader;
import yact.core.format.IntegrityException;
import yact.core.format.InvalidCredentialsException;
import yact.core.format.UnlockedHeader;
import yact.core.pq.HybridKeyPair;
import yact.core.pq.HybridPublicKey;
import yact.core.stream.ChunkedAeadInputStream;
import yact.core.stream.ChunkedAeadOutputStream;

/**
 * An open YaCT container: an encrypted file vault.
 *
 * <p>File layout:
 * <pre>
 * 0          header copy A (64 KiB)
 * 65536      header copy B (64 KiB)
 * 131072     superblock slot 0 (128 bytes)
 * 131200     superblock slot 1 (128 bytes)
 * 131328     data: encrypted file contents and index versions, append-only
 * </pre>
 *
 * <p><b>Crash safety.</b> File contents and the index are only ever appended.
 * {@link #commit()} first forces them to disk, then writes the other
 * superblock slot. Until that slot is written, the previous state is current;
 * a crash at any point leaves either the old or the new state. Header changes
 * (key slots) write copy B, then copy A; opening repairs a copy that differs.
 *
 * <p><b>Deleting.</b> Deleted and replaced files keep their space until
 * {@link #compact()} rewrites the container. Until then, someone with the
 * container file <em>and</em> its key can recover deleted files. Compaction
 * cannot erase copies the file system or an SSD kept elsewhere.
 *
 * <p>Changes are buffered in memory until {@link #commit()} or
 * {@link #close()}. Instances are not thread-safe; the file is locked against
 * other processes while open.
 */
public final class Container implements Closeable
{
	static final long HEADER_B_OFFSET = ContainerHeader.AREA_SIZE;
	static final long SUPERBLOCK_OFFSET = 2L * ContainerHeader.AREA_SIZE;
	static final long DATA_OFFSET = SUPERBLOCK_OFFSET + 2L * Superblock.SIZE;

	private static final byte[] FILE_AAD = "YaCT v1 file".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] INDEX_AAD = "YaCT v1 index".getBytes(StandardCharsets.US_ASCII);
	private static final SecureRandom RANDOM = new SecureRandom();

	/** Opens a key slot; implemented by password and private key. */
	@FunctionalInterface
	private interface Unlocker
	{
		UnlockedHeader unlock(ContainerHeader header) throws InvalidCredentialsException, IntegrityException;
	}

	private Path file;
	private FileChannel channel;
	private FileLock lock;
	private final UnlockedHeader keys;
	private TreeMap<String, Index.Item> items;
	private Superblock current;
	private boolean dirty;
	private boolean closed;

	private Container(Path file, FileChannel channel, FileLock lock, UnlockedHeader keys)
	{
		this.file = file;
		this.channel = channel;
		this.lock = lock;
		this.keys = keys;
	}

	// ================================================================== create / open

	/** Creates a new, empty container protected by a password. Fails if {@code file} exists. */
	public static Container create(Path file, CipherSuite suite, char[] password, Argon2Params params)
			throws IOException
	{
		return create(file, UnlockedHeader.create(suite, ContainerHeader.DEFAULT_CHUNK_EXPONENT, password, params));
	}

	/** Creates a new, empty container that only the owner of {@code recipient}'s private key can open. */
	public static Container create(Path file, CipherSuite suite, HybridPublicKey recipient) throws IOException
	{
		return create(file, UnlockedHeader.create(suite, ContainerHeader.DEFAULT_CHUNK_EXPONENT, recipient));
	}

	private static Container create(Path file, UnlockedHeader keys) throws IOException
	{
		FileChannel ch = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ,
				StandardOpenOption.WRITE);
		Container c = null;
		try
		{
			FileLock lock = lockOrFail(ch);
			c = new Container(file, ch, lock, keys);
			c.writeHeaderCopies();
			c.items = new TreeMap<>();
			c.current = new Superblock(0, new byte[Superblock.SALT_LENGTH], 0, 0);
			ch.write(ByteBuffer.allocate(2 * Superblock.SIZE), SUPERBLOCK_OFFSET);
			c.dirty = true;
			c.commit();
			return c;
		}
		catch (IOException | RuntimeException e)
		{
			keys.close();
			ch.close();
			Files.deleteIfExists(file);
			throw e;
		}
	}

	/**
	 * Opens a container with a password.
	 *
	 * @throws InvalidCredentialsException if the password is wrong
	 * @throws IntegrityException          if the container was modified or damaged
	 * @throws ContainerFormatException    if the file is not a YaCT container
	 */
	public static Container open(Path file, char[] password) throws IOException, InvalidCredentialsException
	{
		return open(file, h -> h.unlock(password));
	}

	/** Opens a container with a private key. Exceptions as for {@link #open(Path, char[])}. */
	public static Container open(Path file, HybridKeyPair key) throws IOException, InvalidCredentialsException
	{
		return open(file, h -> h.unlock(key));
	}

	private static Container open(Path file, Unlocker unlocker) throws IOException, InvalidCredentialsException
	{
		FileChannel ch = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE);
		try
		{
			FileLock lock = lockOrFail(ch);
			if (ch.size() < DATA_OFFSET)
				throw new ContainerFormatException("file too short for a YaCT container");

			byte[] areaA = ChannelIO.readFully(ch, 0, ContainerHeader.AREA_SIZE);
			byte[] areaB = ChannelIO.readFully(ch, HEADER_B_OFFSET, ContainerHeader.AREA_SIZE);
			UnlockedHeader keys = unlockEither(areaA, areaB, unlocker);

			Container c = new Container(file, ch, lock, keys);
			try
			{
				c.repairHeaderCopies(areaA, areaB);
				c.loadIndex();
				return c;
			}
			catch (IOException | RuntimeException e)
			{
				keys.close();
				throw e;
			}
		}
		catch (IOException | InvalidCredentialsException | RuntimeException e)
		{
			ch.close();
			throw e;
		}
	}

	/** Tries header copy A, then B (unless identical). An integrity error outranks a wrong password. */
	private static UnlockedHeader unlockEither(byte[] areaA, byte[] areaB, Unlocker unlocker)
			throws IOException, InvalidCredentialsException
	{
		Exception failureA;
		try
		{
			return unlocker.unlock(ContainerHeader.parse(areaA));
		}
		catch (ContainerFormatException | InvalidCredentialsException | IntegrityException e)
		{
			failureA = e;
		}
		Exception failure = failureA;
		if (!Arrays.equals(areaA, areaB))
		{
			try
			{
				return unlocker.unlock(ContainerHeader.parse(areaB));
			}
			catch (ContainerFormatException | InvalidCredentialsException | IntegrityException failureB)
			{
				failure = worse(failureA, failureB);
			}
		}
		if (failure instanceof InvalidCredentialsException ic)
			throw ic;
		throw (IOException) failure;
	}

	/** Integrity error, then wrong credentials, then format error. */
	private static Exception worse(Exception a, Exception b)
	{
		for (Class<?> c : List.of(IntegrityException.class, InvalidCredentialsException.class))
		{
			if (c.isInstance(a))
				return a;
			if (c.isInstance(b))
				return b;
		}
		return a;
	}

	private static FileLock lockOrFail(FileChannel ch) throws IOException
	{
		FileLock lock;
		try
		{
			lock = ch.tryLock();
		}
		catch (java.nio.channels.OverlappingFileLockException e)
		{
			lock = null; // already open in this JVM
		}
		if (lock == null)
			throw new IOException("container is already open");
		return lock;
	}

	// ================================================================== reading

	/** All files, sorted by path. */
	public List<ContainerEntry> list()
	{
		checkOpen();
		return items.values().stream().map(Index.Item::entry).toList();
	}

	/** Files directly or indirectly below {@code directory} ("" for all), sorted by path. */
	public List<ContainerEntry> list(String directory)
	{
		checkOpen();
		String dir = VaultPaths.normalizeDirectory(directory);
		return items.values().stream().map(Index.Item::entry).filter(e -> VaultPaths.isWithin(e.path(), dir)).toList();
	}

	public Optional<ContainerEntry> entry(String path)
	{
		checkOpen();
		Index.Item it = items.get(VaultPaths.normalize(path));
		return it == null ? Optional.empty() : Optional.of(it.entry());
	}

	/**
	 * Opens a file for reading. The stream fails with {@link IntegrityException}
	 * if the content was modified; data is only complete once it returns -1.
	 * Do not modify the container while the stream is open.
	 */
	public InputStream read(String path) throws IOException
	{
		checkOpen();
		Index.Item it = items.get(VaultPaths.normalize(path));
		if (it == null)
			throw new NoSuchFileException(path);
		return verifiedSize(openStream(it.salt(), it.offset(), it.sealedLength(), FILE_AAD), it.entry().size());
	}

	/**
	 * Decrypts one file to {@code target}. The content is written to a
	 * temporary file next to the target and only moved into place once it
	 * has been fully verified, so no partial file is left behind.
	 */
	public void extract(String path, Path target, boolean overwrite) throws IOException
	{
		checkOpen();
		String p = VaultPaths.normalize(path);
		Index.Item it = items.get(p);
		if (it == null)
			throw new NoSuchFileException(path);
		if (!overwrite && Files.exists(target, LinkOption.NOFOLLOW_LINKS))
			throw new FileAlreadyExistsException(target.toString());

		Path dir = target.toAbsolutePath().getParent();
		Files.createDirectories(dir);
		Path tmp = Files.createTempFile(dir, ".yact-", ".part");
		try
		{
			try (InputStream in = read(p); OutputStream out = Files.newOutputStream(tmp))
			{
				in.transferTo(out);
			}
			Files.setLastModifiedTime(tmp, FileTime.from(it.entry().modified()));
			if (overwrite)
				Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			else
				Files.move(tmp, target); // fails if the target appeared in the meantime
		}
		finally
		{
			Files.deleteIfExists(tmp);
		}
	}

	/**
	 * Decrypts all files below {@code directory} ("" for all) into
	 * {@code targetDir}, keeping their relative paths.
	 */
	public void extractAll(String directory, Path targetDir, boolean overwrite) throws IOException
	{
		checkOpen();
		Path base = targetDir.toAbsolutePath().normalize();
		String dir = VaultPaths.normalizeDirectory(directory);
		for (ContainerEntry e : list(dir))
		{
			String relative = dir.isEmpty() ? e.path() : e.path().substring(dir.length() + 1);
			Path target = base.resolve(relative.replace('/', base.getFileSystem().getSeparator().charAt(0))).normalize();
			// Paths are validated on read and on add; this is a second line of defence.
			if (!target.startsWith(base))
				throw new IOException("refusing to write outside the target directory: " + e.path());
			extract(e.path(), target, overwrite);
		}
	}

	// ================================================================== writing

	/**
	 * Adds a file from a stream.
	 *
	 * @param overwrite replace an existing file at the same path
	 * @throws FileAlreadyExistsException if the path exists and {@code overwrite} is false
	 * @throws IllegalArgumentException   if the path is invalid, or a file and a directory would share a name
	 */
	public void add(String path, InputStream content, Instant modified, boolean overwrite) throws IOException
	{
		checkOpen();
		String p = VaultPaths.normalize(path);
		if (items.containsKey(p) && !overwrite)
			throw new FileAlreadyExistsException(p);
		checkNoFileDirectoryClash(p);

		byte[] salt = new byte[Superblock.SALT_LENGTH];
		RANDOM.nextBytes(salt);
		long offset = channel.size();
		long[] plainSize = new long[1];
		long sealedLength = writeStream(salt, offset, FILE_AAD, out -> plainSize[0] = content.transferTo(out));

		items.put(p, new Index.Item(new ContainerEntry(p, plainSize[0], modified), salt, offset, sealedLength));
		dirty = true;
	}

	/**
	 * Imports a file, or a directory tree, from disk into {@code targetDirectory}
	 * ("" for the root). Symbolic links are skipped and never followed.
	 */
	public void importPath(Path source, String targetDirectory, boolean overwrite) throws IOException
	{
		checkOpen();
		String dir = VaultPaths.normalizeDirectory(targetDirectory);
		String name = source.toAbsolutePath().normalize().getFileName().toString();
		importRecursive(source, VaultPaths.join(dir, name), overwrite);
	}

	private void importRecursive(Path source, String target, boolean overwrite) throws IOException
	{
		if (Files.isSymbolicLink(source))
			return;
		if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS))
		{
			try (DirectoryStream<Path> children = Files.newDirectoryStream(source))
			{
				for (Path child : children)
					importRecursive(child, target + "/" + child.getFileName(), overwrite);
			}
		}
		else if (Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS))
		{
			Instant modified = Files.getLastModifiedTime(source, LinkOption.NOFOLLOW_LINKS).toInstant();
			try (InputStream in = Files.newInputStream(source, LinkOption.NOFOLLOW_LINKS))
			{
				add(target, in, modified, overwrite);
			}
		}
	}

	/** Deletes one file. The space is reclaimed by {@link #compact()}. */
	public void delete(String path) throws IOException
	{
		checkOpen();
		String p = VaultPaths.normalize(path);
		if (items.remove(p) == null)
			throw new NoSuchFileException(path);
		dirty = true;
	}

	/** Deletes all files below {@code directory}; returns how many. */
	public int deleteDirectory(String directory)
	{
		checkOpen();
		String dir = VaultPaths.normalize(directory);
		int before = items.size();
		items.keySet().removeIf(p -> VaultPaths.isWithin(p, dir));
		dirty |= items.size() != before;
		return before - items.size();
	}

	/** Renames or moves a file. Only the index changes; the content is not rewritten. */
	public void rename(String from, String to) throws IOException
	{
		checkOpen();
		String f = VaultPaths.normalize(from);
		String t = VaultPaths.normalize(to);
		Index.Item it = items.get(f);
		if (it == null)
			throw new NoSuchFileException(from);
		if (f.equals(t))
			return;
		if (items.containsKey(t))
			throw new FileAlreadyExistsException(t);
		items.remove(f);
		try
		{
			checkNoFileDirectoryClash(t);
		}
		catch (IllegalArgumentException e)
		{
			items.put(f, it);
			throw e;
		}
		ContainerEntry e = it.entry();
		items.put(t, new Index.Item(new ContainerEntry(t, e.size(), e.modified()), it.salt(), it.offset(), it.sealedLength()));
		dirty = true;
	}

	/** A file "a" and a file "a/b" cannot coexist: "a" would have to be both a file and a directory. */
	private void checkNoFileDirectoryClash(String p)
	{
		for (int i = p.indexOf('/'); i >= 0; i = p.indexOf('/', i + 1))
			if (items.containsKey(p.substring(0, i)))
				throw new IllegalArgumentException("'" + p.substring(0, i) + "' is a file, not a directory");
		String child = items.ceilingKey(p + "/");
		if (child != null && child.startsWith(p + "/"))
			throw new IllegalArgumentException("'" + p + "' is a directory");
	}

	/** True if there are changes that {@link #commit()} has not yet persisted. */
	public boolean hasUncommittedChanges()
	{
		return dirty;
	}

	/**
	 * Persists all changes: writes a new index, forces data to disk, then
	 * switches the superblock. Does nothing if nothing changed.
	 */
	public void commit() throws IOException
	{
		checkOpen();
		if (!dirty)
			return;
		byte[] indexSalt = new byte[Superblock.SALT_LENGTH];
		RANDOM.nextBytes(indexSalt);
		byte[] index = Index.encode(items);
		long offset = Math.max(channel.size(), DATA_OFFSET);
		long length = writeStream(indexSalt, offset, INDEX_AAD, out -> out.write(index));
		channel.force(true);

		Superblock next = new Superblock(current.generation() + 1, indexSalt, offset, length);
		ChannelIO.writeFully(channel, next.encode(keys, RANDOM), SUPERBLOCK_OFFSET + (long) next.slot() * Superblock.SIZE);
		channel.force(true);
		current = next;
		dirty = false;
	}

	// ================================================================== maintenance

	/** Bytes held by deleted or replaced files and old index versions; reclaimed by {@link #compact()}. */
	public long unusedBytes() throws IOException
	{
		checkOpen();
		long live = current.indexLength();
		for (Index.Item it : items.values())
			live += it.sealedLength();
		return channel.size() - DATA_OFFSET - live;
	}

	/**
	 * Rewrites the container without unused space. Commits first. The new
	 * file is built next to the old one and atomically replaces it, so a crash
	 * leaves the old container untouched. Encrypted contents are copied as they
	 * are; nothing is decrypted.
	 */
	public void compact() throws IOException
	{
		commit();
		Path dir = file.toAbsolutePath().getParent();
		Path tmp = dir.resolve("." + file.getFileName() + ".compact-" + HexFormat.of().formatHex(randomBytes(6)));
		TreeMap<String, Index.Item> moved = new TreeMap<>();
		Superblock sb;
		try (FileChannel out = FileChannel.open(tmp, StandardOpenOption.CREATE_NEW, StandardOpenOption.READ,
				StandardOpenOption.WRITE))
		{
			copyRange(channel, out, 0, 2L * ContainerHeader.AREA_SIZE, 0);
			out.write(ByteBuffer.allocate(2 * Superblock.SIZE), SUPERBLOCK_OFFSET);

			long pos = DATA_OFFSET;
			for (Index.Item it : items.values())
			{
				copyRange(channel, out, it.offset(), it.sealedLength(), pos);
				moved.put(it.entry().path(), new Index.Item(it.entry(), it.salt(), pos, it.sealedLength()));
				pos += it.sealedLength();
			}
			byte[] indexSalt = randomBytes(Superblock.SALT_LENGTH);
			byte[] index = Index.encode(moved);
			long length = writeStream(out, indexSalt, pos, INDEX_AAD, o -> o.write(index));
			// The generation keeps counting up, so the new file is never mistaken for an older state.
			sb = new Superblock(current.generation() + 1, indexSalt, pos, length);
			ChannelIO.writeFully(out, sb.encode(keys, RANDOM), SUPERBLOCK_OFFSET + (long) sb.slot() * Superblock.SIZE);
			out.force(true);
		}
		catch (IOException | RuntimeException e)
		{
			Files.deleteIfExists(tmp);
			throw e;
		}

		lock.release();
		channel.close();
		try
		{
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			items = moved;
			current = sb;
		}
		catch (IOException e)
		{
			Files.deleteIfExists(tmp); // the old container is unchanged and stays in use
			throw e;
		}
		finally
		{
			channel = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE);
			lock = lockOrFail(channel);
		}
	}

	// ================================================================== key slots

	/** The current header, e.g. to show the key slots. */
	public ContainerHeader header()
	{
		checkOpen();
		return keys.header();
	}

	public void addPasswordSlot(char[] password, Argon2Params params) throws IOException
	{
		checkOpen();
		keys.addPasswordSlot(password, params);
		writeHeaderCopies();
	}

	public void addPublicKeySlot(HybridPublicKey recipient) throws IOException
	{
		checkOpen();
		keys.addPublicKeySlot(recipient);
		writeHeaderCopies();
	}

	/** See {@link UnlockedHeader#removeKeySlot(int)} for what removing a slot does and does not achieve. */
	public void removeKeySlot(int index) throws IOException
	{
		checkOpen();
		keys.removeKeySlot(index);
		writeHeaderCopies();
	}

	/** Writes copy B, then copy A, each forced to disk, so one valid copy always exists. */
	private void writeHeaderCopies() throws IOException
	{
		byte[] area = keys.header().encode();
		ChannelIO.writeFully(channel, area, HEADER_B_OFFSET);
		channel.force(true);
		ChannelIO.writeFully(channel, area, 0);
		channel.force(true);
	}

	/** After a crash during a header change the copies differ; make both equal to the one that opened. */
	private void repairHeaderCopies(byte[] areaA, byte[] areaB) throws IOException
	{
		byte[] good = keys.header().encode();
		if (!Arrays.equals(good, areaA) || !Arrays.equals(good, areaB))
			writeHeaderCopies();
	}

	// ================================================================== internals

	private void loadIndex() throws IOException
	{
		Superblock a = Superblock.decode(ChannelIO.readFully(channel, SUPERBLOCK_OFFSET, Superblock.SIZE), keys);
		Superblock b = Superblock.decode(ChannelIO.readFully(channel, SUPERBLOCK_OFFSET + Superblock.SIZE, Superblock.SIZE), keys);
		Superblock sb = a == null ? b : b == null ? a : a.generation() >= b.generation() ? a : b;
		if (sb == null)
			throw new IntegrityException("no valid superblock");

		long size = channel.size();
		checkRange(sb.indexOffset(), sb.indexLength(), size);
		byte[] data;
		try (InputStream in = openStream(sb.indexSalt(), sb.indexOffset(), sb.indexLength(), INDEX_AAD))
		{
			data = in.readAllBytes();
		}
		try
		{
			items = Index.decode(data);
		}
		catch (FormatException e)
		{
			throw new IntegrityException("index is inconsistent: " + e.getMessage());
		}
		for (Index.Item it : items.values())
			checkRange(it.offset(), it.sealedLength(), size);
		current = sb;
	}

	private static void checkRange(long offset, long length, long fileSize) throws IntegrityException
	{
		if (offset < DATA_OFFSET || length < 0 || offset > fileSize - length)
			throw new IntegrityException("container file is truncated");
	}

	private InputStream openStream(byte[] salt, long offset, long length, byte[] aad)
	{
		byte[] key = keys.streamKey(salt);
		try
		{
			return new ChunkedAeadInputStream(ChannelIO.input(channel, offset, length), keys.header().cipherSuite(), key,
					aad, keys.header().chunkSize());
		}
		finally
		{
			Passwords.wipe(key);
		}
	}

	@FunctionalInterface
	private interface StreamBody
	{
		void write(OutputStream out) throws IOException;
	}

	private long writeStream(byte[] salt, long offset, byte[] aad, StreamBody body) throws IOException
	{
		return writeStream(channel, salt, offset, aad, body);
	}

	/** Encrypts {@code body}'s output at {@code offset} of {@code ch}; returns the encrypted length. */
	private long writeStream(FileChannel ch, byte[] salt, long offset, byte[] aad, StreamBody body) throws IOException
	{
		ChannelIO.Output raw = new ChannelIO.Output(ch, offset);
		byte[] key = keys.streamKey(salt);
		try
		{
			ChunkedAeadOutputStream enc = new ChunkedAeadOutputStream(raw, keys.header().cipherSuite(), key, aad,
					keys.header().chunkSize());
			body.write(enc);
			enc.finish();
		}
		finally
		{
			Passwords.wipe(key);
		}
		return raw.written();
	}

	/** Wraps a content stream so that a length differing from the index is an integrity error. */
	private static InputStream verifiedSize(InputStream in, long expected)
	{
		return new java.io.FilterInputStream(in)
		{
			private long count;

			@Override
			public int read() throws IOException
			{
				byte[] one = new byte[1];
				return read(one, 0, 1) == -1 ? -1 : one[0] & 0xFF;
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException
			{
				int n = super.read(b, off, len);
				if (n > 0)
					count += n;
				if (count > expected || (n == -1 && count != expected))
					throw new IntegrityException("file size does not match the index");
				return n;
			}
		};
	}

	private static void copyRange(FileChannel from, FileChannel to, long position, long length, long target)
			throws IOException
	{
		long done = 0;
		while (done < length)
		{
			long n = to.transferFrom(from.position(position + done), target + done, length - done);
			if (n <= 0)
				throw new IOException("unexpected end of container file");
			done += n;
		}
	}

	private static byte[] randomBytes(int n)
	{
		byte[] b = new byte[n];
		RANDOM.nextBytes(b);
		return b;
	}

	private void checkOpen()
	{
		if (closed)
			throw new IllegalStateException("container is closed");
	}

	public Path file()
	{
		return file;
	}

	/** Commits pending changes, wipes the keys and releases the file. */
	@Override
	public void close() throws IOException
	{
		if (closed)
			return;
		try
		{
			commit();
		}
		finally
		{
			closed = true;
			keys.close();
			try
			{
				if (lock.isValid())
					lock.release();
			}
			finally
			{
				channel.close();
			}
		}
	}
}
