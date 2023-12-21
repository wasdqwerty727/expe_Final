package com.example.expense_oop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class PieChartController {

    @FXML
    private ChoiceBox<String> userChoiceBox;

    @FXML
    private DatePicker datePicker;

    @FXML
    private PieChart incomePieChart;

    @FXML
    private PieChart expensePieChart;

    @FXML
    private void initialize() {
        // Populate the userChoiceBox with user names
        populateUserChoiceBox();
    }

    private void populateUserChoiceBox() {
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT username FROM users");
             ResultSet resultSet = statement.executeQuery()) {

            ObservableList<String> users = FXCollections.observableArrayList();
            while (resultSet.next()) {
                users.add(resultSet.getString("username"));
            }

            userChoiceBox.setItems(users);
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle the exception as needed
        }
    }

    @FXML
    private void showPieChartClicked() {
        String selectedUser = userChoiceBox.getValue();
        LocalDate selectedDate = datePicker.getValue();

        if (selectedUser != null && selectedDate != null) {
            // Fetch and display income PieChart
            fetchAndDisplayPieChart(incomePieChart, selectedUser, selectedDate, "Income");

            // Fetch and display expense PieChart
            fetchAndDisplayPieChart(expensePieChart, selectedUser, selectedDate, "Expense");
        }
    }

    private void fetchAndDisplayPieChart(PieChart pieChart, String selectedUser, LocalDate selectedDate, String transactionType) {
        pieChart.getData().clear(); // Clear existing data

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT c.category_name, SUM(t.amount) AS total_amount " +
                             "FROM transactions t " +
                             "JOIN categories c ON t.category_id = c.id " +
                             "JOIN users u ON t.user_id = u.id " +
                             "WHERE u.username = ? AND t.date = ? AND t.type_transaction = ? " +
                             "GROUP BY c.category_name")) {

            statement.setString(1, selectedUser);
            statement.setDate(2, java.sql.Date.valueOf(selectedDate));
            statement.setString(3, transactionType);

            try (ResultSet resultSet = statement.executeQuery()) {
                ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

                while (resultSet.next()) {
                    String categoryName = resultSet.getString("category_name");
                    double totalAmount = resultSet.getDouble("total_amount");

                    PieChart.Data data = new PieChart.Data(categoryName + " (" + totalAmount + ")", totalAmount);
                    pieChartData.add(data);
                }

                pieChart.setData(pieChartData);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            // Handle the exception as needed
        }
    }
}
