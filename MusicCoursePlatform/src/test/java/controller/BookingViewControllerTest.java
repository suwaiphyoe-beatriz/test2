package controller;

import dao.*;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingViewControllerTest {

    private BookingViewController controller;

    private BookingDAO bookingDAO;
    private TimeSlotDAO timeSlotDAO;
    private TeacherProfileDAO teacherProfileDAO;
    private LearnerProfileDAO learnerProfileDAO;
    private UserDAO userDAO;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize JavaFX toolkit
        new JFXPanel();

        controller = new BookingViewController();

        bookingDAO = mock(BookingDAO.class);
        timeSlotDAO = mock(TimeSlotDAO.class);
        teacherProfileDAO = mock(TeacherProfileDAO.class);
        learnerProfileDAO = mock(LearnerProfileDAO.class);
        userDAO = mock(UserDAO.class);

        // Inject mocks
        injectPrivateField("bookingDAO", bookingDAO);
        injectPrivateField("timeSlotDAO", timeSlotDAO);
        injectPrivateField("teacherProfileDAO", teacherProfileDAO);
        injectPrivateField("learnerProfileDAO", learnerProfileDAO);
        injectPrivateField("userDAO", userDAO);

        // Inject UI components
        injectPrivateField("userNameLabel", new Label());
        injectPrivateField("bookingsContainer", new FlowPane());
        injectPrivateField("languageCombo", new ComboBox<>());
    }

    private void injectPrivateField(String fieldName, Object value) throws Exception {
        Field field = BookingViewController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    @Test
    void testSetupLanguageCombo() throws Exception {
        controller.initialize();

        ComboBox<String> combo =
                (ComboBox<String>) getPrivateField("languageCombo");

        assertEquals("EN", combo.getValue());
        assertTrue(combo.getItems().contains("DE"));
        assertTrue(combo.getItems().contains("ZH"));
    }

    @Test
    void testLoadBookings_NoLearnerProfile() throws Exception {
        injectPrivateField("learnerProfile", null);

        controller.initialize();

        FlowPane container =
                (FlowPane) getPrivateField("bookingsContainer");

        assertEquals(1, container.getChildren().size());
        assertTrue(container.getChildren().get(0) instanceof Label);
    }

    @Test
    void testLoadBookings_WithBookings() throws Exception {
        LearnerProfile learner = new LearnerProfile();
        learner.setLearnerProfileId(1);

        injectPrivateField("learnerProfile", learner);

        Booking booking = new Booking();
        booking.setBookingId(1);
        booking.setSlotId(10);
        booking.setBookingStatus(Booking.STATUS_CONFIRMED);

        when(bookingDAO.findByLearnerProfileId(1))
                .thenReturn(List.of(booking));

        TimeSlot slot = new TimeSlot();
        slot.setSlotId(10);
        slot.setTeacherProfileId(5);
        slot.setLessonDate(LocalDate.now());
        slot.setStartTime("10:00");
        slot.setEndTime("11:00");

        when(timeSlotDAO.findById(10)).thenReturn(slot);

        TeacherProfile teacher = new TeacherProfile();
        teacher.setTeacherProfileId(5);
        teacher.setUserId(2);
        teacher.setInstrumentsTaught("Piano");

        when(teacherProfileDAO.findById(5)).thenReturn(teacher);

        User teacherUser = new User();
        teacherUser.setUsername("John");

        when(userDAO.findById(2)).thenReturn(teacherUser);

        controller.initialize();

        FlowPane container =
                (FlowPane) getPrivateField("bookingsContainer");

        assertFalse(container.getChildren().isEmpty());
    }

    @Test
    void testHandleDeleteBooking() throws Exception {
        Booking booking = new Booking();
        booking.setBookingId(1);
        booking.setSlotId(10);
        booking.setBookingStatus(Booking.STATUS_CONFIRMED);

        TimeSlot slot = new TimeSlot();
        slot.setSlotId(10);

        when(bookingDAO.update(any())).thenReturn(true);

        controller.handleDeleteBooking(booking, slot);

        verify(bookingDAO).update(booking);
        verify(timeSlotDAO).updateStatus(10, TimeSlot.STATUS_AVAILABLE);

        assertEquals(Booking.STATUS_CANCELLED, booking.getBookingStatus());
    }

    private Object getPrivateField(String fieldName) throws Exception {
        Field field = BookingViewController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(controller);
    }
}