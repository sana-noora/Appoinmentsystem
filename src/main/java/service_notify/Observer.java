package service_notify;


import domain.User;

public interface Observer {
    void notify(User user, String message);
}
