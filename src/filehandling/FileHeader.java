/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/
package filehandling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import security.Random;
import application.newContainer.NewContainer;



public class FileHeader 
{
	public enum retCodes
	{
		ERROR,
		FILEEXISTS,
		SUCCESS
	} 
	
	/**
	 * Writes the Header of a Containerfile
	 * @param force boolean which enables overwriting of an existing Fileheader
	 * @param password the password of a Containerfile
	 * @return <ul><li>ERROR will be returned, if an IOException is thrown while the header is to be written</li>
	 * <li>FILEEXISTIS is returned, if the file exists and {@code force} is set to false</li>
	 * <li>SUCCESS will be returned, if no error occurred</li></ul>
	 */
	public static retCodes writeHeader(NewContainer object, boolean force)
	{
		File outPath = new File(object.getPath()).getParentFile();
		File outFile = new File(object.getPath());
		
		if(outPath != null && !outPath.exists())
		{
			outPath.mkdirs();
		}
		
		if(outFile.exists() && !force)
		{
			return retCodes.FILEEXISTS;
		}
		
		
		//try(FileOutputStream print = new FileOutputStream(outFile))
		try(PrintWriter print = new PrintWriter(new FileOutputStream(outFile)))
		{
			//TODO Reihenfolge
			//1. Algorithmus CHECK
			//2. Hashalgorithmus CHECK
			//3. Pepper mit PW verschlüsselt EBC
			//4. Key mit gepeppertem PW verschlüsselt
			
			Random rand = new Random();
			byte[] pepper = rand.generateRandomByte(128);
			print.println(beginHeader());
			print.println(object.getAlgorithm());
			print.println(object.getHash());
			print.println(pepper);
			
			//Append Pepper to Password
			for(int i = 0; i < object.getPassword().length && i < pepper.length; i++)
			{
				pepper[i] = object.getPassword()[i];
			}
			
			print.println(pepper);
			
			
			print.println(endHeader());
			return retCodes.SUCCESS;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return retCodes.ERROR;
		}
		
		
		
	}

	public void readHeader(String path)
	{
		File inFile = new File(path);
		String line;
		
		try
			(
			InputStream is = new FileInputStream(inFile);
			InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
			BufferedReader br = new BufferedReader(isr)
			)
		{
			for(int i = 0; i <= 2; i++)
			{
				if((line = br.readLine()) == null)
					throw new IOException("No Containerfile");
				//System.out.println(line);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public static int getFirstLineAfterHeader(String path)
	{
		int retVal = 0;
		String temp = "";
		File inFile = new File(path);
		
		try
			(
			InputStream is = new FileInputStream(inFile);
			InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
			BufferedReader br = new BufferedReader(isr)
			)
		{
			for(; !temp.equals(endHeader()) ;retVal++)
			{
				temp = br.readLine();
			}
	    } 
		catch (IOException e) 
		{
		e.printStackTrace();
		}
		
		
		
		return retVal;
	}
	
	public static long getHeaderSize(String path) 
	{
		long retVal = 0;
//		String temp = "";
//		File inFile = new File(path);
//		
//		try
//			(
//			InputStream is = new FileInputStream(inFile);
//			InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
//			BufferedReader br = new BufferedReader(isr)
//			)
//		{
//			for(; !temp.equals(endHeader()) ;retVal+=temp.length())
//			{
//				temp = br.readLine();
//			}
//	    } 
//		catch (IOException e) 
//		{
//		e.printStackTrace();
//		}
		
		
		retVal = 16384;
		
		return retVal;
	}
	
	private static String beginHeader()
	{
		StringBuffer retVal = new StringBuffer();
		retVal.append("#BeginHeader#");
		return retVal.toString();
	}
	
	private static String endHeader()
	{
		StringBuffer retVal = new StringBuffer();
		retVal.append("#EndHeader#");
		return retVal.toString();
	}

	
}
