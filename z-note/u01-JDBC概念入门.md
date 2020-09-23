# 1. 概念

**概念：** JDBC，全名Java DataBase Connectivity，java和数据库连接中间件，是sun公司面对各个数据库提供的一组接口，从本质上来说就是调用者（程序员）和实现者（数据库厂商）之间的协议。
- 如果想让java连接数据库必须向java项目添加该数据库对jdbc的支持（对应数据库的驱动jar包），该支持由具体数据库厂商提供。
- JDBC的API使得开发人员可以使用纯java的方式来连接数据库并进行操作，使得向各种关系数据发送SQL语句就是一件很容易的事。
- JDBC对Java程序员而言是API，对实现与数据库连接的服务提供商而言是接口模型，作为API，JDBC为程序开发提供标准的接口，并为数据库厂商及第三方中间件厂商实现与数据库的连接提供了标准方法。

> 不使用JDBC时的Java连库图

> 使用JDBC时的Java连库图

# 2. 搭建流程

**流程：**
1. 引入对应数据库的驱动包：
    - mysql-connector-java-8.0.15.jar
2. 编写mysql连接测试方法。
3. 定义连库账号和密码。
4. 定义连库的URL：`jdbc:mysql://IP地址:端口号/数据库名?参数列表`：
    - `user`：数据库用户名。
    - `password`：数据库密码。
    - `useUnicode=true`：是否使用Unicode字符集，如果参数characterEncoding设置为gb2312或gbk，本参数值必须设置为true。
    - `characterEncoding=utf-8`：当useUnicode设置为true时，指定字符编码，比如可设置为gb2312或gbk。
    - `autoReconnect=true`：当数据库连接异常中断时，是否自动重新连接，默认false。
    - `maxReconnects=5`：当autoReconnect设置为true时，重试连接的次数，默认3次。
    - `initialTimeout=3`：当autoReconnect设置为true时，两次重连之间的时间间隔，单位是秒，默认2秒。
    - `autoReconnectForPools=true`：是否使用针对数据库连接池的重连策略，默认false。
    - `failOverReadOnly=true`：自动重连成功后，连接是否设置为只读，默认true。
    - `useSSL=false`：消除控制台的一个红色警告，使用SSL漏洞修复。
    - `serverTimezone=UTC`：mysql8版本的驱动必须填写的，mysql5版本可以不写。
    - `connectTimeout`：和数据库服务器建立socket连接时的超时，单位是毫秒，0表示永不超时，默认0。
    - `socketTimeout`：socket操作（读写）超时，单位是毫秒，0表示永不超时，默认0。
    - 在使用数据库连接池的情况下，最好设置 `autoReconnect=true&failOverReadOnly=false` 这两个参数。
5. 通过反射的方式驱动数据库，即反射Driver类：
    - mysql5版本的驱动，Driver类所在的位置：`com.mysql.jdbc.Driver`
    - mysql8版本的驱动，Driver类所在的位置：`com.mysql.cj.jdbc.Driver`
6. 通过 `java.sql.DriverManager` 类的来获取一个连接：
    - `static Connection getConnection(String url, String user, String password)`
    - `static Connection getConnection(String url)`：需要将账号密码附在URL参数中。
7. 测试连接是否关闭：
    - `boolean isClosed()`：关闭返回true。

**源码：** test/ConnectTest.java


3. DataSourc封装

为了有效地重复利用上面的驱动连接代码，你可以将上面测试代码，封装成一个DataSource类，这个类负责驱动数据库、制造连接和关闭连接。

引入静态块（因为驱动的代码你肯定希望全程只运行一次就够了），将驱动的过程放在静态块中。
将驱动串，连接串等配置信息提取成私有静态属性。
封装一个获取连接（线程保护）和关闭连接的方法。

类图

源代码
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Joe
 */
public class DataSource {
    
	private static String driver = "com.mysql.cj.jdbc.Driver";
	private static String user = "joe";
	private static String password = "joe";
	private static String url = "jdbc:mysql://127.0.0.1:3306/j256?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC";
	private static Connection connection;

