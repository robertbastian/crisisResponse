package API

import com.mchange.v2.c3p0.ComboPooledDataSource
import org.scalatra.ScalatraServlet
import slick.driver.MySQLDriver.api.{Database => SDB}

trait Database extends ScalatraServlet {

  protected lazy val db = SDB.forDataSource(connectionPool)

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

  abstract override def destroy = {
    connectionPool.close
    super.destroy
  }
}
