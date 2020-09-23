package com.joezhou.test;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author JoeZhou
 */
public class MySqlTest {

    @Test
    public void connectToMySql() {
        String user = "joezhou";
        String password = "joezhou";
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://localhost:3306/dbjoe";
        String urlParam = "?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC";
        url += urlParam;
        try {
            Class.forName(driver);
            Connection connection = DriverManager.getConnection(url, user, password);
            System.out.println(connection.isClosed() ? "fail" : "success");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

}
