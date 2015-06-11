package filehandling;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.GeneralSecurityException;

import security.Random;
import utils.Algorithms;
import utils.HashAlgorithms;
import utils.Mode;
import application.newContainer.NewContainer;


public class FileCreateContainer 
{
	private char unit;
	private int size;
	private Algorithms ca;
	private Mode mode;
	private HashAlgorithms hash;
	private String path;
	private long offset;
	private long actualSize;
	private byte[] key;
	
	/**
	 * Creates the Environment for the formatting of the new virtual drive
	 * @param unit is the unit after the size "M" or "G"
	 * @param size is the size of the new drive 
	 */
	public FileCreateContainer(NewContainer object) 
	{
		this.unit = object.getUnit();  
		this.size = object.getSize();
		this.ca = object.getAlgorithm();
		this.mode = object.getMode();
		this.path = object.getPath();
		this.hash = object.getHash();
	}

	
	public void doIt() throws GeneralSecurityException
	{
		if(unit == 'M')
			actualSize = size * 1024L * 1024L;
		else if(unit =='G')
			actualSize = size * 1024L * 1024L * 1024L;
		
		File theContainer = new File(path);
		offset = FileHeader.getHeaderSize(path);
		System.out.println(offset);
		
		Random rand = new Random();
		byte[] temp;
		
		try
		(FileOutputStream out = new FileOutputStream(theContainer, true))
		{
			FileChannel ch = out.getChannel().position(offset);
		    
			while(theContainer.length() < actualSize)
			{
				temp = rand.generateRandomByte(512);
				//fos.write(Crypt.encrypt(temp, key.getEncoded(), ca, hash));
				ch.write(ByteBuffer.wrap(temp));
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		
		
	}
	

}
