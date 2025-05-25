import java.sql.*;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Timestamp;

public class CRUD{

    private final String URL;
    private final String userPass;
    private final String userName;
    private Connection connection;

    CRUD(String userName, String userPass){
        this.userName = userName;
        this.userPass = userPass;
        this.URL = "jdbc:mysql://localhost:3306/mathematica";

        try{
            this.connection = DriverManager.getConnection(URL, userName, userPass);
            System.out.println("Mathematica::Connectetd");
        } catch(SQLException except){
            System.out.println(except.getMessage());
        }
    }
   
   public boolean isConnected(){
        try{
            return connection!=null && connection.isValid(2);
        } catch(SQLException except){
            System.out.println(except.getMessage());
            return false;
        }
    }

    public void CreateData(String ImagePath, String Question, boolean Graph_Plotted){
        if(isConnected()){
            try{
                String SQL = "INSERT INTO HISTORY (FilePath, Question, Graph_Plotted) VALUES(?, ?, ?)";
                PreparedStatement statement = connection.prepareStatement(SQL);

                statement.setString(1, ImagePath);
                statement.setString(2, Question);
                statement.setBoolean(3, Graph_Plotted);

                int rowsCreated = statement.executeUpdate();

                if(rowsCreated > 0){
                    System.out.println("CreateData::PASS");
                } else{
                    System.out.println("CreateData::FAIL");
                }
            } catch(SQLException except){
                System.out.println(except.getMessage());
            }
        }
    }

public DefaultTableModel getHistoryTableModel() {
        Vector<String> columnNames = new Vector<>();
        Vector<Vector<Object>> data = new Vector<>();

        if (!isConnected()) {
            System.err.println("Mathematica::Not-Connected");
            // Return an empty model if not connected, so the UI doesn't crash
            return new DefaultTableModel(data, columnNames); 
        }

        String SQL = "SELECT FilePath, Question, Graph_Plotted, Created FROM HISTORY ORDER BY Created";

        try (Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery(SQL)) {

            ResultSetMetaData rsmd = resultSet.getMetaData();
            int columnsNumber = rsmd.getColumnCount();

            for (int i = 1; i <= columnsNumber; i++) {
                columnNames.add(rsmd.getColumnName(i));
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            while (resultSet.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= columnsNumber; i++) {
                    Object value = resultSet.getObject(i);
                    if (value instanceof Timestamp) {
                        // Format the timestamp for better display
                        row.add(((Timestamp) value).toLocalDateTime().format(formatter));
                    } else {
                        row.add(value);
                    }
                }
                data.add(row);
            }

        } catch (SQLException except) {
            System.err.println(except.getMessage());
        }
        return new DefaultTableModel(data, columnNames);
    }

    public int Delete(int days){
        if(!isConnected()){
            System.out.println("Mathmatica::Not-Connected");
        }
        
        LocalDateTime fifteenDaysAgo = LocalDateTime.now().minusDays(days);
        Timestamp timestampLimit = Timestamp.valueOf(fifteenDaysAgo);
        try{
            String SQL = "DELETE * FROM HISTORY WHERE Created < ?";
            PreparedStatement statement = connection.prepareStatement(SQL);
            statement.setTimestamp(1, timestampLimit);
            int rowsDeleted = statement.executeUpdate();
            return rowsDeleted;

        } catch(SQLException except){
            System.out.println(except.getMessage());
            return 0;
        }
    }
}