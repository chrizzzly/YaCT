/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/

package security;

/**
 * @author Christian Hoppe
 *
 */
public class Hash 
{
	public enum HashCodes
	{
		WHIRLPOOL,
		RIPEMD,
		SHA512,
		SHA256
	} 
	
	public byte[] generateHash()
	{
		byte[] retVal = null;
		int algorithm = getHashalgorithm(/*UI-ELEMENT*/);
		
		
		
		
		
		return retVal;
	}
	
	public int getHashalgorithm(/*UI-ELEMENT*/)
	{
		int retVal;
		String hashAlgorithm = null;
		
		switch(hashAlgorithm)
		{
			case "whirlpool": retVal = 1;
				break;
			case "ripemd": retVal = 2;
				break;
			default: retVal = 0;
		}
		
		return retVal;
	}
}
