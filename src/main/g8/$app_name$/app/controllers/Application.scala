package controllers

import play.api._
import play.api.mvc._
import models._
import play.api.Play.current
import play.api.cache.Cache
import scala.concurrent.Future
import scala.slick.session.Session
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.typesafe.plugin._
import models.User
import com.typesafe.plugin.MailerPlugin

object ApplicationController extends Controller with Authentication with DBAccess {

  def login = Action { implicit request =>
    Ok( views.html.login("") )
  }

  def postLogin = Action(parse.multipartFormData) {implicit request =>

    val postData : Map[String,Seq[String]] = request.body.asFormUrlEncoded

    val username:String = postData.getOrElse("username", List[String](null)).head
    val password:String = postData.getOrElse("password", List[String](null)).head

    val promise = Future {
      database.withSession{ implicit session:Session =>
        UserDAO.get(username, password)
      }
    }

    Async {
      promise.map { result =>
        result.map{ user =>
          Redirect("/").withSession(session + ("username" -> user.username))
        }.getOrElse{
          Ok(views.html.login("Invalid Credentials"))
        }
      }
    }
  }

  def logout = Action{ implicit request =>
    Redirect("/").withNewSession
  }

  def register = Action { implicit request =>
    Ok( views.html.register( User(username = "",password = "",email = "", firstName = "", lastName = ""),""))
  }


  def postRegistration = Action(parse.multipartFormData) {implicit request =>
    val promise = Future {
      getPostRegistration(request.body.asFormUrlEncoded)
    }

    Async {
      promise.map { result =>
      if (result._2 == null){
        Redirect("/").withSession(session + ("username" -> result._1.username)  )
      }else{
        Ok(views.html.register(result._1, result._2))
      }
    }
  }
  }

  def getPostRegistration(postData : Map[String,Seq[String]]):(User, String) = {
    database.withSession{ implicit session:Session =>
      val username:String = postData.getOrElse("username", List[String](null)).head
      val password:String = postData.getOrElse("password", List[String](null)).head
      val email:String = postData.getOrElse("email", List[String](null)).head
      val firstName:String = postData.getOrElse("firstName", List[String](null)).head
      val lastName:String = postData.getOrElse("lastName", List[String](null)).head

      val user = User(username = username, password = password, email = email, firstName = firstName, lastName = lastName)

      val validationMessage = UserDAO.isValid(user)

      if(validationMessage == null){
        UserDAO.save(user)
        val userFromDB = UserDAO.get(username)
        (userFromDB.get, validationMessage)
      }else{
        (user, validationMessage)
      }
    }
  }

  def passwordLost = Action { implicit request =>
    Ok( views.html.passwordLost( "" ) )
  }

  def postPasswordLost = Action(parse.multipartFormData) {implicit request =>

    val promise = Future {
      getPostPasswordLost(request.body.asFormUrlEncoded)
    }

    Async {
      promise.map { result =>
        if(result._1){
          sendMail("$app_name$ Password Reset", result._2,
            """<html>
                    <body>
                      <p>
                        Your randomly generated password is %s. Login using this password.
                        If you want to change your password, go to user settings and change your password
                      </p>
                    </body>
                  </html>""".format(result._3))

          Redirect("/").flashing("success" -> "An email was sent to you with your password information.")
        }else
          Ok(views.html.passwordLost("Username does not exist"))
      }
    }
  }

  def getPostPasswordLost(postData : Map[String,Seq[String]]):(Boolean, String, String) = {
    database.withSession{ implicit session:Session =>
      val username:String = postData.getOrElse("username", List[String](null)).head

      val lostUser = UserDAO.get(username)
      val generatedPassword = Util.generatePassword()

      lostUser.map { user =>
        val userToSave = user.copy(password = generatedPassword)

        UserDAO.save(userToSave)
        (true, user.email, generatedPassword)
      }.getOrElse{
        (false, null, null)
      }
    }
  }

  def sendMail(subject:String, recipient:String, htmlBody:String){
    val mail = use[MailerPlugin].email
    mail.setSubject(subject)
    mail.addRecipient(recipient)
    mail.addFrom("noreply@$app_name$.com")
    mail.sendHtml(htmlBody)
  }


  def userEdit = Authenticated { implicit request =>
    database.withSession{ implicit session:Session =>
      val promise = Future {
        request.user
      }

      Async {
        promise.map { result => Ok( views.html.useredit(result, "") ) }
      }

    }
  }

  def postUserEdit = Authenticated(parse.multipartFormData) {implicit request =>
    val promise = Future {
      getUserEdit(request.user.username, request.body.asFormUrlEncoded)
    }

    Async {
      promise.map { result =>
        if(result._2 == null)
          Redirect("/").flashing("success" -> "Updated Successfully").withSession(session   + ("username" -> result._1.username))
        else
          Ok(views.html.useredit(result._1, result._2))
      }
    }
  }


  def getUserEdit(username:String, postData : Map[String,Seq[String]] ):(User, String) = {
    database.withSession{ implicit session:Session =>
      val user = UserDAO.get(username).get

      val email:String = postData.getOrElse("email", List[String](null)).head
      val password:String = postData.getOrElse("password", List[String](null)).head
      val firstName:String = postData.getOrElse("firstName", List[String](null)).head
      val lastName:String = postData.getOrElse("lastName", List[String](null)).head

      val updatedUser = User(
        username = user.username,
        password = if(password != null && password.length() > 0) password else user.password,
        email = email,
        firstName = firstName,
        lastName = lastName,
        isAdmin = user.isAdmin)

      val newPassword =  password != null && password.length() > 0
      val validationMessage = UserDAO.isValid(updatedUser,isUpdate=true,checkPassword=newPassword)
      if(validationMessage == null){

        val userToSave = User(
          username = user.username,
          password = if(newPassword) Util.encryptPassword(password) else user.password,
          email = updatedUser.email,
          firstName = firstName,
          lastName = lastName,
          isAdmin = user.isAdmin)

        UserDAO.save(userToSave)
        (userToSave, null)

      }else{
        (updatedUser, validationMessage)
      }
    }
  }


  def getCurrentUser(implicit request:RequestHeader):Option[User] = {
    request.session.get("username").map { username =>
      database.withSession{ implicit session:Session =>
        return UserDAO.get(username)
      }
    }.getOrElse{
      return None
    }
  }
  
}