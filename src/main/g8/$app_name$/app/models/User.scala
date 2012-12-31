package models

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.TypeMapper._
import scala.slick.session.Session


case class User(username: String, password: String, email: String, firstName: String, lastName: String, isAdmin: Boolean = false)


object UserDAO extends Table[User]("user") {

  def username = column[String]("user_name", O.PrimaryKey, O.DBType("varchar(50)"))
  def password = column[String]("password")
  def email = column[String]("email", O.DBType("varchar(200)"))
  def firstName = column[String]("first_name", O.DBType("varchar(50)"))
  def lastName = column[String]("last_name", O.DBType("varchar(50)"))
  def isAdmin = column[Boolean]("is_admin")

  def * = username ~ password ~ email ~ firstName ~ lastName ~ isAdmin <> (User, User.unapply _)


  def get(username:String)(implicit session:Session):Option[User] = {
    val q = for (u <- UserDAO if u.username === username || u.email === username) yield u
    q.list().headOption
  }

  def get(username:String, password:String)(implicit session:Session):Option[User] = {
    val q = for (u <- UserDAO if u.username === username || u.email === username) yield u
    val userOption = q.list().headOption

    userOption.map { user =>
      if(Util.checkPassword(password, user.password))
        userOption
      else
        None
    }.getOrElse {
      None
    }
  }

  def list(implicit session:Session):Seq[User] = {
    val q = for(u <- UserDAO) yield u
    q.list
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

  def makeAdmin(username:String)(implicit session:Session){
    val user = get(username)

    user.map { u =>
      val userToSave = u.copy(isAdmin = !u.isAdmin)
      save(userToSave)
    }
  }


  def isValid(user:User, isUpdate:Boolean = false, checkPassword:Boolean = true)(implicit session:Session): String = {
    if(user.username.length == 0)
      return "Username is required"

    if(user.username.length > 50)
      return "Username exceeds 50 characters"

    if(user.email.length == 0)
      return "Email is required"

    if(user.email.length > 50)
      return "Email exceeds 50 characters"

    if (checkPassword){
      if(user.password.length == 0)
        return "Password is required"

      if(user.password.length > 20)
        return "Password exceeds 20 characters"
    }

    if(user.firstName.length == 0)
      return "First Name is required"

    if(user.firstName.length > 50)
      return "First Name exceeds 50 characters"

    if(user.lastName.length == 0)
      return "Last Name is required"

    if(user.lastName.length > 50)
      return "Last Name exceeds 50 characters"

    if (!isUpdate){
      val user2 = UserDAO.get(user.username)
      if(user2 != None)
        return "Username already in use."
    }

    null
  }



}
