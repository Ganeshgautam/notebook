package com.ganesh.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class PooledConnection extends DelegatingConnection {

    private ConnectionPoolListener connectionPoolListener;
    private Connection delegate;
    private volatile boolean isClosed;

    public PooledConnection(Connection delegate, ConnectionPoolListener connectionPoolListener) {
        super(delegate);
        this.delegate = delegate;
        this.connectionPoolListener = connectionPoolListener;
    }

    @Override
    public void close() throws SQLException {
        isClosed = true;
        connectionPoolListener.onClosed(delegate);
    }

    @Override
    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    @Override
    public Statement createStatement() throws SQLException {
        if (isClosed) {
            throw new SQLException("Connection is already closed");
        }
        return super.createStatement();
    }
}
