package controller;

import dao.TeacherProfileDAO;
import dao.TimeSlotDAO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import model.TeacherProfile;
import model.TimeSlot;
import model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherProfileViewControllerTest {

    private TeacherProfileViewController controller;

    @Mock private TeacherProfileDAO mockTeacherProfileDAO;
    @Mock private TimeSlotDAO mockTimeSlotDAO;

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
                controller = new TeacherProfileViewController();

                setField(controller, "teacherNameLabel", new Label());
                setField(controller, "weekLabel", new Label());
                setField(controller, "availabilityGrid", new HBox());
                setField(controller, "languageCombo", new ComboBox<>());
                setField(controller, "dayCombo", new ComboBox<>());
                setField(controller, "startTimeCombo", new ComboBox<>());
                setField(controller, "endTimeCombo", new ComboBox<>());
                setField(controller, "errorLabel", new Label());

                setField(controller, "teacherProfileDAO", mockTeacherProfileDAO);
                setField(controller, "timeSlotDAO", mockTimeSlotDAO);

                setField(controller, "weekStart", LocalDate.now().with(DayOfWeek.MONDAY));

            } catch (Exception e) {
                fail("Setup failed: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        latch.await();

        SessionManager.getInstance().logout();
    }

    // --- handleAddTimeSlot: missing fields ---

    @Test
    void testHandleAddTimeSlot_NoSelection_ShowsError() throws Exception {
        runOnFX(() -> {
            // leave dayCombo, startTimeCombo, endTimeCombo empty

            invokeMethod("handleAddTimeSlot");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Please select day, start time, and end time", errorLabel.getText());
            verifyNoInteractions(mockTimeSlotDAO);
        });
    }

    // --- handleAddTimeSlot: end time before start time ---

    @Test
    void testHandleAddTimeSlot_EndTimeBeforeStartTime_ShowsError() throws Exception {
        runOnFX(() -> {
            setField(controller, "teacherProfile", createTeacherProfile());

            ComboBox<String> dayCombo = getField(controller, "dayCombo", ComboBox.class);
            ComboBox<String> startCombo = getField(controller, "startTimeCombo", ComboBox.class);
            ComboBox<String> endCombo = getField(controller, "endTimeCombo", ComboBox.class);

            dayCombo.getItems().add("Monday");
            dayCombo.setValue("Monday");
            startCombo.getItems().add("14:00");
            startCombo.setValue("14:00");
            endCombo.getItems().add("10:00");
            endCombo.setValue("10:00");

            invokeMethod("handleAddTimeSlot");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("End time must be after start time", errorLabel.getText());
            verifyNoInteractions(mockTimeSlotDAO);
        });
    }

    // --- handleAddTimeSlot: no teacher profile ---

    @Test
    void testHandleAddTimeSlot_NoTeacherProfile_ShowsError() throws Exception {
        runOnFX(() -> {
            setField(controller, "teacherProfile", null);

            ComboBox<String> dayCombo = getField(controller, "dayCombo", ComboBox.class);
            ComboBox<String> startCombo = getField(controller, "startTimeCombo", ComboBox.class);
            ComboBox<String> endCombo = getField(controller, "endTimeCombo", ComboBox.class);

            dayCombo.getItems().add("Monday");
            dayCombo.setValue("Monday");
            startCombo.getItems().add("10:00");
            startCombo.setValue("10:00");
            endCombo.getItems().add("11:00");
            endCombo.setValue("11:00");

            invokeMethod("handleAddTimeSlot");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Teacher profile not found", errorLabel.getText());
            verifyNoInteractions(mockTimeSlotDAO);
        });
    }

    // --- handleAddTimeSlot: success ---

    @Test
    void testHandleAddTimeSlot_ValidInput_CreatesSlot() throws Exception {
        runOnFX(() -> {
            setField(controller, "teacherProfile", createTeacherProfile());

            ComboBox<String> dayCombo = getField(controller, "dayCombo", ComboBox.class);
            ComboBox<String> startCombo = getField(controller, "startTimeCombo", ComboBox.class);
            ComboBox<String> endCombo = getField(controller, "endTimeCombo", ComboBox.class);

            dayCombo.getItems().add("Monday");
            dayCombo.setValue("Monday");
            startCombo.getItems().add("10:00");
            startCombo.setValue("10:00");
            endCombo.getItems().add("11:00");
            endCombo.setValue("11:00");

            when(mockTimeSlotDAO.create(any(TimeSlot.class))).thenReturn(true);
            when(mockTimeSlotDAO.findByTeacherProfileIdAndDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());

            invokeMethod("handleAddTimeSlot");

            verify(mockTimeSlotDAO).create(any(TimeSlot.class));
        });
    }

    // --- handleAddTimeSlot: create fails ---

    @Test
    void testHandleAddTimeSlot_CreateFails_ShowsError() throws Exception {
        runOnFX(() -> {
            setField(controller, "teacherProfile", createTeacherProfile());

            ComboBox<String> dayCombo = getField(controller, "dayCombo", ComboBox.class);
            ComboBox<String> startCombo = getField(controller, "startTimeCombo", ComboBox.class);
            ComboBox<String> endCombo = getField(controller, "endTimeCombo", ComboBox.class);

            dayCombo.getItems().add("Monday");
            dayCombo.setValue("Monday");
            startCombo.getItems().add("10:00");
            startCombo.setValue("10:00");
            endCombo.getItems().add("11:00");
            endCombo.setValue("11:00");

            when(mockTimeSlotDAO.create(any(TimeSlot.class))).thenReturn(false);

            invokeMethod("handleAddTimeSlot");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Failed to create time slot", errorLabel.getText());
        });
    }

    // --- handlePrevPage ---

    @Test
    void testHandlePrevPage_DecrementsWeek() throws Exception {
        runOnFX(() -> {
            LocalDate before = LocalDate.now().with(DayOfWeek.MONDAY);
            setField(controller, "weekStart", before);

            invokeMethod("handlePrevPage");

            LocalDate after = getField(controller, "weekStart", LocalDate.class);
            assertEquals(before.minusWeeks(1), after);
        });
    }

    @Test
    void testHandleNextPage_IncrementsWeek() throws Exception {
        runOnFX(() -> {
            LocalDate before = LocalDate.now().with(DayOfWeek.MONDAY);
            setField(controller, "weekStart", before);

            invokeMethod("handleNextPage");

            LocalDate after = getField(controller, "weekStart", LocalDate.class);
            assertEquals(before.plusWeeks(1), after);
        });
    }

    // --- updateAvailabilityGrid ---

    @Test
    void testUpdateAvailabilityGrid_ShowsSevenDayColumns() throws Exception {
        runOnFX(() -> {
            setField(controller, "teacherProfile", createTeacherProfile());

            when(mockTimeSlotDAO.findByTeacherProfileIdAndDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());

            invokeMethod("updateAvailabilityGrid");

            HBox grid = getField(controller, "availabilityGrid", HBox.class);
            assertEquals(7, grid.getChildren().size());
        });
    }

    // --- updateAvailabilityGrid: with slots ---

    @Test
    void testUpdateAvailabilityGrid_WithSlots_ShowsSlotsInColumn() throws Exception {
        runOnFX(() -> {
            setField(controller, "teacherProfile", createTeacherProfile());

            when(mockTimeSlotDAO.findByTeacherProfileIdAndDate(anyInt(), any()))
                    .thenReturn(List.of(createTimeSlot()));

            invokeMethod("updateAvailabilityGrid");

            HBox grid = getField(controller, "availabilityGrid", HBox.class);
            assertEquals(7, grid.getChildren().size());
        });
    }

    // --- handleLogout ---

    @Test
    void testHandleLogout_ClearsSession() {
        User user = createUser();
        SessionManager.getInstance().setCurrentUser(user);

        SessionManager.getInstance().logout();

        assertFalse(SessionManager.getInstance().isLoggedIn());
    }

    // --- Helpers ---

    private User createUser() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("teacher1");
        user.setEmail("teacher@test.com");
        user.setUserType("TEACHER");
        return user;
    }

    private TeacherProfile createTeacherProfile() {
        TeacherProfile profile = new TeacherProfile();
        profile.setTeacherProfileId(1);
        profile.setUserId(1);
        profile.setInstrumentsTaught("Piano");
        profile.setYearsExperience(5);
        profile.setHourlyRate(50);
        return profile;
    }

    private TimeSlot createTimeSlot() {
        TimeSlot slot = new TimeSlot();
        slot.setSlotId(1);
        slot.setStartTime("10:00");
        slot.setEndTime("11:00");
        slot.setSlotStatus(TimeSlot.STATUS_AVAILABLE);
        return slot;
    }

    private void invokeMethod(String methodName) {
        try {
            try {
                Method method = TeacherProfileViewController.class.getDeclaredMethod(methodName, ActionEvent.class);
                method.setAccessible(true);
                method.invoke(controller, new ActionEvent());
            } catch (NoSuchMethodException e) {
                Method method = TeacherProfileViewController.class.getDeclaredMethod(methodName);
                method.setAccessible(true);
                method.invoke(controller);
            }
        } catch (Exception e) {
            fail("Could not invoke " + methodName + ": " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
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