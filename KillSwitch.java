import tradeStrategy.TradeStrategyWrapper;

import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


// kills all active orders

public class KillSwitch {
    public Connection sqlConnection = null;
    public PreparedStatement preparedStatement = null;
    public ResultSet resultSet = null;
    
    
    public KillSwitch () {

        // connect to SQL
        try {
            Class.forName("com.mysql.jdbc.Driver");
            sqlConnection = DriverManager.getConnection(
                "jdbc:mysql://localhost/IBAlgoSystem?user=user&password=pw");


            // connect to socket
            EWrapper tradeStrategyWrapper = new TradeStrategyWrapper(sqlConnection);
            EClientSocket socket = new EClientSocket(tradeStrategyWrapper);
            socket.eConnect("", 4002, 1000);
            try {
                while (!(socket.isConnected()));
            } catch (Exception e) {
            }


            // find outstanding active orders
            preparedStatement = sqlConnection.prepareStatement(
                "SELECT orderID FROM IBAlgoSystem.orderTracking WHERE "
                + "status <> 'Filled' AND status <> 'Cancelled' AND "
                + "status <> 'ApiCancelled' AND status <> 'Inactive';");
            resultSet = preparedStatement.executeQuery();

            // submit cancel request
            while (resultSet.next()) {
                int orderId = resultSet.getInt("orderID");
                socket.cancelOrder(orderId);
            }

            // wait for all orders to cancel
            boolean ContinueProcess = false;
            preparedStatement = sqlConnection.prepareStatement(
                "SELECT COUNT(*) FROM IBAlgoSystem.orderTracking WHERE "
                + "status <> 'Filled' AND status <> 'Cancelled' AND "
                + "status <> 'ApiCancelled' AND status <> 'Inactive';");
            while (!ContinueProcess) {
                resultSet = preparedStatement.executeQuery();
                if (resultSet.getInt("COUNT(*)") == 0) {
                    ContinueProcess = true;
                }
            }
            
            socket.eDisconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public static void main (String args[]) {
        KillSwitch runProcess = new KillSwitch();
    }
}