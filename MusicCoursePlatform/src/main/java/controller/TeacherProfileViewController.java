package controller;

import dao.TeacherProfileDAO;
import dao.TimeSlotDAO;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.TeacherProfile;
import model.TimeSlot;
import model.User;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TeacherProfileViewController {

    @FXML private Label teacherNameLabel;
    @FXML private Label weekLabel;
    @FXML private HBox availabilityGrid;
    @FXML private ComboBox<String> languageCombo;
    @FXML private ComboBox<String> dayCombo;
    @FXML private ComboBox<String> startTimeCombo;
    @FXML private ComboBox<String> endTimeCombo;

    private TeacherProfileDAO teacherProfileDAO;
    private TimeSlotDAO timeSlotDAO;

    private TeacherProfile teacherProfile;
    private LocalDate weekStart;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d");

    @FXML
    public void initialize() {
        teacherProfileDAO = new TeacherProfileDAO();
        timeSlotDAO = new TimeSlotDAO();

        weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        setupLanguageCombo();
        setupTimeComboBoxes();
        loadTeacherInfo();
        updateAvailabilityGrid();
    }

    private void setupLanguageCombo() {
        if (languageCombo != null) {
            languageCombo.getItems().addAll("EN", "DE", "ZH");
            languageCombo.setValue("EN");
        }
    }

    private void setupTimeComboBoxes() {
        dayCombo.getItems().addAll(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        );

        String[] times = {
            "08:00", "09:00", "10:00", "11:00", "12:00",
            "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00"
        };

        startTimeCombo.getItems().addAll(times);
        endTimeCombo.getItems().addAll(times);
    }

    private void loadTeacherInfo() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            teacherNameLabel.setText(currentUser.getUsername());
            teacherProfile = teacherProfileDAO.findByUserId(currentUser.getUserId());

            if (teacherProfile == null) {
                teacherProfile = new TeacherProfile(currentUser.getUserId(), "Piano");
                teacherProfileDAO.create(teacherProfile);
            }
        }
    }

    private void updateAvailabilityGrid() {
        availabilityGrid.getChildren().clear();

        weekLabel.setText(weekStart.format(DateTimeFormatter.ofPattern("MMM d")) +
            " - " + weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d, yyyy")));

        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            VBox dayColumn = createDayColumn(date);
            availabilityGrid.getChildren().add(dayColumn);
        }
    }

    private VBox createDayColumn(LocalDate date) {
        VBox column = new VBox(8);
        column.setAlignment(Pos.TOP_CENTER);
        column.setPrefWidth(130);
        column.setStyle("-fx-background-color: #F7FAFC; -fx-background-radius: 8; -fx-padding: 12;");

        Label dateLabel = new Label(date.format(dateFormatter));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #718096;");
        dateLabel.setWrapText(true);
        dateLabel.setMaxWidth(110);
        column.getChildren().add(dateLabel);

        if (teacherProfile != null) {
            List<TimeSlot> slots = timeSlotDAO.findByTeacherProfileIdAndDate(
                teacherProfile.getTeacherProfileId(), date);

            for (TimeSlot slot : slots) {
                HBox slotBox = createSlotBox(slot);
                column.getChildren().add(slotBox);
            }
        }

        return column;
    }

    private HBox createSlotBox(TimeSlot slot) {
        HBox box = new HBox(4);
        box.setAlignment(Pos.CENTER);

        String timeText = slot.getStartTime() + "-" + slot.getEndTime();

        Button timeBtn = new Button(timeText);
        timeBtn.setPrefWidth(80);
        if (slot.isAvailable()) {
            timeBtn.setStyle("-fx-background-color: white; -fx-border-color: #2D4A47; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 6 8; -fx-font-size: 10px;");
        } else {
            timeBtn.setStyle("-fx-background-color: #CBD5E0; -fx-border-color: #CBD5E0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 6 8; -fx-text-fill: #718096; -fx-font-size: 10px;");
        }

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 12px;");
        deleteBtn.setOnAction(e -> handleDeleteSlot(slot));

        box.getChildren().addAll(timeBtn, deleteBtn);

        return box;
    }

    @FXML
    private void handleAddTimeSlot(ActionEvent event) {
        String selectedDay = dayCombo.getValue();
        String startTime = startTimeCombo.getValue();
        String endTime = endTimeCombo.getValue();

        if (selectedDay == null || startTime == null || endTime == null) {
            showError("Please select day, start time, and end time");
            return;
        }

        if (startTime.compareTo(endTime) >= 0) {
            showError("End time must be after start time");
            return;
        }

        if (teacherProfile == null) {
            showError("Teacher profile not found");
            return;
        }

        LocalDate slotDate = getDateForDay(selectedDay);

        TimeSlot newSlot = new TimeSlot(
            teacherProfile.getTeacherProfileId(),
            slotDate,
            startTime,
            endTime
        );

        boolean created = timeSlotDAO.create(newSlot);
        if (created) {
            updateAvailabilityGrid();
            dayCombo.setValue(null);
            startTimeCombo.setValue(null);
            endTimeCombo.setValue(null);
        } else {
            showError("Failed to create time slot");
        }
    }

    private LocalDate getDateForDay(String day) {
        int dayOffset = switch (day) {
            case "Monday" -> 0;
            case "Tuesday" -> 1;
            case "Wednesday" -> 2;
            case "Thursday" -> 3;
            case "Friday" -> 4;
            case "Saturday" -> 5;
            case "Sunday" -> 6;
            default -> 0;
        };
        return weekStart.plusDays(dayOffset);
    }

    private void handleDeleteSlot(TimeSlot slot) {
        boolean deleted = timeSlotDAO.delete(slot.getSlotId());
        if (deleted) {
            updateAvailabilityGrid();
        }
    }

    @FXML
    private void handlePrevPage(ActionEvent event) {
        weekStart = weekStart.minusWeeks(1);
        updateAvailabilityGrid();
    }

    @FXML
    private void handleNextPage(ActionEvent event) {
        weekStart = weekStart.plusWeeks(1);
        updateAvailabilityGrid();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/teacher_set_availability.fxml"));
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    @FXML private Label errorLabel;

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setStyle("-fx-text-fill: #e53e3e;");
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }
}

