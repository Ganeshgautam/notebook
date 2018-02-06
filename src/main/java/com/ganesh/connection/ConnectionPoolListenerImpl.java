package com.ganesh.connection;

import java.sql.Connection;
import java.util.Queue;

public class ConnectionPoolListenerImpl implements ConnectionPoolListener {

    private Queue<Connection> availableConnections;

    public ConnectionPoolListenerImpl(Queue<Connection> availableConnections) {
        this.availableConnections = availableConnections;
    }

    @Override
    public void onClosed(Connection connection) {
        availableConnections.add(connection);
    }

}
