package controller;

import dao.BookingDAO;
import dao.LearnerProfileDAO;
import dao.TeacherProfileDAO;
import dao.TimeSlotDAO;
import dao.UserDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Booking;
import model.LearnerProfile;
import model.TeacherProfile;
import model.TimeSlot;
import model.User;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BookingViewController {

    @FXML private Label userNameLabel;
    @FXML private FlowPane bookingsContainer;
    @FXML private ComboBox<String> languageCombo;

    private BookingDAO bookingDAO;
    private TimeSlotDAO timeSlotDAO;
    private TeacherProfileDAO teacherProfileDAO;
    private LearnerProfileDAO learnerProfileDAO;
    private UserDAO userDAO;

    private LearnerProfile learnerProfile;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d");

    @FXML
    public void initialize() {
        bookingDAO = new BookingDAO();
        timeSlotDAO = new TimeSlotDAO();
        teacherProfileDAO = new TeacherProfileDAO();
        learnerProfileDAO = new LearnerProfileDAO();
        userDAO = new UserDAO();

        setupLanguageCombo();
        loadUserInfo();
        loadBookings();
    }

    private void setupLanguageCombo() {
        if (languageCombo != null) {
            languageCombo.getItems().addAll("EN", "DE", "ZH");
            languageCombo.setValue("EN");
        }
    }

    private void loadUserInfo() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getUsername());
            learnerProfile = learnerProfileDAO.findByUserId(currentUser.getUserId());
        }
    }

    private void loadBookings() {
        bookingsContainer.getChildren().clear();

        if (learnerProfile == null) {
            Label noBookings = new Label("No bookings found");
            noBookings.setStyle("-fx-text-fill: #718096;");
            bookingsContainer.getChildren().add(noBookings);
            return;
        }

        List<Booking> bookings = bookingDAO.findByLearnerProfileId(learnerProfile.getLearnerProfileId());

        if (bookings.isEmpty()) {
            Label noBookings = new Label("No bookings found");
            noBookings.setStyle("-fx-text-fill: #718096;");
            bookingsContainer.getChildren().add(noBookings);
            return;
        }

        for (Booking booking : bookings) {
            if (!booking.isCancelled()) {
                VBox bookingCard = createBookingCard(booking);
                bookingsContainer.getChildren().add(bookingCard);
            }
        }
    }

    private VBox createBookingCard(Booking booking) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 16;");
        card.setPrefWidth(200);
        card.setAlignment(Pos.TOP_LEFT);

        TimeSlot slot = timeSlotDAO.findById(booking.getSlotId());
        if (slot == null) return card;

        TeacherProfile teacher = teacherProfileDAO.findById(slot.getTeacherProfileId());
        String teacherName = "Unknown";
        String instrument = "Unknown";

        if (teacher != null) {
            User teacherUser = userDAO.findById(teacher.getUserId());
            teacherName = (teacherUser != null) ? teacherUser.getUsername() : "Teacher";
            instrument = teacher.getInstrumentsTaught();
        }

        Label instrumentLabel = new Label(instrument + ", " + teacherName);
        instrumentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096;");

        Label dateLabel = new Label(slot.getLessonDate().format(dateFormatter));
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096;");

        String timeText = slot.getStartTime() + " - " + slot.getEndTime();

        HBox timeBox = new HBox(8);
        timeBox.setAlignment(Pos.CENTER_LEFT);

        Button timeBtn = new Button(timeText);
        timeBtn.setStyle("-fx-background-color: #2D4A47; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16;");
        timeBtn.setPrefWidth(120);

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteBooking(booking, slot));

        timeBox.getChildren().addAll(timeBtn, deleteBtn);

        card.getChildren().addAll(instrumentLabel, dateLabel, timeBox);

        return card;
    }

    private void handleDeleteBooking(Booking booking, TimeSlot slot) {
        booking.setBookingStatus(Booking.STATUS_CANCELLED);
        boolean updated = bookingDAO.update(booking);

        if (updated) {
            timeSlotDAO.updateStatus(slot.getSlotId(), TimeSlot.STATUS_AVAILABLE);
            loadBookings();
        }
    }

    @FXML
    private void handleBackToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/student_course_booking.fxml"));
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePrevPage(ActionEvent event) {
        // Placeholder for pagination
    }

    @FXML
    private void handleNextPage(ActionEvent event) {
        // Placeholder for pagination
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

