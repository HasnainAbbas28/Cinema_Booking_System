import java.util.ArrayList;
import java.util.List;

// --- ENUMS ---
enum MovieCategory { ACTION, COMEDY, DRAMA, HORROR, SCI_FI }
enum SeatStatus { AVAILABLE, BOOKED, LOCKED }
enum SeatType { REGULAR, PREMIUM }
enum BookingStatus { CONFIRMED, CANCELLED, PENDING }
enum PaymentMode { CREDIT_CARD, PAYPAL, UPI, CASH }

// --- INTERFACES --
interface PaymentProcessor {
    boolean processPayment(double amount);
    boolean refundPayment(double amount);
}

// --- USER MANAGEMENT ---
abstract class User {
    protected String userID;
    protected String name;
    protected String email;
    protected String password;

    public User(String userID, String name, String email, String password) {
        this.userID = userID;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getUserID() { return userID; }
    public String getName() { return name; }
}

class Customer extends User {
    public Customer(String userID, String name, String email, String password) {
        super(userID, name, email, password);
    }
}

class Admin extends User {
    public Admin(String userID, String name, String email, String password) {
        super(userID, name, email, password);
    }
}

class SuperAdmin extends Admin {
    public SuperAdmin(String userID, String name, String email, String password) {
        super(userID, name, email, password);
    }
}

class UserManager {
    private List<User> users;
    private int customerCounter = 1;
    private int adminCounter = 1;

    public UserManager() {
        users = new ArrayList<>();
        users.add(new SuperAdmin("SA001", "Chief Admin", "super@cinema.com", "super123"));
        users.add(new Admin("A001", "System Admin", "admin@cinema.com", "admin123"));
        users.add(new Customer("B001", "Alice Smith", "alice@mail.com", "alice123"));
        users.add(new Customer("B002", "Bob Jones", "bob@mail.com", "bob123"));
    }

    public User login(String email, String password) {
        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(email) && u.getPassword().equals(password)) return u;
        }
        return null;
    }

    public User createAccount(String name, String email, String password) {
        String generatedID = "B" + String.format("%03d", customerCounter++);
        User newCust = new Customer(generatedID, name, email, password);
        users.add(newCust);
        return newCust;
    }

    public User createAdmin(String name, String email, String password) {
        String generatedID = "A" + String.format("%03d", adminCounter++);
        User newAdmin = new Admin(generatedID, name, email, password);
        users.add(newAdmin);
        return newAdmin;
    }

    public boolean removeCustomer(String email) {
        return users.removeIf(u -> u.getEmail().equalsIgnoreCase(email) && u instanceof Customer);
    }

    // NEW: Needed for Admin to book on behalf of users
    public User getUserByEmail(String email) {
        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(email)) return u;
        }
        return null;
    }
}

// --- PHYSICAL CINEMA LAYOUT STRUCTURES ---
class Seat {
    private String seatNumber;
    private SeatType type;
    private SeatStatus status;

    public Seat(String seatNumber, SeatType type) {
        this.seatNumber = seatNumber;
        this.type = type;
        this.status = SeatStatus.AVAILABLE;
    }

    public String getSeatNumber() { return seatNumber; }
    public SeatType getType() { return type; }
    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }
}

class Screen {
    private String screenID;
    private String screenName;
    private Seat[][] seats;

    public Screen(String screenID, String screenName, int rows, int cols) {
        this.screenID = screenID;
        this.screenName = screenName;
        this.seats = new Seat[rows][cols];
        initializeSeats(rows, cols);
    }

    private void initializeSeats(int rows, int cols) {
        for (int r = 0; r < rows; r++) {
            char rowChar = (char) ('A' + r);
            for (int c = 0; c < cols; c++) {
                String seatNum = "" + rowChar + (c + 1);
                SeatType type = (r >= rows - 2) ? SeatType.PREMIUM : SeatType.REGULAR;
                seats[r][c] = new Seat(seatNum, type);
            }
        }
    }

