package tradeStrategy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class FetchOrderStatus {
	public int num_fields = 4;
	
    private final Connection sqlConnection;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;


	public String[] ReadOrderStatus(int orderID, Connection sqlConnection) {
        String[] order_stats = new String[num_fields];
        StringBuilder check_order_stats; 

        boolean checks_out = false;
        while (!(checks_out)) {
            try {
                preparedStatement = sqlConnection.prepareStatement(
                    "SELECT status FROM IBAlgoSystem.orderTracking WHERE orderID = " 
                    + Integer.toString(orderId) + ";");
                resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    check_order_stats = new StringBuilder(resultSet.getString(
                        "status"));
                }

                if ((check_order_stats.toString().equals("PendingSubmit")) || 
                    (check_order_stats.toString().equals("PendingCancel")) || 
                    (check_order_stats.toString().equals("PreSubmitted")) || 
                    (check_order_stats.toString().equals("ApiCancelled")) || 
                    (check_order_stats.toString().equals("Cancelled")) || 
                    (check_order_stats.toString().equals("Filled")) || 
                    (check_order_stats.toString().equals("Inactive"))) {
                    
                    order_stats[0] = check_order_stats.toString();
                    
                    preparedStatement = sqlConnection.prepareStatement(
                        "SELECT filled FROM IBAlgoSystem.orderTracking WHERE "
                        + "orderID = " + Integer.toString(orderId) + ";");
                    resultSet = preparedStatement.executeQuery();
                    order_stats[1] = Integer.toString(outputSQLInt(resultSet, 
                        "filled"));
                    
                    preparedStatement = sqlConnection.prepareStatement(
                        "SELECT remaining FROM IBAlgoSystem.orderTracking WHERE "
                        + "orderID = " + Integer.toString(orderId) + ";");
                    resultSet = preparedStatement.executeQuery();
                    order_stats[2] = Integer.toString(outputSQLInt(resultSet, 
                        "remaining"));
                    
                    preparedStatement = sqlConnection.prepareStatement(
                        "SELECT avgFillPrice FROM IBAlgoSystem.orderTracking "
                        + "WHERE orderID = " + Integer.toString(orderId) + ";");
                    resultSet = preparedStatement.executeQuery();
                    order_stats[3] = Double.toString(outputSQLDouble(resultSet, 
                        "avgFillPrice"));

                    checks_out = true;
                }
            } catch (Exception e) {
            }
        }
		return order_stats;
	}
	

	public void WriteOrderStatus(int orderID, String status, int filled, 
        int remaining, double avgFillPrice, Connection sqlConnection) {
		try {
            preparedStatement = sqlConnection.prepareStatement(
                "UPDATE IBAlgoSystem.orderTracking SET orderID = " 
                + Integer.toString(orderID) + ", status = '" + status 
                + "', filled = " + Integer.toString(filled) + ", remaining = " 
                + Integer.toString(remaining) + ", avgFillPrice = "
                + Double.toString(avgFillPrice) + " WHERE orderID = " 
                + Integer.toString(orderID) + ";");
            preparedStatement.executeUpdate();
        } catch (Exception e) {
			e.printStackTrace();
		}
	}
	

    public boolean OrderRecorded(int orderID, Connection sqlConnection) {
        boolean order_recorded = false;
        try {
            preparedStatement = sqlConnection.prepareStatement(
                "SELECT COUNT(*) FROM IBAlgoSystem.orderTracking WHERE orderID = " 
                + Integer.toString(orderID) + ";");
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                if (resultSet.getInt("COUNT(*)") > 0) {
                    order_recorded = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return order_recorded;
    }


    public int outputSQLInt(ResultSet resultSet, String field_name) throws SQLException {
        int output_value = -1;
        while (resultSet.next()) {
            output_value = resultSet.getInt(field_name);
        }
        return output_value;
    }

    public double outputSQLDouble(ResultSet resultSet, String field_name) throws SQLException {
        double output_value = -1.0;
        while (resultSet.next()) {
            output_value = resultSet.getInt(field_name);
        }
        return output_value;
    }
}