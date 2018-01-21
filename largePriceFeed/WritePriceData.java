package largePriceFeed;

import com.ib.client.EClientSocket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Date;


public class WritePriceData {
    public static final int NULL=0, LOOK=1<<0, LONG=1<<1, SHORT=1<<2, WAIT_FILL=1<<3, WAIT_CANCEL=1<<4;
    public int sysState = NULL;
    private final RequestPriceData data;
    private final EClientSocket socket;
    private final Connection sqlConnection;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;
    private double prevLastPrice;
    private int bugCounter;
    
    private static int nextOrderId = 1;

    WritePriceData(RequestPriceData data, EClientSocket socket, 
        Connection sqlConnection) {
        this.data = data;
        this.socket = socket;
        this.sqlConnection = sqlConnection;
        sysState = LOOK;
    }

    void check() {
        try {
            if (data.cont.m_secType.equals("STK")) {
                // check that lastPrice isn't a bug
                statement = sqlConnection.createStatement();
                resultSet = statement.executeQuery(
                    "SELECT last FROM IBAlgoSystem.price WHERE symbol='"
                    + data.cont.m_symbol + "' AND secType='STK';");
                prevLastPrice = outputLastPrice(resultSet);
                resultSet = statement.executeQuery(
                    "SELECT bugCounter FROM IBAlgoSystem.price WHERE symbol='" 
                    + data.cont.m_symbol + "' AND secType='STK';");
                bugCounter = outputBugCounter(resultSet);


                if ((prevLastPrice > 0.0) && 
                    (Math.abs(data.lastPrice/prevLastPrice - 1) > 0.1) && 
                    (bugCounter < 3)) {
                    bugCounter++;
                    preparedStatement = sqlConnection.prepareStatement(
                        "UPDATE IBAlgoSystem.price SET bugCounter=" 
                        + Integer.toString(bugCounter) + ";");
                } else {
                    preparedStatement = sqlConnection.prepareStatement(
                        "UPDATE IBAlgoSystem.price SET bid =" 
                        + Double.toString(data.bidPrice) + ", ask =" 
                        + Double.toString(data.askPrice) + ", last =" 
                        + Double.toString(data.lastPrice) + ", close = " 
                        + Double.toString(data.closePrice) + 
                        ", bugCounter = 0, updateTime = " 
                        + Long.toString((new Date()).getTime()) 
                        + " WHERE symbol = '" + data.cont.m_symbol + 
                        "' AND secType = 'STK' AND currency = '" 
                        + data.cont.m_currency + "';");
                }
            
            } else if (data.cont.m_secType.equals("OPT")) {
                preparedStatement = sqlConnection.prepareStatement(
                    "UPDATE IBAlgoSystem.price SET bid =" 
                    + Double.toString(data.bidPrice) + ", ask =" + 
                    Double.toString(data.askPrice) + ", last =" 
                    + Double.toString(data.lastPrice) + ", close = " 
                    + Double.toString(data.closePrice) + 
                    ", updateTime = " + Long.toString((new Date()).getTime()) 
                    + " WHERE symbol = '" + data.cont.m_symbol + 
                    "' AND secType = 'OPT' AND currency = '" 
                    + data.cont.m_currency + "' AND expiry = '" 
                    + data.cont.m_expiry + "' AND strike = " + 
                    Double.toString(data.cont.m_strike) + " AND callorput = '" 
                    + data.cont.m_right + "' AND multiplier = '" 
                    + data.cont.m_multiplier + "';");
            
            } else if (data.cont.m_secType.equals("CASH")) {
                preparedStatement = sqlConnection.prepareStatement(
                    "UPDATE IBAlgoSystem.price SET bid =" 
                    + Double.toString(data.bidPrice) + ", ask =" + 
                    Double.toString(data.askPrice) + ", last =" 
                    + Double.toString(data.lastPrice) + ", close =" 
                    + Double.toString(data.closePrice) + ", updateTime = " 
                    + Long.toString((new Date()).getTime()) 
                    + " WHERE symbol = '" + data.cont.m_symbol 
                    + "' AND secType = 'CASH' AND currency = '" 
                    + data.cont.m_currency + "';");
            }
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
    public double outputLastPrice(ResultSet resultSet) throws SQLException {
        Double lastPrice = -1.0;
        while (resultSet.next()) {
            lastPrice = resultSet.getDouble("last");
        }
        return lastPrice;
    }

    public int outputBugCounter(ResultSet resultSet) throws SQLException {
        int bugCounter = 0;
        while (resultSet.next()) {
            bugCounter = resultSet.getInt("bugCounter");
        }
        return bugCounter;
    }
}