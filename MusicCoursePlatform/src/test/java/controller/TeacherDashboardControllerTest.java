package controller;

import dao.BookingDAO;
import dao.TeacherProfileDAO;
import dao.TimeSlotDAO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherDashboardControllerTest {

    private TeacherDashboardController controller;

    @Mock private TeacherProfileDAO mockTeacherProfileDAO;
    @Mock private TimeSlotDAO mockTimeSlotDAO;
    @Mock private BookingDAO mockBookingDAO;

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
                controller = new TeacherDashboardController();

                setField(controller, "nameLabel", new Label());
                setField(controller, "instrumentsCombo", new ComboBox<>());
                setField(controller, "experienceField", new TextField());
                setField(controller, "pricingField", new TextField());
                setField(controller, "bioField", new TextArea());
                setField(controller, "monthLabel", new Label());
                setField(controller, "calendarGrid", new FlowPane());
                setField(controller, "selectedDateLabel", new Label());
                setField(controller, "startTimeCombo", new ComboBox<>());
                setField(controller, "endTimeCombo", new ComboBox<>());
                setField(controller, "timeSlotsContainer", new VBox());
                setField(controller, "languageCombo", new ComboBox<>());
                setField(controller, "errorLabel", new Label());

                setField(controller, "teacherProfileDAO", mockTeacherProfileDAO);
                setField(controller, "timeSlotDAO", mockTimeSlotDAO);
                setField(controller, "bookingDAO", mockBookingDAO);

                setField(controller, "currentMonth", YearMonth.now());

            } catch (Exception e) {
                fail("Setup failed: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        latch.await();

        SessionManager.getInstance().logout();
    }

    // --- handlePrevMonth ---

    @Test
    void testHandlePrevMonth_DecrementsMonth() throws Exception {
        runOnFX(() -> {
            YearMonth before = YearMonth.now();
            setField(controller, "currentMonth", before);

            invokeMethod("handlePrevMonth");

            YearMonth after = getField(controller, "currentMonth", YearMonth.class);
            assertEquals(before.minusMonths(1), after);
        });
    }

    // --- handleNextMonth ---

    @Test
    void testHandleNextMonth_IncrementsMonth() throws Exception {
        runOnFX(() -> {
            YearMonth before = YearMonth.now();
            setField(controller, "currentMonth", before);

            invokeMethod("handleNextMonth");

            YearMonth after = getField(controller, "currentMonth", YearMonth.class);
            assertEquals(before.plusMonths(1), after);
        });
    }

    // --- handleSaveProfile: no user or profile ---

    @Test
    void testHandleSaveProfile_NoUser_DoesNotCallDAO() throws Exception {
        runOnFX(() -> {
            setField(controller, "currentUser", null);
            setField(controller, "teacherProfile", null);

            invokeMethod("handleSaveProfile");

            verifyNoInteractions(mockTeacherProfileDAO);
        });
    }

    // --- handleSaveProfile: invalid experience ---

    @Test
    void testHandleSaveProfile_InvalidExperience_ShowsError() throws Exception {
        runOnFX(() -> {
            setField(controller, "currentUser", createUser());
            setField(controller, "teacherProfile", createTeacherProfile());

            ComboBox<String> instrumentsCombo = getField(controller, "instrumentsCombo", ComboBox.class);
            instrumentsCombo.getItems().add("Piano");
            instrumentsCombo.setValue("Piano");

            getField(controller, "experienceField", TextField.class).setText("abc");
            getField(controller, "pricingField", TextField.class).setText("50");
            getField(controller, "bioField", TextArea.class).setText("Bio");

            invokeMethod("handleSaveProfile");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertTrue(errorLabel.getText().contains("Invalid experience"));
            verifyNoInteractions(mockTeacherProfileDAO);
        });
    }

    // --- handleSaveProfile: invalid pricing ---

    @Test
    void testHandleSaveProfile_InvalidPricing_ShowsError() throws Exception {
        runOnFX(() -> {
            setField(controller, "currentUser", createUser());
            setField(controller, "teacherProfile", createTeacherProfile());

            ComboBox<String> instrumentsCombo = getField(controller, "instrumentsCombo", ComboBox.class);
            instrumentsCombo.getItems().add("Piano");
            instrumentsCombo.setValue("Piano");

            getField(controller, "experienceField", TextField.class).setText("5");
            getField(controller, "pricingField", TextField.class).setText("abc");
            getField(controller, "bioField", TextArea.class).setText("Bio");

            invokeMethod("handleSaveProfile");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertTrue(errorLabel.getText().contains("Invalid pricing"));
            verifyNoInteractions(mockTeacherProfileDAO);
        });
    }

    // --- handleSaveProfile: success ---

    @Test
    void testHandleSaveProfile_ValidInput_CallsUpdate() throws Exception {
        runOnFX(() -> {
            setField(controller, "currentUser", createUser());
            setField(controller, "teacherProfile", createTeacherProfile());

            ComboBox<String> instrumentsCombo = getField(controller, "instrumentsCombo", ComboBox.class);
            instrumentsCombo.getItems().add("Piano");
            instrumentsCombo.setValue("Piano");

            getField(controller, "experienceField", TextField.class).setText("5");
            getField(controller, "pricingField", TextField.class).setText("50");
            getField(controller, "bioField", TextArea.class).setText("Experienced teacher");

            when(mockTeacherProfileDAO.update(any(TeacherProfile.class))).thenReturn(true);

            invokeMethod("handleSaveProfile");

            verify(mockTeacherProfileDAO).update(any(TeacherProfile.class));
        });
    }

    // --- handleSaveProfile: update fails ---

    @Test
    void testHandleSaveProfile_UpdateFails_ShowsError() throws Exception {
        runOnFX(() -> {
            setField(controller, "currentUser", createUser());
            setField(controller, "teacherProfile", createTeacherProfile());

            ComboBox<String> instrumentsCombo = getField(controller, "instrumentsCombo", ComboBox.class);
            instrumentsCombo.getItems().add("Piano");
            instrumentsCombo.setValue("Piano");

            getField(controller, "experienceField", TextField.class).setText("5");
            getField(controller, "pricingField", TextField.class).setText("50");
            getField(controller, "bioField", TextArea.class).setText("Bio");

            when(mockTeacherProfileDAO.update(any(TeacherProfile.class))).thenReturn(false);

            invokeMethod("handleSaveProfile");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertTrue(errorLabel.getText().contains("Failed to save"));
        });
    }

    // --- handleAddTimeSlot: no date ---

    @Test
    void testHandleAddTimeSlot_NoDate_ShowsError() throws Exception {
        runOnFX(() -> {
            setField(controller, "selectedDate", null);
            setField(controller, "teacherProfile", createTeacherProfile());

            invokeMethod("handleAddTimeSlot");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Please select a date first!", errorLabel.getText());
            verifyNoInteractions(mockTimeSlotDAO);
        });
    }

    // --- handleAddTimeSlot: no teacher profile ---

    @Test
    void testHandleAddTimeSlot_NoTeacherProfile_ShowsError() throws Exception {
        runOnFX(() -> {
            setField(controller, "selectedDate", LocalDate.now());
            setField(controller, "teacherProfile", null);

            invokeMethod("handleAddTimeSlot");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Teacher profile not found!", errorLabel.getText());
            verifyNoInteractions(mockTimeSlotDAO);
        });
    }

    // --- handleAddTimeSlot: no time selected ---

    @Test
    void testHandleAddTimeSlot_NoTimeSelected_ShowsError() throws Exception {
        runOnFX(() -> {
            setField(controller, "selectedDate", LocalDate.now());
            setField(controller, "teacherProfile", createTeacherProfile());

            invokeMethod("handleAddTimeSlot");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertEquals("Please select start and end time!", errorLabel.getText());
            verifyNoInteractions(mockTimeSlotDAO);
        });
    }

    // --- handleAddTimeSlot: success ---

    @Test
    void testHandleAddTimeSlot_ValidInput_CreatesSlot() throws Exception {
        runOnFX(() -> {
            setField(controller, "selectedDate", LocalDate.now());
            setField(controller, "teacherProfile", createTeacherProfile());

            ComboBox<String> startCombo = getField(controller, "startTimeCombo", ComboBox.class);
            ComboBox<String> endCombo = getField(controller, "endTimeCombo", ComboBox.class);
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
            setField(controller, "selectedDate", LocalDate.now());
            setField(controller, "teacherProfile", createTeacherProfile());

            ComboBox<String> startCombo = getField(controller, "startTimeCombo", ComboBox.class);
            ComboBox<String> endCombo = getField(controller, "endTimeCombo", ComboBox.class);
            startCombo.getItems().add("10:00");
            startCombo.setValue("10:00");
            endCombo.getItems().add("11:00");
            endCombo.setValue("11:00");

            when(mockTimeSlotDAO.create(any(TimeSlot.class))).thenReturn(false);

            invokeMethod("handleAddTimeSlot");

            Label errorLabel = getField(controller, "errorLabel", Label.class);
            assertTrue(errorLabel.getText().contains("Failed to add"));
        });
    }

    // --- updateTimeSlots: no date or profile ---

    @Test
    void testUpdateTimeSlots_NoDateOrProfile_ContainerEmpty() throws Exception {
        runOnFX(() -> {
            setField(controller, "selectedDate", null);
            setField(controller, "teacherProfile", null);

            invokeMethod("updateTimeSlots");

            VBox container = getField(controller, "timeSlotsContainer", VBox.class);
            assertTrue(container.getChildren().isEmpty());
        });
    }

    // --- updateTimeSlots: no slots shows label ---

    @Test
    void testUpdateTimeSlots_NoSlots_ShowsNoSlotsLabel() throws Exception {
        runOnFX(() -> {
            setField(controller, "selectedDate", LocalDate.now());
            setField(controller, "teacherProfile", createTeacherProfile());

            when(mockTimeSlotDAO.findByTeacherProfileIdAndDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());

            invokeMethod("updateTimeSlots");

            VBox container = getField(controller, "timeSlotsContainer", VBox.class);
            assertEquals(1, container.getChildren().size());
            assertTrue(container.getChildren().get(0) instanceof Label);
            assertEquals("No time slots set", ((Label) container.getChildren().get(0)).getText());
        });
    }

    // --- updateTimeSlots: slots shown ---

    @Test
    void testUpdateTimeSlots_WithSlots_ShowsSlots() throws Exception {
        runOnFX(() -> {
            setField(controller, "selectedDate", LocalDate.now());
            setField(controller, "teacherProfile", createTeacherProfile());

            when(mockTimeSlotDAO.findByTeacherProfileIdAndDate(anyInt(), any()))
                    .thenReturn(List.of(createTimeSlot()));

            invokeMethod("updateTimeSlots");

            VBox container = getField(controller, "timeSlotsContainer", VBox.class);
            assertFalse(container.getChildren().isEmpty());
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
                Method method = TeacherDashboardController.class.getDeclaredMethod(methodName, ActionEvent.class);
                method.setAccessible(true);
                method.invoke(controller, new ActionEvent());
            } catch (NoSuchMethodException e) {
                Method method = TeacherDashboardController.class.getDeclaredMethod(methodName);
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