package com.ganesh.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

public class PooledDataSource implements DataSource {

    private final Queue<Connection> availableConnection;
    private ConnectionPoolListener connectionPoolListener;

    public PooledDataSource(int size) throws ClassNotFoundException, SQLException {
        Class.forName("oracle.jdbc.driver.OracleDriver");

        availableConnection = new ConcurrentLinkedQueue<>();
        connectionPoolListener = new ConnectionPoolListenerImpl(availableConnection);
        IntStream.range(1, size).forEach(i -> {
            try {
                availableConnection.add(DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "system", "oracle"));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = availableConnection.poll();
        if (connection == null) {
            throw new SQLException("No connection available in the pool");
        }
        return new PooledConnection(connection, connectionPoolListener);
    }

}
