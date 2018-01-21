import tradeStrategy.FetchOrderStatus;
import tradeStrategy.TradeStrategyWrapper;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.TagValue;
import com.ib.client.CommissionReport;
import com.ib.client.UnderComp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


// template to implement trade strategies

public class TradeStrategy {
	public Connection sqlConnection = null;
    public PreparedStatement preparedStatement = null;
    public ResultSet resultSet = null;
	

	public TradeStrategy () {
		
		// connect to SQL
        try {
            Class.forName("com.mysql.jdbc.Driver");
            sqlConnection = DriverManager.getConnection(
            	"jdbc:mysql://localhost/IBAlgoSystem?user=user&password=pw");


	    	// connect to socket
		    EWrapper tradeStrategyWrapper = new TradeStrategyWrapper();
		    EClientSocket socket = new EClientSocket(tradeStrategyWrapper);
		    socket.eConnect("", 7496, 1000);
			try {
				while (!(socket.isConnected()));
			} catch (Exception e) {
			}


			// load account summary
			socket.reqAccountSummary(7496, "All", "NetLiquidation,"
				+ "TotalCashValue, SettledCash, AccruedCash, BuyingPower,"
				+ "EquityWithLoanValue, PreviousEquityWithLoanValue,"
				+ "GrossPositionValue, ReqTEquity, ReqTMargin, SMA, "
				+ "InitMarginReq, MaintMarginReq, AvailableFunds,"
				+ "ExcessLiquidity, Cushion, FullInitMarginReq,"
				+ "FullMaintMarginReq, FullAvailableFunds, FullExcessLiquidity,"
				+ "LookAheadNextChange, LookAheadInitMarginReq,"
				+ "LookAheadMaintMarginReq, LookAheadAvailableFunds,"
				+ "LookAheadExcessLiquidity, HighestSeverity, Leverage");
		    

		    // Here, call core functions to fetch prices, submit orders, manage 
		    // margin limit, etc.
    		
    	} catch (Exception e) {
            e.printStackTrace();
        }
	}
		
    
	public static void main (String args[]) {
		TradeStrategy runProcess = new TradeStrategy();
	}
}