package API.data

import com.mchange.v2.c3p0.ComboPooledDataSource
import slick.driver.PostgresDriver.api.{Database => SDB}

object DatabaseConnection {

  lazy val db = SDB.forDataSource(connectionPool)

  private lazy val connectionPool = {
    val c = new ComboPooledDataSource
    c.setDriverClass("org.postgresql.Driver")
    c.setJdbcUrl("jdbc:postgresql://"+sys.env("DB_URL")+":5432/main")
    c.setUser("root")
    c.setPassword(sys.env("DB_ROOT_PASSWORD"))
    c.setMinPoolSize(1)
    c.setMaxPoolSize(50)
    c.setAcquireIncrement(1)
    c
  }

  def close() = connectionPool.close()
}