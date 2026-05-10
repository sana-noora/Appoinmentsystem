package tests;


import domain.User;
import org.junit.jupiter.api.Test;
import service_notify.Notifier;
import service_notify.Observer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class NotifierTest {

    @Test
    void notifyAll_callsObserverOnce() {
        User user = mock(User.class);
        Observer observer = mock(Observer.class);

        Notifier notifier = new Notifier();
        notifier.addObserver(observer);

        notifier.notifyAll(user, "Hello");

        verify(observer, times(1))
                .notify(user, "Hello");
    }

    @Test
    void notifyAll_callsAllObservers() {
        User user = mock(User.class);
        Observer observer1 = mock(Observer.class);
        Observer observer2 = mock(Observer.class);

        Notifier notifier = new Notifier();
        notifier.addObserver(observer1);
        notifier.addObserver(observer2);

        notifier.notifyAll(user, "Reminder");

        verify(observer1, times(1))
                .notify(user, "Reminder");
        verify(observer2, times(1))
                .notify(user, "Reminder");
    }

    @Test
    void notifyAll_withNoObservers_doesNothing() {
        User user = mock(User.class);

        Notifier notifier = new Notifier();

        assertDoesNotThrow(() ->
                notifier.notifyAll(user, "No observers")
        );
    }
}

