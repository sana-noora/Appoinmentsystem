package service_notify;

import domain.User;
import java.util.ArrayList;
import java.util.List;

public class Notifier {

    private final List<Observer> observers = new ArrayList<>();

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void notifyAll(User user, String message) {
        for (Observer observer : observers) {
            observer.notify(user, message);
        }
    }
}
