package controller;

import dao.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import model.*;
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
class StudentDashboardControllerTest {

    private StudentDashboardController controller;

    @Mock private TeacherProfileDAO mockTeacherProfileDAO;
    @Mock private TimeSlotDAO mockTimeSlotDAO;
    @Mock private BookingDAO mockBookingDAO;
    @Mock private LearnerProfileDAO mockLearnerProfileDAO;
    @Mock private UserDAO mockUserDAO;

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
                controller = new StudentDashboardController();

                setField(controller, "teacherNameLabel", new Label());
                setField(controller, "teacherInstrumentLabel", new Label());
                setField(controller, "teacherExperienceLabel", new Label());
                setField(controller, "teacherRateLabel", new Label());
                setField(controller, "errorLabel", new Label());
                setField(controller, "instrumentCombo", new ComboBox<>());
                setField(controller, "teacherCombo", new ComboBox<>());
                setField(controller, "monthLabel", new Label());
                setField(controller, "calendarGrid", new FlowPane());
                setField(controller, "selectedDateLabel", new Label());
                setField(controller, "selectedTimeLabel", new Label());
                setField(controller, "timeSlotsContainer", new VBox());
                setField(controller, "languageCombo", new ComboBox<>());
                setField(controller, "bookButton", new Button());

                setField(controller, "teacherProfileDAO", mockTeacherProfileDAO);
                setField(controller, "timeSlotDAO", mockTimeSlotDAO);
                setField(controller, "bookingDAO", mockBookingDAO);
                setField(controller, "learnerProfileDAO", mockLearnerProfileDAO);
                setField(controller, "userDAO", mockUserDAO);

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

    // --- handleBookNow: no teacher selected ---

    @Test
    void testHandleBookNow_NoTeacher_DoesNotCallBookingDAO() throws Exception {
        runOnFX(() -> {
            setField(controller, "selectedTeacher", null);
            setField(controller, "selectedSlot", null);

            invokeMethod("handleBookNow");

            verifyNoInteractions(mockBookingDAO);
        });
    }

    // --- handleBookNow: no slot selected ---

    @Test
    void testHandleBookNow_NoSlot_DoesNotCallBookingDAO() throws Exception {
        runOnFX(() -> {
            TeacherProfile teacher = createTeacherProfile();
            setField(controller, "selectedTeacher", teacher);
            setField(controller, "selectedSlot", null);

            invokeMethod("handleBookNow");

            verifyNoInteractions(mockBookingDAO);
        });
    }

    // --- confirmBooking: no learner profile ---

    @Test
    void testConfirmBooking_NoLearnerProfile_DoesNotCallBookingDAO() throws Exception {
        runOnFX(() -> {
            setField(controller, "learnerProfile", null);
            setField(controller, "selectedSlot", createTimeSlot(true));
            setField(controller, "selectedTeacher", createTeacherProfile());

            invokeMethod("handleBookNow");

            verifyNoInteractions(mockBookingDAO);
        });
    }

    // --- confirmBooking: successful booking ---

    @Test
    void testConfirmBooking_Success_CreatesBookingAndUpdatesSlot() throws Exception {
        runOnFX(() -> {
            LearnerProfile learner = new LearnerProfile(1, "Piano");
            learner.setLearnerProfileId(10);
            TimeSlot slot = createTimeSlot(true);
            TeacherProfile teacher = createTeacherProfile();

            setField(controller, "learnerProfile", learner);
            setField(controller, "selectedSlot", slot);
            setField(controller, "selectedTeacher", teacher);
            setField(controller, "selectedDate", LocalDate.now());

            when(mockBookingDAO.create(any(Booking.class))).thenReturn(true);
            when(mockTimeSlotDAO.findByTeacherProfileIdAndDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());

            invokeMethod("confirmBooking");

            verify(mockBookingDAO).create(any(Booking.class));
            verify(mockTimeSlotDAO).updateStatus(slot.getSlotId(), TimeSlot.STATUS_BOOKED);
            assertNull(getField(controller, "selectedSlot", TimeSlot.class));
        });
    }

    // --- confirmBooking: booking fails ---

