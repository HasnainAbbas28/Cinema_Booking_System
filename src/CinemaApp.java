import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CinemaApp extends Application {

    private Stage window;
    private User loggedInUser;

    // Core Navigation State Trackers
    private CinemaBranch selectedBranch;
    private Show selectedShow;
    private List<Seat> selectedSeats = new ArrayList<>();

    // UI Hijack Modes for Admin Overrides
    private boolean isAdminLockingMode = false;
    private boolean isAdminBookingMode = false;
    private User adminTargetUser = null; // Holds the customer the admin is booking for

    public static void main(String[] args) {
        CinemaBookingSystem.initializeSystem();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        window = primaryStage;
        window.setTitle("iPlex Cinemas - Booking Hub");
        window.setScene(createLoginScreen());
        window.show();
    }

    // =========================================================================
    // STANDARD CUSTOMER SCREENS (Reused for Admin Overrides)
    // =========================================================================
    private Scene createLoginScreen() {
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));

        Label titleLabel = new Label("Welcome to iPlex Cinemas");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));

        TextField emailInput = new TextField();
        emailInput.setPromptText("Email Address");

        PasswordField passInput = new PasswordField();
        passInput.setPromptText("Password");

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);

        Button loginBtn = new Button("Login");

        loginBtn.setOnAction(e -> {
            loggedInUser = CinemaBookingSystem.getUserManager()
                    .login(emailInput.getText(), passInput.getText());

            if (loggedInUser != null) {

                isAdminLockingMode = false;
                isAdminBookingMode = false;
                adminTargetUser = null;

                if (loggedInUser instanceof Admin) {
                    window.setScene(createAdminDashboard());
                } else {
                    window.setScene(createBranchSelectionScreen());
                }

            } else {
                errorLabel.setText("Invalid credentials.");
            }
        });

        // ================= CREATE ACCOUNT BUTTON =================

        Button createAccountBtn = new Button("Create Account");

        createAccountBtn.setOnAction(e -> {

            Dialog<User> dialog = new Dialog<>();
            dialog.setTitle("Create Account");
            dialog.setHeaderText("Register New Customer Account");

            ButtonType registerButtonType =
                    new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);

            dialog.getDialogPane().getButtonTypes()
                    .addAll(registerButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            TextField nameField = new TextField();
            nameField.setPromptText("Full Name");

            TextField emailField = new TextField();
            emailField.setPromptText("Email");

            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Password");

            grid.add(new Label("Name:"), 0, 0);
            grid.add(nameField, 1, 0);

            grid.add(new Label("Email:"), 0, 1);
            grid.add(emailField, 1, 1);

            grid.add(new Label("Password:"), 0, 2);
            grid.add(passwordField, 1, 2);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {

                if (dialogButton == registerButtonType) {

                    String name = nameField.getText().trim();
                    String email = emailField.getText().trim();
                    String password = passwordField.getText().trim();

                    if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        showAlert("Error", "All fields are required.");
                        return null;
                    }

                    User existingUser = CinemaBookingSystem
                            .getUserManager()
                            .getUserByEmail(email);

                    if (existingUser != null) {
                        showAlert("Error", "Account already exists.");
                        return null;
                    }

                    User newUser = CinemaBookingSystem
                            .getUserManager()
                            .createAccount(name, email, password);

                    showAlert("Success", "Account Created Successfully!");

                    return newUser;
                }

                return null;
            });

            dialog.showAndWait();
        });

        // =========================================================

        layout.getChildren().addAll(
                titleLabel,
                emailInput,
                passInput,
                loginBtn,
                createAccountBtn,
                errorLabel
        );

        return new Scene(layout, 850, 600);
    }

    private Scene createBranchSelectionScreen() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label();
        if (isAdminLockingMode) titleLabel.setText("ADMIN VISUAL LOCK: Select Branch");
        else if (isAdminBookingMode) titleLabel.setText("ADMIN BOOKING: Select Branch for " + adminTargetUser.getName());
        else titleLabel.setText("Select a Cinema Branch");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        for (CinemaBranch branch : CinemaBookingSystem.getBranches()) {
            Button branchBtn = new Button(branch.getLocationName());
            branchBtn.setOnAction(e -> {
                selectedBranch = branch;
                window.setScene(createMovieGridScreen());
            });
            buttonBox.getChildren().add(branchBtn);
        }

        Button backBtn = new Button(isAdminLockingMode || isAdminBookingMode ? "Cancel Override (Back to Dashboard)" : "Logout");
        backBtn.setOnAction(e -> {
            if(isAdminLockingMode || isAdminBookingMode) {
                isAdminLockingMode = false;
                isAdminBookingMode = false;
                adminTargetUser = null;
                window.setScene(createAdminDashboard());
            } else {
                loggedInUser = null;
                window.setScene(createLoginScreen());
            }
        });

        layout.getChildren().addAll(titleLabel, buttonBox, backBtn);
        return new Scene(layout, 850, 600);
    }

    private Scene createMovieGridScreen() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label("Now Showing at " + selectedBranch.getLocationName());
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        ScrollPane scrollPane = new ScrollPane();
        VBox movieListBox = new VBox(15);

        for (Movie movie : CinemaBookingSystem.getMovieCatalog()) {
            List<Show> availableShows = CinemaBookingSystem.getShowsForMovieAndBranch(movie, selectedBranch);
            if (!availableShows.isEmpty()) {
                HBox movieRow = new HBox(15);
                Label movieTitle = new Label(movie.getTitle());
                ComboBox<Show> showtimeDropdown = new ComboBox<>();
                showtimeDropdown.getItems().addAll(availableShows);

                Button selectSeatsBtn = new Button(isAdminLockingMode ? "Manage Locks" : "Select Seats");
                selectSeatsBtn.setDisable(true);
                showtimeDropdown.setOnAction(e -> selectSeatsBtn.setDisable(false));

                selectSeatsBtn.setOnAction(e -> {
                    selectedShow = showtimeDropdown.getValue();
                    selectedSeats.clear();
                    window.setScene(createSeatMatrixScreen());
                });
                movieRow.getChildren().addAll(movieTitle, showtimeDropdown, selectSeatsBtn);
                movieListBox.getChildren().add(movieRow);
            }
        }
        scrollPane.setContent(movieListBox);
        Button backBtn = new Button("Back to Branches");
        backBtn.setOnAction(e -> window.setScene(createBranchSelectionScreen()));
        layout.getChildren().addAll(titleLabel, scrollPane, backBtn);
        return new Scene(layout, 850, 600);
    }

    private Scene createSeatMatrixScreen() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        Label titleLabel = new Label("Select Seats - " + selectedShow.getMovie().getTitle());
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        GridPane seatGrid = new GridPane();
        seatGrid.setAlignment(Pos.CENTER);
        seatGrid.setHgap(5); seatGrid.setVgap(5);

        Seat[][] backendSeats = selectedShow.getScreen().getSeats();
        Button proceedBtn = new Button(isAdminLockingMode ? "Done Locking (Return)" : "Proceed to Payment");
        if (!isAdminLockingMode) proceedBtn.setDisable(true);

        for (int r = 0; r < backendSeats.length; r++) {
            for (int c = 0; c < backendSeats[r].length; c++) {
                Seat seatObj = backendSeats[r][c];
                Button seatBtn = new Button(seatObj.getSeatNumber());

                if (isAdminLockingMode) {
                    if (seatObj.getStatus() == SeatStatus.BOOKED) {
                        seatBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                        seatBtn.setDisable(true);
                    } else if (seatObj.getStatus() == SeatStatus.LOCKED) {
                        seatBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: black;");
                        seatBtn.setOnAction(e -> {
                            seatObj.setStatus(SeatStatus.AVAILABLE);
                            window.setScene(createSeatMatrixScreen());
                        });
                    } else {
                        seatBtn.setStyle("-fx-background-color: #E0E0E0;");
                        seatBtn.setOnAction(e -> {
                            seatObj.setStatus(SeatStatus.LOCKED);
                            window.setScene(createSeatMatrixScreen());
                        });
                    }
                } else {
                    if (seatObj.getStatus() != SeatStatus.AVAILABLE) {
                        seatBtn.setStyle("-fx-background-color: #F44336;");
                        seatBtn.setDisable(true);
                    } else {
                        seatBtn.setOnAction(e -> {
                            if (selectedSeats.contains(seatObj)) {
                                selectedSeats.remove(seatObj);
                                seatBtn.setStyle("");
                            } else {
                                selectedSeats.add(seatObj);
                                seatBtn.setStyle("-fx-background-color: #4CAF50;");
                            }
                            proceedBtn.setDisable(selectedSeats.isEmpty());
                        });
                    }
                }
                seatGrid.add(seatBtn, c, r);
            }
        }

        Button backBtn = new Button("Back to Shows");
        backBtn.setOnAction(e -> window.setScene(createMovieGridScreen()));

        if (isAdminLockingMode) {
            proceedBtn.setOnAction(e -> {
                isAdminLockingMode = false;
                window.setScene(createAdminDashboard());
            });
        } else {
            proceedBtn.setOnAction(e -> window.setScene(createPaymentScreen()));
        }

        layout.getChildren().addAll(titleLabel, seatGrid, new HBox(10, backBtn, proceedBtn));
        return new Scene(layout, 850, 600);
    }

    private Scene createPaymentScreen() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);

        Button payBtn = new Button("Confirm Payment");
        payBtn.setOnAction(e -> {
            // Check if admin is booking on behalf of customer
            User activeUser = isAdminBookingMode ? adminTargetUser : loggedInUser;

            Booking b = CinemaBookingSystem.getBookingSystem().makeBooking(activeUser, selectedShow, selectedSeats, PaymentMode.CREDIT_CARD);
            if (b != null) {
                showAlert("Success", "Booking Processed Successfully!");
                if (isAdminBookingMode) {
                    isAdminBookingMode = false;
                    adminTargetUser = null;
                    window.setScene(createAdminDashboard());
                } else {
                    window.setScene(createBranchSelectionScreen());
                }
            }
        });

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> window.setScene(createSeatMatrixScreen()));
        layout.getChildren().addAll(new Label("Payment Confirmation"), payBtn, backBtn);
        return new Scene(layout, 850, 600);
    }


    // =========================================================================
    // UPDATED ADMIN DASHBOARD
    // =========================================================================
    private Scene createAdminDashboard() {
        BorderPane layout = new BorderPane();
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // --- TAB 1: MOVIE MANAGEMENT ---
        Tab movieTab = new Tab("Movies");
        VBox movieBox = new VBox(10); movieBox.setPadding(new Insets(15));
        ListView<Movie> movieListView = new ListView<>();
        movieListView.getItems().addAll(CinemaBookingSystem.getMovieCatalog());

        TextField mTitleInput = new TextField(); mTitleInput.setPromptText("Movie Title");
        ComboBox<MovieCategory> mCatInput = new ComboBox<>(); mCatInput.getItems().addAll(MovieCategory.values()); mCatInput.setPromptText("Category");
        TextField mDurInput = new TextField(); mDurInput.setPromptText("Duration (Mins)");

        Button addMovieBtn = new Button("Add Movie");
        addMovieBtn.setOnAction(e -> {
            Movie nm = new Movie(mTitleInput.getText(), mCatInput.getValue(), Integer.parseInt(mDurInput.getText()));
            CinemaBookingSystem.addMovie(nm);
            movieListView.getItems().add(nm);
            mTitleInput.clear(); mDurInput.clear(); mCatInput.setValue(null);
        });

        Button removeMovieBtn = new Button("Remove Movie");
        removeMovieBtn.setOnAction(e -> {
            Movie sel = movieListView.getSelectionModel().getSelectedItem();
            if(sel != null) { CinemaBookingSystem.removeMovie(sel); movieListView.getItems().remove(sel); }
        });

        movieBox.getChildren().addAll(new Label("Manage Movies:"), movieListView, new HBox(10, mTitleInput, mCatInput, mDurInput, addMovieBtn, removeMovieBtn));
        movieTab.setContent(movieBox);

        // --- TAB 2: SHOW MANAGEMENT (Now uses dynamic Branch -> Show layout) ---
        Tab showTab = new Tab("Shows");
        StackPane showTabContent = new StackPane(); // Dynamic container inside the tab
        showTabContent.getChildren().add(createAdminBranchSelectionForShows(showTabContent));
        showTab.setContent(showTabContent);

        // --- TAB 3: BOOKING MANAGEMENT ---
        Tab bookingTab = new Tab("Bookings");
        VBox bookBox = new VBox(10); bookBox.setPadding(new Insets(15));
        ListView<Booking> bookingList = new ListView<>();
        bookingList.getItems().addAll(CinemaBookingSystem.getBookingSystem().getAllBookings());

        Button cancelBookBtn = new Button("Cancel Booking");
        cancelBookBtn.setOnAction(e -> {
            Booking b = bookingList.getSelectionModel().getSelectedItem();
            if(b != null) {
                CinemaBookingSystem.getBookingSystem().adminCancelBooking(b);
                bookingList.refresh();
                showAlert("Cancelled", "Booking cancelled and seats freed.");
            }
        });

        Button editSeatsBtn = new Button("Edit Booking Seats");
        editSeatsBtn.setOnAction(e -> {
            Booking b = bookingList.getSelectionModel().getSelectedItem();
            if(b == null) return;

            TextInputDialog dialog = new TextInputDialog();
            dialog.setHeaderText("Enter NEW comma-separated Seat IDs (e.g. A1,A2) to move this customer to:");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String[] seatIDs = result.get().split(",");
                List<Seat> newSeats = new ArrayList<>();
                for(String id : seatIDs) {
                    Seat found = b.getShow().getScreen().getSeatByID(id.trim());
                    if(found != null) newSeats.add(found);
                }
                String msg = CinemaBookingSystem.getBookingSystem().adminUpdateBookingSeats(b, newSeats);
                showAlert("Seat Update", msg);
                bookingList.refresh();
            }
        });

        // NEW: Add Booking directly for a customer
        Button addBookingBtn = new Button("Add Booking for Customer");
        addBookingBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        addBookingBtn.setOnAction(e -> {
            TextInputDialog emailDialog = new TextInputDialog();
            emailDialog.setHeaderText("Enter Registered Customer Email:");
            Optional<String> emailResult = emailDialog.showAndWait();

            if (emailResult.isPresent() && !emailResult.get().trim().isEmpty()) {
                String email = emailResult.get().trim();
                User foundUser = CinemaBookingSystem.getUserManager().getUserByEmail(email);

                if (foundUser == null) {
                    foundUser = showAdminCustomerRegistrationDialog(email); // Register them if missing
                }

                if (foundUser != null) {
                    adminTargetUser = foundUser;
                    isAdminBookingMode = true; // Hijack UI flow
                    window.setScene(createBranchSelectionScreen());
                }
            }
        });

        Button visualLockBtn = new Button("Visually Lock/Unlock Seats");
        visualLockBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        visualLockBtn.setOnAction(e -> {
            isAdminLockingMode = true;
            window.setScene(createBranchSelectionScreen());
        });

        bookBox.getChildren().addAll(
                new Label("Manage Customer Bookings:"), bookingList,
                new HBox(10, cancelBookBtn, editSeatsBtn, addBookingBtn),
                new Separator(), new Label("Global Tools:"), visualLockBtn
        );
        bookingTab.setContent(bookBox);

        tabPane.getTabs().addAll(movieTab, showTab, bookingTab);

        if (loggedInUser instanceof SuperAdmin) {
            Tab superTab = new Tab("Super Admin");
            VBox superBox = new VBox(10); superBox.setPadding(new Insets(15));
            TextField newAdName = new TextField(); newAdName.setPromptText("Admin Name");
            TextField newAdEmail = new TextField(); newAdEmail.setPromptText("Admin Email");
            TextField newAdPass = new TextField(); newAdPass.setPromptText("Admin Password");
            Button createAdBtn = new Button("Register New Admin");

            createAdBtn.setOnAction(e -> {
                CinemaBookingSystem.getUserManager().createAdmin(newAdName.getText(), newAdEmail.getText(), newAdPass.getText());
                showAlert("Success", "New Admin Registered.");
                newAdName.clear(); newAdEmail.clear(); newAdPass.clear();
            });

            superBox.getChildren().addAll(new Label("Register Administrative Personnel:"), newAdName, newAdEmail, newAdPass, createAdBtn);
            superTab.setContent(superBox);
            tabPane.getTabs().add(superTab);
        }

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> { loggedInUser = null; window.setScene(createLoginScreen()); });
        HBox bottom = new HBox(logoutBtn); bottom.setAlignment(Pos.CENTER_RIGHT); bottom.setPadding(new Insets(10));

        layout.setCenter(tabPane);
        layout.setBottom(bottom);
        return new Scene(layout, 850, 600);
    }

    // =========================================================================
    // ADMIN SHOW MANAGEMENT UI COMPONENTS (Dynamic Tab Content)
    // =========================================================================
    private VBox createAdminBranchSelectionForShows(StackPane container) {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().add(new Label("Select a Branch to Manage its Schedule:"));

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        for (CinemaBranch branch : CinemaBookingSystem.getBranches()) {
            Button branchBtn = new Button(branch.getLocationName());
            branchBtn.setOnAction(e -> {
                // Swap the container content forward to the Shows view
                container.getChildren().setAll(createAdminShowsListForBranch(container, branch));
            });
            buttonBox.getChildren().add(branchBtn);
        }
        layout.getChildren().add(buttonBox);
        return layout;
    }

    private VBox createAdminShowsListForBranch(StackPane container, CinemaBranch branch) {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getChildren().add(new Label("Managing Schedule for: " + branch.getLocationName()));

        ListView<Show> showListView = new ListView<>();
        List<Show> branchShows = new ArrayList<>();
        for(Show s : CinemaBookingSystem.getShowSchedule()) {
            if (branch.getScreens().contains(s.getScreen())) branchShows.add(s);
        }
        showListView.getItems().addAll(branchShows);

        // Control Inputs
        ComboBox<Movie> movieCombo = new ComboBox<>(); movieCombo.getItems().addAll(CinemaBookingSystem.getMovieCatalog());
        ComboBox<Screen> screenCombo = new ComboBox<>(); screenCombo.getItems().addAll(branch.getScreens());
        TextField timeInput = new TextField(); timeInput.setPromptText("Time (e.g. 1400)");
        CheckBox is3DBox = new CheckBox("Premium (3D)");

        // BUG FIX: The listener that auto-populates fields when a show is clicked
        showListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                movieCombo.setValue(newVal.getMovie());
                screenCombo.setValue(newVal.getScreen());
                timeInput.setText(String.valueOf(newVal.getStartTime()));
                is3DBox.setSelected(newVal.is3D());
            }
        });

        Button addShowBtn = new Button("Add Show");
        addShowBtn.setOnAction(e -> {
            if (movieCombo.getValue() != null && screenCombo.getValue() != null && !timeInput.getText().isEmpty()) {
                Show newShow = new Show(movieCombo.getValue(), Integer.parseInt(timeInput.getText()), screenCombo.getValue(), is3DBox.isSelected());
                CinemaBookingSystem.addShow(newShow);
                showListView.getItems().add(newShow);
                timeInput.clear();
            }
        });

        Button editShowBtn = new Button("Edit Selected Show");
        editShowBtn.setOnAction(e -> {
            Show sel = showListView.getSelectionModel().getSelectedItem();
            if(sel != null && movieCombo.getValue() != null && screenCombo.getValue() != null) {
                sel.setMovie(movieCombo.getValue());
                sel.setScreen(screenCombo.getValue());
                sel.setStartTime(Integer.parseInt(timeInput.getText()));
                sel.set3D(is3DBox.isSelected());
                showListView.refresh();
                showAlert("Success", "Show successfully updated!");
            } else {
                showAlert("Error", "Please select a show and ensure all fields are filled.");
            }
        });

        Button removeShowBtn = new Button("Remove Show");
        removeShowBtn.setOnAction(e -> {
            Show sel = showListView.getSelectionModel().getSelectedItem();
            if(sel != null) {
                CinemaBookingSystem.removeShow(sel);
                showListView.getItems().remove(sel);
            }
        });

        Button backBtn = new Button("Back to Branches");
        backBtn.setOnAction(e -> {
            // Swap the container content backward
            container.getChildren().setAll(createAdminBranchSelectionForShows(container));
        });

        layout.getChildren().addAll(
                showListView,
                new HBox(10, movieCombo, screenCombo, timeInput, is3DBox),
                new HBox(10, addShowBtn, editShowBtn, removeShowBtn, backBtn)
        );
        return layout;
    }

    // =========================================================================
    // UTILITY DIALOGS
    // =========================================================================
    private User showAdminCustomerRegistrationDialog(String email) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Register New Customer");
        dialog.setHeaderText("No account found for " + email + ".\nPlease provide details to register:");

        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        TextField nameInput = new TextField(); nameInput.setPromptText("Full Name");
        PasswordField passInput = new PasswordField(); passInput.setPromptText("Password");

        grid.add(new Label("Name:"), 0, 0); grid.add(nameInput, 1, 0);
        grid.add(new Label("Password:"), 0, 1); grid.add(passInput, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButtonType) {
                return CinemaBookingSystem.getUserManager().createAccount(nameInput.getText(), email, passInput.getText());
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.showAndWait();
    }
}