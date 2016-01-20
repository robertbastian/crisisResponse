package API.data

import com.mchange.v2.c3p0.ComboPooledDataSource
import slick.driver.MySQLDriver.api.{Database => SDB}

object DatabaseConnection {

  lazy val db = SDB.forDataSource(connectionPool)

  private lazy val connectionPool = {
    val c = new ComboPooledDataSource
    c.setDriverClass("com.mysql.jdbc.Driver")
    c.setJdbcUrl("jdbc:mysql://"+sys.env("DB_URL")+":3306/crisis")
    c.setUser("root")
    c.setPassword(sys.env("DB_ROOT_PASSWORD"))
    c.setMinPoolSize(1)
    c.setMaxPoolSize(50)
    c.setAcquireIncrement(1)
    c
  }

  def close = connectionPool.close
}