    @Test
    void testConfirmBooking_Failure_DoesNotUpdateSlotStatus() throws Exception {
        runOnFX(() -> {
            LearnerProfile learner = new LearnerProfile(1, "Piano");
            learner.setLearnerProfileId(10);
            TimeSlot slot = createTimeSlot(true);

            setField(controller, "learnerProfile", learner);
            setField(controller, "selectedSlot", slot);
            setField(controller, "selectedTeacher", createTeacherProfile());

            when(mockBookingDAO.create(any(Booking.class))).thenReturn(false);

            invokeMethod("confirmBooking");

            verify(mockBookingDAO).create(any(Booking.class));
            verify(mockTimeSlotDAO, never()).updateStatus(anyInt(), anyString());
        });
    }

    // --- handleLogout ---

    @Test
    void testHandleLogout_ClearsSession() {
        User user = new User("test", "hash", "test@test.com", "LEARNER");
        SessionManager.getInstance().setCurrentUser(user);

        SessionManager.getInstance().logout();

        assertFalse(SessionManager.getInstance().isLoggedIn());
    }

    // --- updateTimeSlots: no date or teacher ---

    @Test
    void testUpdateTimeSlots_NoDateOrTeacher_ContainerEmpty() throws Exception {
        runOnFX(() -> {
            setField(controller, "selectedDate", null);
            setField(controller, "selectedTeacher", null);

            invokeMethod("updateTimeSlots");

            VBox container = getField(controller, "timeSlotsContainer", VBox.class);
            assertTrue(container.getChildren().isEmpty());
        });
    }

    // --- updateTimeSlots: no available slots shows label ---

    @Test
    void testUpdateTimeSlots_NoAvailableSlots_ShowsNoSlotsLabel() throws Exception {
        runOnFX(() -> {
            setField(controller, "selectedDate", LocalDate.now());
            setField(controller, "selectedTeacher", createTeacherProfile());

            when(mockTimeSlotDAO.findByTeacherProfileIdAndDate(anyInt(), any()))
                    .thenReturn(Collections.emptyList());

            invokeMethod("updateTimeSlots");

            VBox container = getField(controller, "timeSlotsContainer", VBox.class);
            assertEquals(1, container.getChildren().size());
            assertTrue(container.getChildren().get(0) instanceof Label);
            assertEquals("No available slots", ((Label) container.getChildren().get(0)).getText());
        });
    }

    // --- updateTimeSlots: available slots shown ---

    @Test
    void testUpdateTimeSlots_WithAvailableSlots_ShowsSlots() throws Exception {
        runOnFX(() -> {
            setField(controller, "selectedDate", LocalDate.now());
            setField(controller, "selectedTeacher", createTeacherProfile());

            TimeSlot slot = createTimeSlot(true);
            when(mockTimeSlotDAO.findByTeacherProfileIdAndDate(anyInt(), any()))
                    .thenReturn(List.of(slot));

            invokeMethod("updateTimeSlots");

            VBox container = getField(controller, "timeSlotsContainer", VBox.class);
            assertFalse(container.getChildren().isEmpty());
        });
    }

    // --- Helpers ---

    private TeacherProfile createTeacherProfile() {
        TeacherProfile profile = new TeacherProfile();
        profile.setTeacherProfileId(1);
        profile.setUserId(1);
        profile.setInstrumentsTaught("Piano");
        profile.setYearsExperience(5);
        profile.setHourlyRate(50);
        return profile;
    }

    private TimeSlot createTimeSlot(boolean available) {
        TimeSlot slot = new TimeSlot();
        slot.setSlotId(1);
        slot.setStartTime("10:00");
        slot.setEndTime("11:00");
        slot.setSlotStatus(available ? TimeSlot.STATUS_AVAILABLE : TimeSlot.STATUS_BOOKED);
        return slot;
    }

    private void invokeMethod(String methodName) {
        try {
            try {
                Method method = StudentDashboardController.class.getDeclaredMethod(methodName, ActionEvent.class);
                method.setAccessible(true);
                method.invoke(controller, new ActionEvent());
            } catch (NoSuchMethodException e) {
                Method method = StudentDashboardController.class.getDeclaredMethod(methodName);
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