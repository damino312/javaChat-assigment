import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.sql.*;
import java.time.LocalDateTime;

public class ChatMessageDAO {
    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/javachat";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "rootroot";
    private PrintStream os = null;


    public void saveMessage(String message, String sender, String recipient, LocalDateTime currentDate, Time currentTime) {
        try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO chat (sender, recipient, time, message, date) VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, sender);
            stmt.setString(2, recipient);
            stmt.setTime(3, currentTime);
            stmt.setString(4, message);
            stmt.setTimestamp(5, Timestamp.valueOf(currentDate));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void getHistory(Socket clientSocket, String clientName) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD)) {
             Statement stmt = conn.createStatement();
             String sql = "SELECT sender, recipient, time, message, date FROM chat";
             ResultSet rs = stmt.executeQuery(sql);

             os = new PrintStream(clientSocket.getOutputStream());

             while(rs.next()) {
                 String sender = rs.getString("sender");
                 String recipient = rs.getString("recipient");
                 String time = String.valueOf(rs.getTime("time"));
                 String message = rs.getString("message");
                 String date = rs.getString("date");
                 if (recipient != null ) {
                     if (recipient.equals(clientName)) {
                         os.println(time +" "+ date + "Private message:"+ " >" + sender + "> " + message);

                     } else {
                         continue;
                     }
                 } else {
                     os.println(time +" "+ date + " <" + sender + "> " + message);
                 }
             }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