    public String getScreenID() { return screenID; }
    public String getScreenName() { return screenName; }
    public Seat[][] getSeats() { return seats; }

    public Seat getSeatByID(String seatID) {
        for(Seat[] row : seats) {
            for(Seat s : row) {
                if(s.getSeatNumber().equalsIgnoreCase(seatID)) return s;
            }
        }
        return null;
    }

    @Override
    public String toString() { return screenName; }
}

class CinemaBranch {
    private String branchID;
    private String locationName;
    private List<Screen> screens;

    public CinemaBranch(String branchID, String locationName) {
        this.branchID = branchID;
        this.locationName = locationName;
        this.screens = new ArrayList<>();
    }

    public void addScreen(Screen s) { screens.add(s); }
    public String getBranchID() { return branchID; }
    public String getLocationName() { return locationName; }
    public List<Screen> getScreens() { return screens; }

    @Override
    public String toString() { return locationName; }
}

// --- MOVIE MANAGEMENT CATALOG & SCHEDULES ---
class Movie {
    private static int movieCounter = 1;
    private String movieID;
    private String title;
    private MovieCategory category;
    private int durationMinutes;

    public Movie(String title, MovieCategory category, int durationMinutes) {
        this.movieID = "M" + String.format("%03d", movieCounter++);
        this.title = title;
        this.category = category;
        this.durationMinutes = durationMinutes;
    }

    public String getMovieID() { return movieID; }
    public String getTitle() { return title; }
    public MovieCategory getCategory() { return category; }
    public int getDurationMinutes() { return durationMinutes; }

    public void setTitle(String title) { this.title = title; }
    public void setCategory(MovieCategory category) { this.category = category; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    @Override
    public String toString() { return title + " (" + durationMinutes + "m)"; }
}

class Show {
    private static int showCounter = 1;
    private String showID;
    private Movie movie;
    private int startTime;
    private Screen screen;
    private boolean is3D;

    public Show(Movie movie, int startTime, Screen screen, boolean is3D) {
        this.showID = "SH" + String.format("%03d", showCounter++);
        this.movie = movie;
        this.startTime = startTime;
        this.screen = screen;
        this.is3D = is3D;
    }

    public String getShowID() { return showID; }
    public Movie getMovie() { return movie; }
    public int getStartTime() { return startTime; }
    public Screen getScreen() { return screen; }
    public boolean is3D() { return is3D; }

    public void setMovie(Movie movie) { this.movie = movie; }
    public void setStartTime(int startTime) { this.startTime = startTime; }
    public void setScreen(Screen screen) { this.screen = screen; }
    public void set3D(boolean is3D) { this.is3D = is3D; }

    @Override
    public String toString() { return movie.getTitle() + " | Time: " + startTime + " | " + screen.getScreenName(); }
}

// --- RESERVATION ENGINE & CHECKOUT ---
class Ticket {
    private String ticketID;
    private Booking booking;
    private static int ticketCounter = 1;

    public Ticket(Booking booking) {
        this.ticketID = "TIK-" + String.format("%04d", ticketCounter++);
        this.booking = booking;
    }
}

class PaymentRecord implements PaymentProcessor {
    private String paymentID;
    private double amountPaid;
    private PaymentMode mode;
    private boolean isSuccessful;
    private static int paymentCounter = 1;

    public PaymentRecord(double amountPaid, PaymentMode mode) {
        this.paymentID = "PAY-" + String.format("%04d", paymentCounter++);
        this.amountPaid = amountPaid;
        this.mode = mode;
        this.isSuccessful = false;
    }

    @Override
    public boolean processPayment(double amount) {
        this.isSuccessful = true;
        return true;
    }

    @Override
    public boolean refundPayment(double amount) { return true; }
}

class Booking {
    private String bookingID;
    private User user;
    private Show show;
    private List<Seat> seats;
    private double totalAmount;
    private BookingStatus status;
    private PaymentRecord paymentRecord;
    private static int bookingCounter = 1;

