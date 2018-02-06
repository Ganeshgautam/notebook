package com.ganesh.connection;

import java.sql.Connection;

public interface ConnectionPoolListener {

    void onClosed(Connection connection);

}
