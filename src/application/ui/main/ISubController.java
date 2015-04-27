package application.ui.main;


public interface ISubController<T> 
{
    void cancel();

    T doIt();
}
