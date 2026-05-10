package tests;


import domain.User;
import org.junit.jupiter.api.Test;
import service_notify.EmailObserver;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailObserverTest {

    @Test
    void notify_doesNotThrowException() {
        User user = mock(User.class);
        when(user.getEmail()).thenReturn("test@email.com");

        EmailObserver observer = new EmailObserver();

        assertDoesNotThrow(() ->
                observer.notify(user, "Test message")
        );
    }
}
