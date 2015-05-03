package application.ui.main;


public interface ISubController<T> 
{
    void cancel();

    void doIt(T object);
    
    Class<?> getExpectedClass();
}
