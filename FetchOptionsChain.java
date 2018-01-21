import fetchOptionsChain.RequestOptionChainWrapper;
import fetchOptionsChain.RequestOptionChain;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// Read in TickerList.csv, a list of contracts to generate options chains from
// TickerList.csv fields: Active (T/F) | symbol | secType | exchange | currency
// | expiry | strike | right | multiplier
// Read in PennyPilot.csv, a list of Penny Pilot tickers
// Output option chain to IBAlgoSystem.MasterChainList

public class FetchOptionsChain {
	private EClientSocket socket = null;
	public String delimiter = ",";
	public String delimiter_under = "_";
	public String csvFile = "TickerList.csv";
	public String PennyPilotFile = "PennyPilot.csv";
	public String[] TickerList;
	public int counter_iter = 0;
	public double price = -1.0;
	public Connection sqlConnection = null;
    public PreparedStatement preparedStatement = null;
    public ResultSet resultSet = null;


	public void sqlClose() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }

            if (sqlConnection != null) {
                sqlConnection.close();
            }
        } catch (Exception e) {
        }
    }

		
	public FetchOptionsChain() {
		Map<String, String> PennyPilotMap = new HashMap<>();
		boolean foundPennyTick;
		
		// load Penny Pilot Tickers
		List<String> PennyPilotTickers = new ArrayList<String>();
		StringBuilder temp_ticker = new StringBuilder("");
		try (BufferedReader br1 = new BufferedReader(new FileReader(PennyPilotFile))) {
	        String input1;
	        while ((input1 = br1.readLine()) != null) {
        		if (!input1.split(delimiter)[0].equals(temp_ticker.toString())) {
        			temp_ticker = new StringBuilder(input1.split(delimiter)[0]);
        			PennyPilotTickers.add(temp_ticker.toString());
        		}
	        }
	        br1.close();
		} catch (IOException e) {
	        e.printStackTrace();
	    }
		Collections.sort(PennyPilotTickers);
		
		
		// find number of rows in TickerList
		int rows = 0;
		try (BufferedReader br2 = new BufferedReader(new FileReader(csvFile))) {
	        String input2;
	        while ((input2 = br2.readLine()) != null) {
        		rows++;
	        }
	        br2.close();
		} catch (IOException e) {
	        e.printStackTrace();
	    }
		
		
		// write values of TickerList
		TickerList = new String[rows];
		try (BufferedReader br3 = new BufferedReader(new FileReader(csvFile))) {
			String input3;
			int row_iter = 0;
			while ((input3 = br3.readLine()) != null) {
				TickerList[row_iter] = input3;
				row_iter++;
	        }
			br3.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		// connect to sql, update counter
		try {
	        Class.forName("com.mysql.jdbc.Driver");
	        sqlConnection = DriverManager.getConnection(
	        	"jdbc:mysql://localhost/IBAlgoSystem?user=user&password=pw");
	        preparedStatement = sqlConnection.prepareStatement(
	        	"UPDATE IBAlgoSystem.counter SET counter = 0");
	        preparedStatement.executeUpdate();
	        preparedStatement = sqlConnection.prepareStatement(
	        	"TRUNCATE TABLE IBAlgoSystem.MasterChainList");
	        preparedStatement.executeUpdate();

			
			// write PennyPilotMap
			for (int i=0; i < TickerList.length; i++) {
				foundPennyTick = false;
				for (int j=0; j < PennyPilotTickers.size(); j++) {
					if (TickerList[i].split(delimiter)[1].equals(PennyPilotTickers.get(j))) {
						foundPennyTick = true;
						break;
					}
				}
				
				if (foundPennyTick) {
					PennyPilotMap.put(TickerList[i].split(delimiter)[1], "T");
				} else {
					PennyPilotMap.put(TickerList[i].split(delimiter)[1], "F");
				}
	    	}

			
			// connect to socket
	        EWrapper requestOptionChainWrapper = new RequestOptionChainWrapper(sqlConnection);
	        EClientSocket socket = new EClientSocket(requestOptionChainWrapper);
			socket.eConnect (null, 4002, 100);
			try {
				while (!(socket.isConnected()));
			} catch (Exception e) {
			}
			
			// submit a new contract for every request
	        for (int i = 0; i < rows; i++) {
	    		String line = TickerList[i];
	    		Contract cont = new Contract();
	    		cont.m_symbol = line.split(delimiter)[1];
	    		cont.m_secType = "OPT";
	    		cont.m_exchange = "SMART";
	    		cont.m_currency = line.split(delimiter)[4];
	    		cont.m_multiplier = "100";
	    		RequestOptionChain data = new RequestOptionChain(cont, socket, sqlConnection);
		    }
	        
	        
			// check counter to disconnect socket
	    	preparedStatement = sqlConnection.prepareStatement(
	    		"SELECT counter FROM IBAlgoSystem.counter;");
	    	resultSet = preparedStatement.executeQuery();
	    	counter_iter = WriteInt(resultSet, "counter");
	    	while (counter_iter < rows) {
	    		resultSet = preparedStatement.executeQuery();
		    	counter_iter = WriteInt(resultSet, "counter");
	    	}
		    socket.eDisconnect();
			
		} catch (Exception e) {
            e.printStackTrace();
        } finally {
            sqlClose();
        }
	}


    private int WriteInt(ResultSet resultSet, String column_name) throws SQLException {
        int output_counter = 0;
        while (resultSet.next()) {
        	output_counter = resultSet.getInt(column_name);
        }
        return output_counter;
    }

    
    public static void main(String args[]) {
        try {
			FetchOptionsChain runProcess = new FetchOptionsChain();
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }
}