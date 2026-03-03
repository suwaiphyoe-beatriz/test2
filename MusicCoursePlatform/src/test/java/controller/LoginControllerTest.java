package controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    private LoginController controller;

    @Mock
    private service.UserService mockUserService;

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown(); // already initialized
        }
        latch.await();
    }

    @BeforeEach
    void setUp() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                controller = new LoginController();

                // Inject FXML fields via reflection
                setField(controller, "emailField", new TextField());
                setField(controller, "passwordField", new PasswordField());
                setField(controller, "errorLabel", new Label());

                // Inject mock service via reflection
                setField(controller, "userService", mockUserService);

                // Hide error label initially
                controller.initialize();
                setField(controller, "userService", mockUserService); // override after initialize
            } catch (Exception e) {
                fail("Setup failed: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }

    // --- handleLogin tests ---

    @Test
    void testHandleLogin_EmptyEmail_ShowsError() throws Exception {
        runOnFX(() -> {
            getField(controller, "emailField", TextField.class).setText("");
            getField(controller, "passwordField", PasswordField.class).setText("password");

            invokeHandleLogin(controller);

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Please fill in all fields!", errorLabel.getText());
            assertTrue(errorLabel.isVisible());
        });
    }

    @Test
    void testHandleLogin_EmptyPassword_ShowsError() throws Exception {
        runOnFX(() -> {
            getField(controller, "emailField", TextField.class).setText("test@test.com");
            getField(controller, "passwordField", PasswordField.class).setText("");

            invokeHandleLogin(controller);

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Please fill in all fields!", errorLabel.getText());
            assertTrue(errorLabel.isVisible());
        });
    }

    @Test
    void testHandleLogin_InvalidCredentials_ShowsError() throws Exception {
        when(mockUserService.authenticateByEmail("wrong@test.com", "wrongpass")).thenReturn(null);

        runOnFX(() -> {
            getField(controller, "emailField", TextField.class).setText("wrong@test.com");
            getField(controller, "passwordField", PasswordField.class).setText("wrongpass");

            invokeHandleLogin(controller);

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Invalid email or password!", errorLabel.getText());
            assertTrue(errorLabel.isVisible());
        });
    }

    @Test
    void testHandleLogin_ServiceThrowsIllegalArgument_ShowsError() throws Exception {
        when(mockUserService.authenticateByEmail(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Invalid input"));

        runOnFX(() -> {
            getField(controller, "emailField", TextField.class).setText("test@test.com");
            getField(controller, "passwordField", PasswordField.class).setText("password");

            invokeHandleLogin(controller);

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Invalid input", errorLabel.getText());
        });
    }

    @Test
    void testHandleLogin_ServiceThrowsException_ShowsError() throws Exception {
        when(mockUserService.authenticateByEmail(anyString(), anyString()))
                .thenThrow(new RuntimeException("DB error"));

        runOnFX(() -> {
            getField(controller, "emailField", TextField.class).setText("test@test.com");
            getField(controller, "passwordField", PasswordField.class).setText("password");

            invokeHandleLogin(controller);

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertTrue(errorLabel.getText().contains("Login failed"));
        });
    }

    // --- handleBack tests ---

    @Test
    void testHandleBack_ClearsFields() throws Exception {
        runOnFX(() -> {
            TextField emailField = getField(controller, "emailField", TextField.class);
            PasswordField passwordField = getField(controller, "passwordField", PasswordField.class);
            emailField.setText("test@test.com");
            passwordField.setText("password");

            invokeHandleBack(controller);

            assertEquals("", emailField.getText());
            assertEquals("", passwordField.getText());
        });
    }

    @Test
    void testHandleBack_HidesErrorLabel() throws Exception {
        runOnFX(() -> {
            Label errorLabel = getField(controller, "errorLabel", Label.class);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);

            invokeHandleBack(controller);

            assertFalse(errorLabel.isVisible());
            assertFalse(errorLabel.isManaged());
        });
    }

    // --- Helper methods ---

    private void invokeHandleLogin(LoginController controller) {
        try {
            Method method = LoginController.class.getDeclaredMethod("handleLogin", ActionEvent.class);
            method.setAccessible(true);
            method.invoke(controller, new ActionEvent());
        } catch (Exception e) {
            fail("Could not invoke handleLogin: " + e.getMessage());
        }
    }

    private void invokeHandleBack(LoginController controller) {
        try {
            Method method = LoginController.class.getDeclaredMethod("handleBack", ActionEvent.class);
            method.setAccessible(true);
            method.invoke(controller, new ActionEvent());
        } catch (Exception e) {
            fail("Could not invoke handleBack: " + e.getMessage());
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static <T> T getField(Object target, String fieldName, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return type.cast(field.get(target));
        } catch (Exception e) {
            fail("Could not get field: " + fieldName);
            return null;
        }
    }

    private void runOnFX(RunnableWithException task) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] thrown = {null};
        Platform.runLater(() -> {
            try {
                task.run();
            } catch (Exception e) {
                thrown[0] = e;
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        if (thrown[0] != null) throw thrown[0];
    }

    @FunctionalInterface
    interface RunnableWithException {
        void run() throws Exception;
    }
}