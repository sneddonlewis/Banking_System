package banking.repository;

import banking.entities.Account;
import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.Optional;

public class AccountRepository {
    private static AccountRepository instance;

    private static final String dbFileName = "card.s3db";
    private static final String URL = "jdbc:sqlite:card.s3db";

    private static final String CREATE = "CREATE TABLE IF NOT EXISTS card (\n"
            + "	id integer PRIMARY KEY,\n"
            + "	number text NOT NULL UNIQUE,\n"
            + "	pin text NOT NULL,\n"
            + " balance integer\n"
            + ");";

    private final SQLiteDataSource dataSource;


    public static AccountRepository getInstance() {
        if (instance == null) {
            synchronized (AccountRepository.class) {
                if (instance == null) {
                    instance = new AccountRepository();
                }
            }
        }
        return instance;
    }

    private AccountRepository() {
        if (instance != null) {
            throw new RuntimeException("use getInstance() method to create");
        }
        this.dataSource = new SQLiteDataSource();
        this.dataSource.setUrl("jdbc:sqlite:" + dbFileName);
        createTable();
    }

    private void createTable() {
        try (Statement statement = this.dataSource.getConnection().createStatement()) {
            statement.executeUpdate(CREATE);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public Account createAccount() {
        int rowsAffected = 0;
        String sql = "INSERT INTO card (number, pin, balance) VALUES (?, ?, ?)";
        Account generatedAccount = Account.generateNewAccount();
        while (rowsAffected == 0) {
            try (Connection connection = DriverManager.getConnection(URL)) {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    connection.setAutoCommit(false);
                    statement.setString(1, generatedAccount.getCardNumber());
                    statement.setString(2, generatedAccount.getPin());
                    statement.setInt(3, generatedAccount.getBalance());
                    rowsAffected = statement.executeUpdate();
                    if (rowsAffected == 0) {
                        generatedAccount = Account.generateNewAccount();
                    }
                    connection.commit();
                    System.out.println("\nYour card has been created");
                    generatedAccount.printDetails();
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return generatedAccount;
    }

    public Optional<Account> readOne(String cardNumberInput, String pinInput) {
        String sql = "SELECT * FROM card WHERE number = ? AND pin = ?;";

        try (Connection connection = DriverManager.getConnection(URL)) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                connection.setAutoCommit(false);
                statement.setString(1, cardNumberInput);
                statement.setString(2, pinInput);
                ResultSet resultSet = statement.executeQuery();

                String number = resultSet.getString("number");
                String pin = resultSet.getString("pin");
                int balance = resultSet.getInt("balance");

                connection.commit();
                return Optional.of(new Account(number, pin, balance));
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public boolean accountExists(String accountNumber) {
        String sql = "SELECT number FROM card WHERE number = ?";

        try (Connection connection = DriverManager.getConnection(URL)) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, accountNumber);
                ResultSet resultSet = statement.executeQuery();
                return resultSet.isBeforeFirst();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addIncome(Account account, int income) {
        String sql = "UPDATE card SET balance = balance + " + income + " WHERE number = ?;";

        try (Connection connection = DriverManager.getConnection(URL)) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, account.getCardNumber());
                System.out.println("CARD NUMBER: " + account.getCardNumber());
                int updated = statement.executeUpdate();
                System.out.println("UPDATED: " + updated);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            return false;
        }

        return true;
    }

    public int getBalanceForAccount(String accountNumber) {
        String sql = "SELECT balance FROM card WHERE number = ?;";
        try (Connection connection = DriverManager.getConnection(URL)) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, accountNumber);
                ResultSet resultSet = statement.executeQuery();
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Problem looking up balance");
    }

    public void transferMoney(Account account, String transferee, int amount) {
        String remove = "UPDATE card SET balance = balance - " + amount + " WHERE number = " + account.getCardNumber();
        String add = "UPDATE card SET balance = balance + " + amount + " WHERE number = " + transferee;

        try (Connection connection = DriverManager.getConnection(URL)) {
            connection.setAutoCommit(false);
            try (
                PreparedStatement rmStatement = connection.prepareStatement(remove);
                PreparedStatement addStatement = connection.prepareStatement(add)
            ) {
                rmStatement.executeUpdate();
                addStatement.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteOne(String cardNumber) {
        String sql = "DELETE FROM card WHERE number = ?;";
        try (Connection connection = DriverManager.getConnection(URL)) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, cardNumber);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
