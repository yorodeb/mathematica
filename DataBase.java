import java.sql.*;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Timestamp;

/*CRUD OPERATION AND METHODS:
 * Create --> CreateData()
 * Read --> getHistoryTableModel()
 * Delete --> Delete()*/

public class DataBase{
    private final String URL;  /*@param for method DriverManager.getConnection() --> URL, userPass, userName*/
    private final String userPass;
    private final String userName;
    private Connection connection; /*Object -> 'connection' of Class -> 'Connection'*/

		/*DataBase() --> Constructor for Connecting to SQL Database*/
    DataBase(String userName, String userPass){
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

	 /*isConnected() --> Validating Connection -- returns <bool>*/
   pubic boolean isConnected(){
        try{
            return connection!=null && connection.isValid(2);
        } catch(SQLException except){
            System.out.println(except.getMessage());
            return false;
        }
    }

	 /*CreateData() --> Method for 'INSERT' statements
		* -- returns <void>*/
    public void CreateData(String ImagePath, String Question){
        if(isConnected()){
            try{
                String SQL = "INSERT INTO HISTORY (FilePath, Question) VALUES(?, ?)";
                PreparedStatement statement = connection.prepareStatement(SQL);

                statement.setString(1, ImagePath); //assining @param ImagePath to Values.
                statement.setString(2, Question); //assining @param Question to Values.

                int rowsCreated = statement.executeUpdate();

								/*this statement validates the changes in SQL table 'HISTORY'*/
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

/*getHistoryTableModel() --> Method for getting Vector based table in GUI (in Windows.java)
 * -- returns <DefaultTableModel>*/
public DefaultTableModel getHistoryTableModel() {
        Vector<String> columnNames = new Vector<>();
        Vector<Vector<Object>> data = new Vector<>();

				//Validating Connection with 'Mathematica'
        if (!isConnected()) {
            System.err.println("Mathematica::Not-Connected");
            return new DefaultTableModel(data, columnNames); 
        }

        String SQL = "SELECT FilePath, Question, Created FROM HISTORY ORDER BY Created";

        try (Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery(SQL)) {

            ResultSetMetaData rsmd = resultSet.getMetaData(); //Reading MetaData
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

		/*Delete() --> Method for 'DELETE' statement
		 * -- return <int> >> 'Number Of Rows Deleted'
		 * -- delete data from 'HISTORY' which is more than 15 days old.*/
    public int Delete(int days){
				
				//Validating Connection with 'Mathematica'
        if(!isConnected()){
            System.out.println("Mathmatica::Not-Connected");
        }
        
        LocalDateTime fifteenDaysAgo = LocalDateTime.now().minusDays(days); //<CurrentTime> - <@param Days>
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
