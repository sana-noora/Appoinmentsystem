package service_notify;


import domain.User;

public class EmailObserver implements Observer {

    @Override
    public void notify(User user, String message) {
        System.out.println(
            "[EMAIL] To: " + user.getEmail() +
            " | Message: " + message
        );
    }
}