    public Booking(User user, Show show, List<Seat> seats) {
        this.bookingID = "BK-" + String.format("%04d", bookingCounter++);
        this.user = user;
        this.show = show;
        this.seats = seats;
        this.status = BookingStatus.PENDING;
        this.totalAmount = calculateTotalAmount();
    }

    private double calculateTotalAmount() {
        double sum = 0;
        for (Seat s : seats) {
            sum += (s.getType() == SeatType.PREMIUM) ? CinemaBookingSystem.premiumPrice : CinemaBookingSystem.regularPrice;
        }
        return sum;
    }

    public void confirmBooking() { this.status = BookingStatus.CONFIRMED; }

    public String getBookingID() { return bookingID; }
    public User getUser() { return user; }
    public Show getShow() { return show; }
    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; this.totalAmount = calculateTotalAmount(); }
    public double getTotalAmount() { return totalAmount; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public PaymentRecord getPaymentRecord() { return paymentRecord; }
    public void setPaymentRecord(PaymentRecord p) { this.paymentRecord = p; }

    @Override
    public String toString() {
        return bookingID + " | " + user.getName() + " | " + show.getMovie().getTitle() + " | " + status;
    }
}

class BookingManager {
    private List<Booking> bookings = new ArrayList<>();

    public List<Booking> getAllBookings() { return bookings; }

    public Booking makeBooking(User user, Show show, List<Seat> seats, PaymentMode paymentMode) {
        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) return null;
            seat.setStatus(SeatStatus.BOOKED);
        }

        Booking booking = new Booking(user, show, seats);
        PaymentRecord payment = new PaymentRecord(booking.getTotalAmount(), paymentMode);

        if (!payment.processPayment(booking.getTotalAmount())) {
            for (Seat seat : seats) seat.setStatus(SeatStatus.AVAILABLE);
            return null;
        }

        booking.setPaymentRecord(payment);
        booking.confirmBooking();
        bookings.add(booking);
        return booking;
    }

    public String adminUpdateBookingSeats(Booking booking, List<Seat> newSeats) {
        if (booking.getSeats().size() != newSeats.size()) return "Must select the same number of seats.";
        for (int i = 0; i < newSeats.size(); i++) {
            if (newSeats.get(i).getStatus() != SeatStatus.AVAILABLE) return "Target seat " + newSeats.get(i).getSeatNumber() + " is not available.";
            if (booking.getSeats().get(i).getType() != newSeats.get(i).getType()) return "Target seat " + newSeats.get(i).getSeatNumber() + " type mismatch.";
        }

        for (Seat s : booking.getSeats()) s.setStatus(SeatStatus.AVAILABLE);
        for (Seat s : newSeats) s.setStatus(SeatStatus.BOOKED);
        booking.setSeats(newSeats);
        return "Seats successfully updated.";
    }

    public void adminCancelBooking(Booking booking) {
        booking.setStatus(BookingStatus.CANCELLED);
        for (Seat s : booking.getSeats()) s.setStatus(SeatStatus.AVAILABLE);
        if (booking.getPaymentRecord() != null) booking.getPaymentRecord().refundPayment(booking.getTotalAmount());
    }
}

// --- MAIN RUNTIME APPLICATION INTERFACE ---
public class CinemaBookingSystem {
    private static List<CinemaBranch> branches = new ArrayList<>();
    private static List<Movie> movieCatalog = new ArrayList<>();
    private static List<Show> showSchedule = new ArrayList<>();
    public static double premiumPrice = 1000.0;
    public static double regularPrice = 500.0;

    private static UserManager userManager;
    private static BookingManager bookingSystem;

    public static List<CinemaBranch> getBranches() { return branches; }
    public static List<Movie> getMovieCatalog() { return movieCatalog; }
    public static List<Show> getShowSchedule() { return showSchedule; }
    public static UserManager getUserManager() { return userManager; }
    public static BookingManager getBookingSystem() { return bookingSystem; }

