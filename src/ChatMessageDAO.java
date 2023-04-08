import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;

public class ChatMessageDAO {
    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/javachat";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "rootroot";
    private PrintStream os = null;
    private static AES aes = new AES();


    public void saveMessage(String message, String sender, String recipient, LocalDateTime currentDate, Time currentTime) {
        try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO chat (sender, recipient, time, message, date) VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, sender);
            stmt.setString(2, recipient);
            stmt.setTime(3, currentTime);
            String encryptedMessage = aes.encrypt(message);
            byte[] encryptedMessageBytes = encryptedMessage.getBytes(StandardCharsets.UTF_8);

            stmt.setBytes(4, encryptedMessageBytes);
            stmt.setTimestamp(5, Timestamp.valueOf(currentDate));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void getHistory(Socket clientSocket, String clientName) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD)) {
            Statement stmt = conn.createStatement();
            String sql = "SELECT sender, recipient, time, message, date FROM chat";
            ResultSet rs = stmt.executeQuery(sql);

            os = new PrintStream(clientSocket.getOutputStream());

            while (rs.next()) {
                String sender = rs.getString("sender");
                String recipient = rs.getString("recipient");
                String time = String.valueOf(rs.getTime("time"));
                String message = aes.decrypt(rs.getString("message"));
                String date = rs.getString("date");
                if (recipient != null) {
                    if (recipient.equals(clientName)) {
                        os.println(time + " " + date + " Private message:" + " >" + sender + "> " + message);
                    } else if (sender.equals(clientName)) {
                        os.println(time + " " + date + " Private message to " +recipient +": " + message);
                    } else {
                        continue;
                    }
                } else {
                    os.println(time + " " + date + " <" + sender + "> " + message);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void downloadHistory(Socket clientSocket, String clientName) { // write into a file
        try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD)) {
            Statement stmt = conn.createStatement();
            String sql = "SELECT sender, recipient, time, message, date FROM chat";
            ResultSet rs = stmt.executeQuery(sql);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("history.txt"))) {
                while (rs.next()) {
                    String sender = rs.getString("sender");
                    String recipient = rs.getString("recipient");
                    String time = String.valueOf(rs.getTime("time"));
                    String message = aes.decrypt(rs.getString("message"));
                    String date = rs.getString("date");
                    if (recipient != null) {
                        if (recipient.equals(clientName)) {
                            writer.write(time + " " + date + " Private message:" + " >" + sender + "> " + message);
                            writer.newLine();
                        } else if (sender.equals(clientName)) {
                            writer.write(time + " " + date + " Private message to " +recipient +": " + message);
                            writer.newLine();
                        } else {
                            continue;
                        }
                    } else {
                        writer.write(time + " " + date + " <" + sender + "> " + message);
                        writer.newLine();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

