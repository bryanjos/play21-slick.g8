package models

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.StaticQuery
import scala.slick.lifted.TypeMapper._
import scala.slick.session.Session
import org.mindrot.jbcrypt._


case class User(username: String, password: String, email: String)


object UserDAO extends Table[User]("user") {

  def username = column[String]("user_name", O.PrimaryKey, O.DBType("varchar(50)"))
  def password = column[String]("password", O.DBType("varchar(20)"))
  def email = column[String]("email", O.DBType("varchar(200)"))

  def * = username ~ password ~ email <> (User, User.unapply _)


  def get(username:String)(implicit session:Session):Option[User] = {
    val q = for (u <- UserDAO if u.username === username || u.email === username) yield u
    q.list().headOption
  }

  def get(username:String, password:String)(implicit session:Session):Option[User] = {
    val q = for (u <- UserDAO if u.username === username || u.email === username) yield u
    val user = q.list().headOption.getOrElse(null)

    if (user != null && Util.checkPassword(user.password, password)){
      Option[User](user)
    }else{
      None
    }
  }

  def save(user:User)(implicit session:Session){
    if(get(user.username) == None){
      val userToSave = user.copy(password = Util.encryptPassword(user.password))
      this.insert(userToSave)
    }else{
      val q = for(u <- UserDAO if u.username === u.username) yield u
      q.update(user)
    }
  }

  def delete(username:String)(implicit session:Session) = {
    val q = for(u <- UserDAO if u.username === username) yield u
    q.delete
  }


  def isValid(user:User)(implicit session:Session): String = {
    if(user.username.length() == 0)
      return "Username is required"

    if(user.username.length() > 50)
      return "Username exceeds 50 characters"

    if(user.email.length() == 0)
      return "Email is required"

    if(user.email.length() > 50)
      return "Email exceeds 50 characters"

    if(user.password.length() == 0)
      return "Password is required"

    if(user.password.length() > 20)
      return "Password exceeds 20 characters"

    null
  }

}