    public static List<Show> getShowsForMovieAndBranch(Movie movie, CinemaBranch branch) {
        List<Show> filtered = new ArrayList<>();
        for (Show show : showSchedule) {
            if (show.getMovie().getMovieID().equals(movie.getMovieID())) {
                for (Screen screen : branch.getScreens()) {
                    if (show.getScreen().getScreenID().equals(screen.getScreenID())) {
                        filtered.add(show);
                    }
                }
            }
        }
        return filtered;
    }

    public static void addMovie(Movie m) { movieCatalog.add(m); }
    public static void removeMovie(Movie m) { movieCatalog.remove(m); }
    public static void addShow(Show s) { showSchedule.add(s); }
    public static void removeShow(Show s) { showSchedule.remove(s); }

    public static void initializeSystem() {
        userManager = new UserManager();
        bookingSystem = new BookingManager();

        Movie m1 = new Movie("Dune: Part Two", MovieCategory.SCI_FI, 166);
        Movie m2 = new Movie("Avengers: Doomsday", MovieCategory.ACTION, 150);
        Movie m3 = new Movie("Joker: Folie à Deux", MovieCategory.DRAMA, 138);
        Movie m4 = new Movie("Deadpool & Wolverine", MovieCategory.ACTION, 127);
        Movie m5 = new Movie("Gladiator II", MovieCategory.DRAMA, 145);
        Movie m6 = new Movie("Nosferatu", MovieCategory.HORROR, 132);
        Movie m7 = new Movie("Beetlejuice Beetlejuice", MovieCategory.COMEDY, 104);

        movieCatalog.addAll(List.of(m1, m2, m3, m4, m5, m6, m7));

        CinemaBranch hkBranch = new CinemaBranch("C-HK01", "iPlex HK");
        Screen hk1 = new Screen("HK-S1", "Screen 1", 6, 10);
        Screen hk2 = new Screen("HK-S2", "Screen 2", 6, 10);
        Screen hk3 = new Screen("HK-S3", "Screen 3", 6, 10);
        Screen hk4 = new Screen("HK-S4", "Screen 4", 6, 10);
        hkBranch.getScreens().addAll(List.of(hk1, hk2, hk3, hk4));

        CinemaBranch bwBranch = new CinemaBranch("C-BW02", "iPlex BW");
        Screen bw1 = new Screen("BW-S1", "Screen 1", 6, 10);
        Screen bw2 = new Screen("BW-S2", "Screen 2", 6, 10);
        Screen bw3 = new Screen("BW-S3", "Screen 3", 6, 10);
        Screen bw4 = new Screen("BW-S4", "Screen 4", 6, 10);
        Screen bw5 = new Screen("BW-S5", "Screen 5", 6, 10);
        Screen bw6 = new Screen("BW-S6", "Screen 6", 6, 10);
        bwBranch.getScreens().addAll(List.of(bw1, bw2, bw3, bw4, bw5, bw6));

        branches.addAll(List.of(hkBranch, bwBranch));

        showSchedule.add(new Show(m1, 1300, hk1, true));
        showSchedule.add(new Show(m1, 1700, hk1, true));
        showSchedule.add(new Show(m4, 1430, hk2, false));
        showSchedule.add(new Show(m3, 1800, hk3, false));
        showSchedule.add(new Show(m6, 2100, hk4, false));

        showSchedule.add(new Show(m2, 1200, bw1, true));
        showSchedule.add(new Show(m2, 1600, bw1, true));
        showSchedule.add(new Show(m5, 1445, bw3, false));
        showSchedule.add(new Show(m7, 1230, bw4, false));

        User alice = userManager.login("alice@mail.com", "alice123");
        Show aliceShow = showSchedule.get(0);
        List<Seat> aliceSeats = new ArrayList<>();
        aliceSeats.add(aliceShow.getScreen().getSeats()[0][0]); // Seat A1
        aliceSeats.add(aliceShow.getScreen().getSeats()[0][1]); // Seat A2
        bookingSystem.makeBooking(alice, aliceShow, aliceSeats, PaymentMode.CREDIT_CARD);
    }
}