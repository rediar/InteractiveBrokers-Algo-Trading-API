import simplePriceFeed.RequestPriceWrapper;
import simplePriceFeed.RequestPriceData;
import simplePriceFeed.WritePriceData;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;


// Read in TickerList.csv - a list of contracts to receive live price streams from
// TickerList.csv fields: Active (T/F) | symbol | secType | exchange | currency
// | expiry | strike | right | multiplier
// Output prices to IBAlgoSystem.price

public class SimplePriceFeed {
    public Connection sqlConnection = null;
    public PreparedStatement preparedStatement = null;


    public SimplePriceFeed() throws Exception {
        // read in TickerList.csv
        String csvFile = "TickerList.csv";
        String delimiter = ",";
        
        // find number of rows in TickerList
        int rows = 0;
        try (BufferedReader br1 = new BufferedReader(new FileReader(csvFile))) {
            String input1;
            while ((input1 = br1.readLine()) != null) {
                if (input1.split(delimiter)[0].equals("T")) {
                    rows++;
                }
            }
            br1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        

        // connect to IBAlgoSystem.price, clear previous data
        Class.forName("com.mysql.jdbc.Driver");
        sqlConnection = DriverManager.getConnection(
            "jdbc:mysql://localhost/IBAlgoSystem?user=user&password=pw");
        // this line assumes TickerList.csv only contains stocks (secType = STK)
        preparedStatement = sqlConnection.prepareStatement(
            "DELETE FROM IBAlgoSystem.price WHERE secType = 'STK'");
        preparedStatement.executeUpdate();


        // write dummy values of TickerList to IBAlgoSystem.price
        String[] TickerLines = new String[rows];
        try (BufferedReader br2 = new BufferedReader(new FileReader(csvFile))) {
            String input2;
            int row_iter = 0;
            while ((input2 = br2.readLine()) != null) {
                if (input2.split(delimiter)[0].equals("T")) {
                    if (input2.split(delimiter)[2].equals("STK")) {
                        preparedStatement = sqlConnection.prepareStatement(
                            "INSERT INTO IBAlgoSystem.price (entry, symbol, "
                            + "secType, currency, bid, ask, last, close, "
                            + "bugCounter, updateTime) VALUES (default,'"
                            + input2.split(delimiter)[1] + "','" 
                            + input2.split(delimiter)[2] + "','" 
                            + input2.split(delimiter)[4] 
                            + "', -1.0, -1.0, -1.0, -1.0, 0, 0)");
                    } else if (input2.split(delimiter)[2].equals("OPT")) {
                        preparedStatement = sqlConnection.prepareStatement(
                            "DELETE FROM IBAlgoSystem.price WHERE symbol = '" 
                            + input2.split(delimiter)[1] + 
                            "' and secType = 'OPT' and currency = '" 
                            + input2.split(delimiter)[4] + "' and expiry = '" 
                            + input2.split(delimiter)[5] + "' and strike = " 
                            + input2.split(delimiter)[6] + " and callorput = '" 
                            + input2.split(delimiter)[7] 
                            + "' and multiplier = '"
                            + input2.split(delimiter)[8] + "';");
                        preparedStatement.executeUpdate();

                        preparedStatement = sqlConnection.prepareStatement(
                            "INSERT INTO IBAlgoSystem.price (entry, symbol, "
                            + "secType, currency, expiry, strike, callorput, "
                            + "multiplier, bid, ask, last, close, bugCounter, "
                            + "updateTime) VALUES (default,'" + 
                            input2.split(delimiter)[1] + "','" 
                            + input2.split(delimiter)[2] + "','" 
                            + input2.split(delimiter)[4] + "','" 
                            + input2.split(delimiter)[5] + "'," 
                            + input2.split(delimiter)[6] +",'" 
                            + input2.split(delimiter)[7] + "','" 
                            + input2.split(delimiter)[8] 
                            + "', -1.0, -1.0, -1.0, -1.0, 0, 0)");
                    }
                    preparedStatement.executeUpdate();

                    TickerLines[row_iter] = input2;
                    row_iter++;
                }
            }
            br2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // connect to IB socket
        EWrapper requestPriceWrapper = new RequestPriceWrapper();
        EClientSocket socket = new EClientSocket(requestPriceWrapper);
        socket.eConnect("", 4002, 0);
        try {
            while (!(socket.isConnected()));
        } catch (Exception e) {
        }

        // request live data setting
        socket.reqMarketDataType(1);
        
        // submit a new contract for every request
        for (int i = 0; i < rows; i++) {
            String line = TickerLines[i];
            Contract cont = new Contract();
            cont.m_symbol = line.split(delimiter)[1];
            cont.m_secType = line.split(delimiter)[2];
            cont.m_exchange = line.split(delimiter)[3];
            cont.m_currency = line.split(delimiter)[4];
            if (cont.m_secType.equals("OPT")) {
                cont.m_expiry = line.split(delimiter)[5];
                cont.m_strike = Double.parseDouble(line.split(delimiter)[6]);
                cont.m_right = line.split(delimiter)[7];
                cont.m_multiplier = line.split(delimiter)[8];
            }
            RequestPriceData data = new RequestPriceData(cont, socket,
                sqlConnection);
        }
    }

    public static void main(String[] args) {
        try {
            SimplePriceFeed runProcess = new SimplePriceFeed();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}