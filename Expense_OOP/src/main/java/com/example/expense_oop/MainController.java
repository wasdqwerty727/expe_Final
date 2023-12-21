package com.example.expense_oop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label balanceLabel;

    @FXML
    private ChoiceBox<String> categoryChoiceBox;

    @FXML
    private TextField amountField;

    @FXML
    private ChoiceBox<String> typeChoiceBox;

    @FXML
    private DatePicker dateDatePicker;

    @FXML
    private Button logoutButton;
    @FXML
    private Button addButton;

    @FXML
    private TableView<Transaction> transactionsTableView;

    @FXML
    private TableColumn<Transaction, String> categoryColumn;

    @FXML
    private TableColumn<Transaction, Double> amountColumn;

    @FXML
    private TableColumn<Transaction, String> typeColumn;

    @FXML
    private TableColumn<Transaction, LocalDate> dateColumn;

    @FXML
    private TableColumn<Transaction, String> currencyColumn;
    private ObservableList<Transaction> transactionsList = FXCollections.observableArrayList();
    private String username;

    @FXML
    private void initialize() {
        loadCategories();
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        currencyColumn.setCellValueFactory(new PropertyValueFactory<>("currency"));

        typeChoiceBox.getItems().addAll("Income", "Expense");
        logoutButton.setOnAction(event -> logoutButtonClicked());
        addButton.setOnAction(event -> addButtonClicked());

        configureDatePicker();
    }

    private void loadCategories() {
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT category_name FROM categories");
             ResultSet resultSet = statement.executeQuery()) {

            List<String> categories = new ArrayList<>();
            while (resultSet.next()) {
                categories.add(resultSet.getString("category_name"));
            }

            categoryChoiceBox.getItems().addAll(categories);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load categories from the database.");
        }
    }

    private void configureDatePicker() {
        StringConverter<LocalDate> converter = new StringConverter<>() {
            final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        };

        dateDatePicker.setConverter(converter);
        dateDatePicker.setPromptText("yyyy-MM-dd");
    }

    @FXML
    private void loginButtonClicked() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (authenticateUser(username, password)) {
            welcomeLabel.setText("Welcome " + username);

            displayUserBalance(username);

            updateTableView();

            this.username = username;
        } else {
            showAlert("Login Failed", "Invalid username or password.");
        }
    }

    private boolean authenticateUser(String username, String password) {
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM users WHERE username = ? AND password = ?")) {

            statement.setString(1, username);
            statement.setString(2, password);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    @FXML
    private void addButtonClicked() {
        String username = usernameField.getText();
        String category = categoryChoiceBox.getValue();
        String amountText = amountField.getText();
        String type = typeChoiceBox.getValue();
        LocalDate date = dateDatePicker.getValue();

        if (username.isEmpty() || category == null || amountText.isEmpty() || type == null || date == null) {
            showAlert("Error", "Please fill in all fields.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);

            insertTransaction(username, category, amount, type, date);

            updateBalance(username, amount, type);
            updateInitialBalance(username, amount, type);

            updateTableView();

        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid amount. Please enter a valid number.");
        }
    }
    private void updateInitialBalance(String username, double amount, String type) {
        double initialBalance = 0.0;

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT initial_balance FROM users WHERE username = ?")) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    initialBalance = resultSet.getDouble("initial_balance");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to fetch user's initial balance from the database.");
        }

        double newBalance = (type.equals("Income")) ? initialBalance + amount : initialBalance - amount;

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET initial_balance = ? WHERE username = ?")) {

            statement.setDouble(1, newBalance);
            statement.setString(2, username);

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to update user's initial balance in the database.");
        }
    }
    private void updateBalance(String username, double amount, String type) {
        double currentBalance = 0.0;
        String currencySymbol = "";

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT u.initial_balance, c.currency_symbol " +
                             "FROM users u " +
                             "JOIN currencies c ON u.currency_id = c.id " +
                             "WHERE u.username = ?")) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    currentBalance = resultSet.getDouble("initial_balance");
                    currencySymbol = resultSet.getString("currency_symbol");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to fetch user balance and currency symbol from the database.");
        }

        double newBalance = (type.equals("Income")) ? currentBalance + amount : currentBalance - amount;

        balanceLabel.setText("Your balance: " + newBalance + " " + currencySymbol);
    }
    private void insertTransaction(String username, String category, double amount, String type, LocalDate date) {
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO transactions (user_id, category_id, amount, type_transaction, date, currency_id) " +
                             "VALUES ((SELECT id FROM users WHERE username = ?), " +
                             "(SELECT id FROM categories WHERE category_name = ?), ?, ?, ?, " +
                             "(SELECT currency_id FROM users WHERE username = ?))")) {

            statement.setString(1, username);
            statement.setString(2, category);
            statement.setDouble(3, amount);
            statement.setString(4, type);
            statement.setDate(5, java.sql.Date.valueOf(date));
            statement.setString(6, username);

            statement.executeUpdate();

            showAlert("Success", "Transaction added successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to add transaction to the database.");
        }
    }
    private void updateTableView() {
        transactionsList.clear(); // Clear existing data

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT c.category_name, t.amount, t.type_transaction, t.date, cu.currency_symbol " +
                             "FROM transactions t " +
                             "JOIN categories c ON t.category_id = c.id " +
                             "JOIN currencies cu ON t.currency_id = cu.id " +
                             "WHERE t.user_id = (SELECT id FROM users WHERE username = ?) " +
                             "ORDER BY t.date DESC")) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String category = resultSet.getString("category_name");
                    double amount = resultSet.getDouble("amount");
                    String type = resultSet.getString("type_transaction");
                    LocalDate date = resultSet.getDate("date").toLocalDate();
                    String currency = resultSet.getString("currency_symbol");

                    transactionsList.add(new Transaction(category, amount, type, date, currency));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to fetch user transactions from the database.");
        }

        transactionsTableView.setItems(transactionsList);
    }

    private void displayUserBalance(String username) {
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT u.initial_balance, c.currency_symbol " +
                             "FROM users u " +
                             "JOIN currencies c ON u.currency_id = c.id " +
                             "WHERE u.username = ?")) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    double initialBalance = resultSet.getDouble("initial_balance");
                    String currencySymbol = resultSet.getString("currency_symbol");

                    balanceLabel.setText("Your balance: " + initialBalance + " " + currencySymbol);
                } else {
                    showAlert("Error", "Failed to fetch user balance.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to fetch user balance from the database.");
        }
    }

    @FXML
    private void logoutButtonClicked() {
        welcomeLabel.setText("Welcome");
        balanceLabel.setText("Your balance:");
        usernameField.clear();
        passwordField.clear();
        categoryChoiceBox.getSelectionModel().clearSelection(); // Clear selected category
        amountField.clear();
        typeChoiceBox.getSelectionModel().clearSelection(); // Clear selected transaction type
        dateDatePicker.setValue(null); // Clear selected date
        transactionsList.clear();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();

    }

}
