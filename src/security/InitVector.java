/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/

package security;

import java.awt.MouseInfo;

public class InitVector 
{
	public String InitVector()
	{
		return doTheMath();
	}
	
	private boolean nextIsClicked = false;
	
	private String doTheMath()
	{
		String ret = null;
		long x = 0;
		long y = 0;
		
		Random rand = new Random();
		Long z = (long) rand.generateRandomInt();
		y = (long) MouseInfo.getPointerInfo().getLocation().getY();
		x = (long) MouseInfo.getPointerInfo().getLocation().getX();
		
		do
		{
			for(int i=0; i<10; i++)
			{
				z = x * y;
				if(z%2 == 0)
				{
					z -= System.currentTimeMillis();
					i++;
					
				}
				else
				{
					z *= System.currentTimeMillis();
				}
			}
			y = (long) MouseInfo.getPointerInfo().getLocation().getY();
			x = (long) MouseInfo.getPointerInfo().getLocation().getX();
		}
		while(!nextIsClicked);
		ret = z.toString();
		System.out.println(z);
		return ret;
	}

	public boolean isNextClicked() 
	{
		return nextIsClicked;
	}

	public void setNextIsClicked(boolean nextIsClicked) 
	{
		this.nextIsClicked = nextIsClicked;
	}
}
