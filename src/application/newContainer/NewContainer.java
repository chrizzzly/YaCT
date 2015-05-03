/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/

package application.newContainer;

import utils.Algorithms;
import utils.Filesystems;
import utils.HashAlgorithms;
import utils.Mode;


public class NewContainer 
{

	
	private int size;
	private int blocksize = 4;
	private char unit;  
	private Filesystems fs;
		
	private Algorithms algorithm;
	private HashAlgorithms hash;
	private Mode mode;
	private char[] password;
	
	private String path;
		
	
	public NewContainer()
	{	
		size = size==0 ? 50 : size;
		blocksize = blocksize==0 ? 4 : blocksize;
		
//		InitVector iv = new InitVector();
//		iv.InitVector();
	}
	
	public void setSize(int i)
	{
		this.size = i;
	}
	
	public void setBlocksize(int i)
	{
		this.blocksize = i;
	}
	
	public void setUnit(char c)
	{
		this.unit = c;
	}
	

	public Algorithms getAlgorithm() 
	{
		return algorithm;
	}

	public void setAlgorithm(Algorithms algorithm) 
	{
		this.algorithm = algorithm;
	}

	public HashAlgorithms getHash() 
	{
		return hash;
	}

	public void setHash(HashAlgorithms hash) 
	{
		this.hash = hash;
	}

	public char[] getPassword() 
	{
		return password;
	}

	public void setPassword(char[] password) 
	{
		this.password = password;
	}

	public String getPath() 
	{
		return path;
	}

	public void setPath(String path) 
	{
		this.path = path;
	}

	public Mode getMode()
	{
		return mode;
	}
	
	public void setMode(Mode value) 
	{
		this.mode = value;
	}

	public void setFileSystem(Filesystems fs) 
	{
		this.fs = fs;
	}




}
