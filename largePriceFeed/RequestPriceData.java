package largePriceFeed;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;


public class RequestPriceData {
            final Contract cont;
            final boolean snapshot;
    private final EClientSocket socket;
    private final WritePriceData writePriceData;
    private final Connection sqlConnection;

    private static int nextId = 1;
    private final int myId;

    List<Double> bidPrices = new ArrayList<>();
    List<Double> askPrices = new ArrayList<>();
    List<Double> lastPrices = new ArrayList<>();
    List<Double> closePrices = new ArrayList<>();
    double bidPrice = -1.0;
    double askPrice = -1.0;
    double lastPrice = -1.0;
    double closePrice = -1.0;

    public RequestPriceData(Contract cont, boolean snapshot, 
        EClientSocket socket, Connection sqlConnection) {
        this.cont = cont;
        this.snapshot = snapshot;
        this.socket = socket;
        this.sqlConnection = sqlConnection;
        writePriceData = new WritePriceData(this, socket, sqlConnection);
        myId = nextId++;
        ((RequestPriceWrapper) socket.requestPriceWrapper()).dataMap.put(myId, this);
        reqData();
    }

    private void reqData() {
        socket.reqMktData(myId, cont, "", snapshot, null);
    }
    
    // record bid price
    public void dataRecdBid(double inputPrice) {
        bidPrice = inputPrice;
        bidPrices.add(inputPrice);
        writePriceData.check();
    }

    // record ask price
    public void dataRecdAsk(double inputPrice) {
        askPrice = inputPrice;
        askPrices.add(inputPrice);
        writePriceData.check();
    }

    // record last price
    public void dataRecdLast(double inputPrice) {
        lastPrice = inputPrice;
        lastPrices.add(inputPrice);
        writePriceData.check();
    }

    // record close price
    public void dataRecdClose(double inputPrice) {
        closePrice = inputPrice;
        closePrices.add(inputPrice);
        writePriceData.check();
    }
}