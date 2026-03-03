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
class SignupControllerTest {

    private SignupController controller;

    @Mock
    private service.UserService mockUserService;

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        latch.await();
    }

    @BeforeEach
    void setUp() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                controller = new SignupController();
                setField(controller, "usernameField", new TextField());
                setField(controller, "emailField", new TextField());
                setField(controller, "passwordField", new PasswordField());
                setField(controller, "errorLabel", new Label());
                setField(controller, "userService", mockUserService);
            } catch (Exception e) {
                fail("Setup failed: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }

    // --- Empty field validation ---

    @Test
    void testStudentSignup_EmptyUsername_ShowsError() throws Exception {
        runOnFX(() -> {
            getField(controller, "usernameField", TextField.class).setText("");
            getField(controller, "emailField", TextField.class).setText("test@test.com");
            getField(controller, "passwordField", PasswordField.class).setText("password123");

            invokeMethod("handleStudentSignup");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Please fill in all fields!", errorLabel.getText());
            assertTrue(errorLabel.isVisible());
        });
    }

    @Test
    void testStudentSignup_EmptyEmail_ShowsError() throws Exception {
        runOnFX(() -> {
            getField(controller, "usernameField", TextField.class).setText("testuser");
            getField(controller, "emailField", TextField.class).setText("");
            getField(controller, "passwordField", PasswordField.class).setText("password123");

            invokeMethod("handleStudentSignup");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Please fill in all fields!", errorLabel.getText());
        });
    }

    @Test
    void testStudentSignup_EmptyPassword_ShowsError() throws Exception {
        runOnFX(() -> {
            getField(controller, "usernameField", TextField.class).setText("testuser");
            getField(controller, "emailField", TextField.class).setText("test@test.com");
            getField(controller, "passwordField", PasswordField.class).setText("");

            invokeMethod("handleStudentSignup");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Please fill in all fields!", errorLabel.getText());
        });
    }

    // --- Email validation ---

    @Test
    void testStudentSignup_InvalidEmail_ShowsError() throws Exception {
        runOnFX(() -> {
            getField(controller, "usernameField", TextField.class).setText("testuser");
            getField(controller, "emailField", TextField.class).setText("notanemail");
            getField(controller, "passwordField", PasswordField.class).setText("password123");

            invokeMethod("handleStudentSignup");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Please enter a valid email address!", errorLabel.getText());
        });
    }

    // --- Password length validation ---

    @Test
    void testStudentSignup_ShortPassword_ShowsError() throws Exception {
        runOnFX(() -> {
            getField(controller, "usernameField", TextField.class).setText("testuser");
            getField(controller, "emailField", TextField.class).setText("test@test.com");
            getField(controller, "passwordField", PasswordField.class).setText("123");

            invokeMethod("handleStudentSignup");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Password must be at least 6 characters!", errorLabel.getText());
        });
    }

    // --- Successful registration ---

    @Test
    void testStudentSignup_ValidInput_CallsRegisterWithLearner() throws Exception {
        User mockUser = new User("testuser", "hash", "test@test.com", "LEARNER");
        when(mockUserService.registerUser("testuser", "password123", "test@test.com", "LEARNER"))
                .thenReturn(mockUser);

        runOnFX(() -> {
            getField(controller, "usernameField", TextField.class).setText("testuser");
            getField(controller, "emailField", TextField.class).setText("test@test.com");
            getField(controller, "passwordField", PasswordField.class).setText("password123");

            invokeMethod("handleStudentSignup");

            verify(mockUserService).registerUser("testuser", "password123", "test@test.com", "LEARNER");
        });
    }

    @Test
    void testTeacherSignup_ValidInput_CallsRegisterWithTeacher() throws Exception {
        User mockUser = new User("teacheruser", "hash", "teacher@test.com", "TEACHER");
        when(mockUserService.registerUser("teacheruser", "password123", "teacher@test.com", "TEACHER"))
                .thenReturn(mockUser);

        runOnFX(() -> {
            getField(controller, "usernameField", TextField.class).setText("teacheruser");
            getField(controller, "emailField", TextField.class).setText("teacher@test.com");
            getField(controller, "passwordField", PasswordField.class).setText("password123");

            invokeMethod("handleTeacherSignup");

            verify(mockUserService).registerUser("teacheruser", "password123", "teacher@test.com", "TEACHER");
        });
    }

    // --- Exception handling ---

    @Test
    void testStudentSignup_IllegalArgumentException_ShowsError() throws Exception {
        when(mockUserService.registerUser(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Username already taken"));

        runOnFX(() -> {
            getField(controller, "usernameField", TextField.class).setText("testuser");
            getField(controller, "emailField", TextField.class).setText("test@test.com");
            getField(controller, "passwordField", PasswordField.class).setText("password123");

            invokeMethod("handleStudentSignup");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Username already taken", errorLabel.getText());
        });
    }

    @Test
    void testStudentSignup_GenericException_ShowsError() throws Exception {
        when(mockUserService.registerUser(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("DB error"));

        runOnFX(() -> {
            getField(controller, "usernameField", TextField.class).setText("testuser");
            getField(controller, "emailField", TextField.class).setText("test@test.com");
            getField(controller, "passwordField", PasswordField.class).setText("password123");

            invokeMethod("handleStudentSignup");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertTrue(errorLabel.getText().contains("Registration failed"));
        });
    }

    // --- Helpers ---

    private void invokeMethod(String methodName) {
        try {
            Method method = SignupController.class.getDeclaredMethod(methodName, ActionEvent.class);
            method.setAccessible(true);
            method.invoke(controller, new ActionEvent());
        } catch (Exception e) {
            fail("Could not invoke " + methodName + ": " + e.getMessage());
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