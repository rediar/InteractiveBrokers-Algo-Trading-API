import largePriceFeed.RequestPriceData;
import largePriceFeed.RequestPriceWrapper;
import largePriceFeed.WritePriceData;

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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


// Read in IBAlgoSystem.MasterChainList - a list of options contracts to 
// receive live price streams from
// The number of entries may exceed ib's feed limit (FEED_LIMIT)
// This script divides the entries into smaller batches to submit to ib
// Output prices to IBAlgoSystem.price

public class LargePriceFeed {
	private EClientSocket socket = null;
	public String delimiter = ",";
	public String delimiter_under = "_";
	public String TickerListCSVFile = "TickerList.csv";
	public String[] TickerLines;
	public Map<Integer, Boolean> FileUpdated = new HashMap<Integer, Boolean>();
	public long updateTime, dt_now;
	public boolean AllUpdated, running_updated;
	public int FEED_LIMIT = 100;
	public Connection sqlConnection = null;
    public PreparedStatement preparedStatement = null;
    public ResultSet resultSet = null;
	

	public LargePriceFeed () {
		int NUM_FEEDS = 0;

		// find NUM_FEEDS from 100 - rows of TickerList
		try (BufferedReader br3 = new BufferedReader(new FileReader(TickerListCSVFile))) {
	        String input3;
	        while((input3 = br3.readLine()) != null) {
	        	if (!input3.split(delimiter)[0].equals("active")) {
	        		NUM_FEEDS++;
	        	}
	        }
	        br3.close();
		} catch (IOException e) {
	        e.printStackTrace();
	    }
		
		// make sure NUM_FEEDS is sufficient
		if (NUM_FEEDS >= (FEED_LIMIT - 5)) {
			System.out.println("Not enough sockets for feed!");
			
		} else {
			NUM_FEEDS = FEED_LIMIT - 5 - NUM_FEEDS;

			// connect to SQL
			try {
		        Class.forName("com.mysql.jdbc.Driver");
		        sqlConnection = DriverManager.getConnection(
		        	"jdbc:mysql://localhost/IBAlgoSystem?user=user&password=pw");

				// find number of rows in MasterChainList
				preparedStatement = sqlConnection.prepareStatement(
					"SELECT COUNT(*) FROM IBAlgoSystem.MasterChainList;");
				resultSet = preparedStatement.executeQuery();
				int rows = resultSet.getInt("COUNT(*)");

				
				// write values of MasterChainList to TickerLines, dummy price
				// TickerLines format: active, symbol, secType, exchange, 
				// currency, expiry, strike, right, multiplier, pennyPilot, 
				// moneyness
				TickerLines = new String[rows];
				preparedStatement = sqlConnection.prepareStatement(
					"SELECT * FROM IBAlgoSystem.MasterChainList");
				resultSet = preparedStatement.executeQuery();

				int row_iter = 0;
				while (resultSet.next()) {
					String symbol = resultSet.getString("symbol");
					String exchange = resultSet.getString("exchange");
					String currency = resultSet.getString("currency");
					String expiry = resultSet.getString("expiry");
					double strike = resultSet.getDouble("strike");
					String right = resultSet.getString("callorput");
					String multiplier = resultSet.getString("multiplier");
					String pennyPilot = resultSet.getString("pennyPilot");
					String moneyness = resultSet.getString("moneyness");
					double bid = -1.0;
					double ask = -1.0;

					// delete previous entry
					preparedStatement = sqlConnection.prepareStatement(
						"DELETE FROM IBAlgoSystem.price WHERE symbol = '" 
						+ symbol + "' and secType = 'OPT' and currency = '" 
						+ currency + "' and expiry = '" + expiry + 
						"' and strike = " + Double.toString(strike) + 
						" and callorput = '" + right + "' and multiplier = '" 
						+ multiplier + "';");
                    preparedStatement.executeUpdate();

                    // write new entry
					preparedStatement = sqlConnection.prepareStatement(
						"INSERT INTO IBAlgoSystem.price (entry, symbol, "
						+ "secType, currency, expiry, strike, callorput, "
						+ "multiplier, bid, ask, last, close, bugCounter, "
						+ "updateTime) VALUES (default,'" + symbol + 
						"', 'OPT', '" + currency + "', '" + expiry + "', " 
						+ Double.toString(strike) + ", '" + right + "', '" 
						+ multiplier + "', 0.0, 0.01, -1.0, -1.0, 0, 0);");
					preparedStatement.executeUpdate();
				}
		        

		        // divide the list of names into batches of NUM_FEEDS
		        int num_batches = rows/NUM_FEEDS + 1;
		
		        
				// connect to socket
		        EWrapper requestPriceWrapper = new RequestPriceWrapper();
		        EClientSocket socket = new EClientSocket(requestPriceWrapper);
		        

		        // update prices by batch        
		        
				// connect to socket
		        socket.eConnect(null, 4002, 101);
	    		try {
	    			while (!(socket.isConnected()));
	    		} catch (Exception e) {
	    		}
		    	
		    	// add while loop to make perpeptual
		        while (true) {
			        for (int i=0; i < num_batches; i++) {
			    		
			    		// send price feed requests
		    			for (int j=0; j < NUM_FEEDS; j++) {
		    				if ((i*NUM_FEEDS + j) < rows) {
		    					// submit a new contract for every request
			    				String line = TickerLines[i*NUM_FEEDS + j];
			    	    		Contract cont = new Contract();
			    	    		cont.m_symbol = line.split(delimiter_under)[1];
			    	    		cont.m_secType = line.split(delimiter_under)[2];
			    	    		// cont.m_exchange = line.split(delimiter)[3];
			    	    		cont.m_exchange = "SMART";
			    	    		cont.m_currency = line.split(delimiter_under)[4];
			    	    		cont.m_expiry = line.split(delimiter_under)[5];
			    	    		cont.m_strike = Double.parseDouble(line.split(delimiter_under)[6]);
			    	    		cont.m_right = line.split(delimiter_under)[7];
			    	    		cont.m_multiplier = line.split(delimiter_under)[8];
			    	    		
			    	    		FileUpdated.put(i*NUM_FEEDS + j, false);
			    	    		
			    	    		RequestPriceData data = new RequestPriceData(cont, 
			    	    			true, socket, sqlConnection);
		    				} else {
		    					FileUpdated.put(i*NUM_FEEDS + j, true);
		    				}
		    			}
				    	
			    			
				    	// check price entry is updated to continue
			    		AllUpdated = false;
			    		while (!AllUpdated) {
				    		for (int j=0; j < NUM_FEEDS; j++) {
				    			if (!FileUpdated.get(i*NUM_FEEDS + j)) {
				    				String line = TickerLines[i*NUM_FEEDS + j];
				    				String symbol = line.split(delimiter_under)[1];
									String exchange = line.split(delimiter_under)[3];
									String currency = line.split(delimiter_under)[4];
									String expiry = line.split(delimiter_under)[5];
									double strike = Double.parseDouble(line.split(delimiter_under)[6]);
									String right = line.split(delimiter_under)[7];
									String multiplier = line.split(delimiter_under)[8];

									preparedStatement = sqlConnection.prepareStatement(
										"SELECT updateTime FROM IBAlgoSystem.price WHERE symbol = '"
										+ symbol + "' and secType = 'OPT' and currency = '"
										+ currency + "' and expiry = '" + expiry
										+ "' and strike = " + Double.toString(strike)
										+ " and callorput = '" + right
										+ "' and multiplier = '" + multiplier + "';");
									resultSet = preparedStatement.executeQuery();
									while (resultSet.next()) {
										updateTime = resultSet.getLong("updateTime");
									}

									// check the last (last updated field) is actually updated, and within 5*NUM_FEEDS secs
									if ((fetchPrice.FetchSTKPrice(symbol, "USD", sqlConnection)[2] > -0.01) && 
										(((new Date()).getTime() - updateTime) < 5*NUM_FEEDS*1000)) {
										FileUpdated.put(i*NUM_FEEDS + j, true);
									}
				    			}
				    		}
				    		
				    		for (int j=0; j < NUM_FEEDS; j++) {
				    			running_updated = true;
				    			if (!FileUpdated.get(i*NUM_FEEDS + j)) {
				    				running_updated = false;
				    				break;
				    			}
				    		}
				    		if (running_updated) {
				    			AllUpdated = true;
				    		}
		    			}
				    	
			    		// pause for 1 sec between each batch
			    		dt_now = (new Date()).getTime();
			    		while (((new Date()).getTime() - dt_now) < (1*1000));
				    }
			        
					// pause for 1 min between each loop
			        if (num_batches < 60) {
						dt_now = (new Date()).getTime();
						while (((new Date()).getTime() - dt_now) < (60*1000));
			        }
		        }
		    } catch (Exception e) {
	            e.printStackTrace();
        	}
		}
	}

    
    public static void main (String args[]) {
        try {
        	LargePriceFeed runProcess = new LargePriceFeed();
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }
}