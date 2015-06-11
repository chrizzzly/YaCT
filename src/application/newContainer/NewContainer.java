/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/

package application.newContainer;

import java.util.Arrays;

import utils.Algorithms;
import utils.Filesystems;
import utils.HashAlgorithms;
import utils.Mode;


public class NewContainer 
{

	
	private int size;
	private char unit;  
		
	private Algorithms algorithm;
	private HashAlgorithms hash;
	private Mode mode;
	private byte[] password;
	
	private String path;
		
	
	public NewContainer()
	{	
		size = size==0 ? 50 : size;

		
//		InitVector iv = new InitVector();
//		iv.InitVector();
	}
	
	public void setSize(int i)
	{
		this.size = i;
	}
	
	public int getSize() 
	{
		return size;
	}
	
	public void setUnit(char c)
	{
		this.unit = c;
	}
	
	public char getUnit()
	{
		return unit;
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

	public byte[] getPassword() 
	{
		return password;
	}

	public void setPassword(byte[] password) 
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


	@Override
	public String toString()
	{
		return Arrays.toString(password);
	}

	


}
