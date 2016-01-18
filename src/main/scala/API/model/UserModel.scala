package API.model

import slick.driver.MySQLDriver.api._

object UserModel {
  case class User(
            id: Option[Long],
            var name: String,
            var gender: String)

  class Users(tag: Tag) extends Table[User](tag, "User"){
    def id = column[Long]("id",O.PrimaryKey,O.AutoInc)
    def name = column[String]("name")
    def gender = column[String]("gender")
    def * = (id.?,name,gender) <> (User.tupled, User.unapply)
  }

  val users = TableQuery[Users]
}

