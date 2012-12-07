package controllers

import play.api._
import play.api.mvc._
import models._
import scala.slick.session.Session
import concurrent.Future
import scala.slick.session.Session
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Application extends Controller with DBAccess {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  /**
   * Login page.
   */
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
    Ok( views.html.register( User(username = "",password = "",email = ""),""))
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

      val user = User(username = username, password = password, email = email)

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