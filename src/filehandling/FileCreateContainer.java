package filehandling;

import java.util.HashMap;
import java.util.Map;


public class FileCreateContainer 
{
	String unit;
	int size;
	int blocksize;
	static Map<String,String> env = new HashMap<>();
	
	/**
	 * Creates the Environment for the formatting of the new virtual drive
	 * @param unit is the unit after the size "M" or "G"
	 * @param size is the size of the new drive 
	 * @param blocksize is the blocksize o f the new drive 
	 */
	public static void setEnvVars(String unit, int size, int blocksize)
	{
		unit = unit.equals(null) ? "M" : unit;  
		size = size==0 ? 50 : size;
		blocksize = blocksize==0 ? 4 : blocksize;
		
		env.put("capacity", size + unit);
		env.put("blockSize", blocksize + "k");
	}
	
	public static HashMap getEnvVars()
	{
		return (HashMap) env;
	}
}
