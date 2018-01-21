package fetchOptionsChain;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import java.sql.Connection;


public class RequestOptionChain {
            final Contract cont;
    private final EClientSocket socket;
    private final Connection sqlConnection;

    private static int nextId = 1;
    private final int myId;

    public RequestOptionChain(Contract cont, EClientSocket socket, 
        Connection sqlConnection) {
    	this.cont = cont;
        this.socket = socket;
        this.sqlConnection = sqlConnection;
        myId = nextId++;
        socket.requestOptionChainWrapper();
        reqOptionData();
    }
    
    private void reqOptionData() {
        socket.reqContractDetails(myId, cont);
    }
}