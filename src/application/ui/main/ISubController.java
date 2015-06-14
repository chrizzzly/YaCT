/*******************************************************************************
 * YaCT | (c) 2015 Christian Hoppe | http://opensource.org/licenses/MIT
 ******************************************************************************/

package application.ui.main;

import java.security.GeneralSecurityException;

import application.ui.ControllerValidationException;


public interface ISubController<T> 
{
    void cancel();

    void doIt(T object) throws ControllerValidationException, GeneralSecurityException;
    
    Class<?> getExpectedClass();
    
    boolean validate();
}
