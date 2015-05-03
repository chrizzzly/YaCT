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
import java.util.Base64.Encoder;
import java.util.Base64.Decoder;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Base64;

import security.Random;
import utils.SysProps;



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
	public static retCodes writeHeader(boolean force, byte[] password, byte[] ca, byte[] ha) //CA und HA!!
	{
		File outPath = new File(SysProps.getUserhome());
		File outFile = new File(outPath, "test.txt");
		
		if(!outPath.exists())
		{
			outPath.mkdirs();
		}
		
		if(outFile.exists() && !force)
		{
			return retCodes.FILEEXISTS;
		}
		
		//try(PrintWriter print = new PrintWriter(new FileOutputStream(outFile)))
		try(FileOutputStream print = new FileOutputStream(outFile))
		{
			//TODO Betriebssystemabhängiges schreiben der neuen zeile!
			//TODO Reihenfolge
			//1. Algorithmus
			//2. Hashalgorithmus
			//3. Pepper mit PW verschlüsselt EBC
			//4. Key mit gepeppertem PW verschlüsselt
			
			Random rand = new Random();
			byte[] pepper = rand.generateRandomByte(0);
			
			//print.println(Base64.encode(ca.getBytes("UTF-8")));
			print.write(Base64.encode(ca));
			print.flush();
			print.write("\n".getBytes("UTF-8"));
//			print.write(SysProps.getLinebreak());
			print.flush();
			
			//print.println(Base64.encode(pepper));
			print.write(Base64.encode(pepper));
			print.write("\n".getBytes("UTF-8"));
			print.flush();
			
			//Append Pepper to Password
			byte[] peppered = Arrays.concatenate(password, pepper);
			
			//TODO Hash the peppered password

			//print.println(Base64.encode(peppered));
			print.write(Base64.encode(peppered));
			print.write("\n".getBytes("UTF-8"));
			print.flush();
			
			return retCodes.SUCCESS;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return retCodes.ERROR;
		}
		
		
		
	}

	public static void readHeader()
	{
		File inFile = new File(SysProps.getUserhome(), "test.txt");
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
					throw new IOException("No Containerfile Error");
				//System.out.println(line);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

}
