/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/

package application.ui.main;

import javafx.scene.Parent;

public class SubWindow 
{
	private Parent parent;
	private ISubController controller;
	
	public Parent getParent() 
	{
		return parent;
	}
	public void setParent(Parent parent) 
	{
		this.parent = parent;
	}
	public ISubController getController() 
	{
		return controller;
	}
	public void setController(ISubController controller) 
	{
		this.controller = controller;
	}
}