	static {
		try {
			Class.forName(driver);
			connection = DriverManager.getConnection(url, user, password);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	public synchronized Connection getConnection() {
		return connection;
	}

	public void closeConnection(Connection connection) {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}


测试
@Test
public void connectTestWithDataSource() throws SQLException {
    
    // 赵四获取连接
    Connection connection1 = new DataSource().getConnection();
    
    // 刘能获取连接
    Connection connection2 = new DataSource().getConnection();
    
    System.out.println(connection1.isClosed());
    System.out.println(connection2.isClosed());
    
    // 赵四和刘能获取同一个连接，如果赵四关闭这个连接的时候刘能还没有使用完...
    System.out.println(connection1 == connection2);
}


4. DataSourc优化：连接池

每次访问数据库，都需要获取一个连接，很浪费资源，我们可以在直接在静态块中准备10个或者更多的连接，形成一个连接池，当调用者想要获取连接的时候，直接从池中获取，当调用者想要关闭连接的时候，将连接回收到池中，重新利用，这就是连接池的概念。

类图


源代码
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Joe
 */
public class DataSource {

	private static String driver = "com.mysql.cj.jdbc.Driver";
	private static String user = "joe";
	private static String password = "joe";
	private static String url = "jdbc:mysql://127.0.0.1:3306/j256?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC";
	private static List<Connection> connectionPool;
	private static int connectionPoolSize = 10;

	static {
		try {
			Class.forName(driver);
			initConnectionPool();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	private static void initConnectionPool() throws SQLException {
        connectionPool = new ArrayList<>();
		for (int i = 0; i < connectionPoolSize; i++) {
			Connection connection = createNewConnection();
			connectionPool.add(connection);
		}
	}

	private static Connection createNewConnection() throws SQLException {
		return DriverManager.getConnection(url, user, password);
	}

	public synchronized Connection getConnection() {
		Connection connection = null;
		try {
			if (connectionPool.isEmpty()) {
				connection = createNewConnection();
			} else {
				connection = connectionPool.remove(0);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return connection;
	}

	public void closeConnection(Connection connection) {
		try {
			if (connectionPool.size() < connectionPoolSize) {
				connectionPool.add(connection);
			} else {
				connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}


测试
@Test
public void connectionPoolTest() throws SQLException {
    
    // 赵四获取连接
    Connection connection1 = new DataSource().getConnection();
    
    // 刘能获取连接
    Connection connection2 = new DataSource().getConnection();
    
    System.out.println(connection1.isClosed());
    System.out.println(connection2.isClosed());
    
    // 赵四和刘能获取不同连接，互相之间不干扰
    System.out.println(connection1 == connection2);
}


5. DataSourc优化：属性文件

       属性文件可以帮助我们代码解耦，将一些配置信息单独提取出来，放入到属性文件中，然后使用程序去读取属性文件的内容，这样的操作可以使得配置与代码分离。

建议新创建一个资源文件夹config，并直接在该目录下创建db.properties文件。

资源文件夹不能嵌套资源文件夹，但是可以包含package。

db.properties文件
driver=com.mysql.cj.jdbc.Driver
user=joe
password=joe
url=jdbc:mysql://127.0.0.1:3306/j256?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC
connectionPoolSize=10


tips：如果url中的utf-8不支持，使用utf8替换。

创建从属性文件中获取信息的方法，然后在静态块的第一行调用它
private static void getMsgFromPropertiesFile() {
    ResourceBundle bundle = PropertyResourceBundle.getBundle("db");
    driver = bundle.getString("driver");
    user = bundle.getString("user");
    password = bundle.getString("password");
    url = bundle.getString("url");
    connectionPoolSize = Integer.valueOf(bundle.getString("connectionPoolSize"));
}


6. DataSourc优化：静态工厂模式

在测试类中，我们需要自己new一个DataSource然后再使用它，即是生产者，又是使用者，所以我们可以使用静态工厂模式，将生产者和使用者分离。

类图


工厂类源代码
public class DataSourceFactory {
	public static DataSource getDataSource() {
		return new DataSource();
	}
}


测试
// 生产者和使用者分离：静态工厂
@Test
public void connectTestWithStaticFactoryModel() throws SQLException {
	DataSource dataSource = DataSourceFactory.getDataSource();
	Connection connection = dataSource.getConnection();
	System.out.println(connection.isClosed());
}


--------------------------------------------------------------------------------
 By Mr.Joe