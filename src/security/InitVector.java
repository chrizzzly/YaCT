package security;

import java.awt.MouseInfo;

public class InitVector 
{
	public void InitVector()
	{
		doTheMath();
	}
	
	private boolean nextIsClicked = false;
	
	private long doTheMath()
	{
		long retVal = 0;
		long x = 0;
		long y = 0;
		
		Random rand = new Random();
		long z = rand.generateRandomInt();
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
			System.out.println(z);
		}
		while(!nextIsClicked);
		
		return retVal;
	}

	public boolean isNextIsClicked() 
	{
		return nextIsClicked;
	}

	public void setNextIsClicked(boolean nextIsClicked) 
	{
		this.nextIsClicked = nextIsClicked;
	}
}
