package com.joezhou.datasource;

import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author JoeZhou
 */
public class DataSourceTest {

    @Test
    public void dataSourceTest() {
        DataSource dataSource = new DataSource();
        Connection connection = dataSource.getConnection();
        try {
            System.out.println(connection.isClosed() ? "fail" : "success");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dataSource.closeConnection(connection);
        }
    }
}
