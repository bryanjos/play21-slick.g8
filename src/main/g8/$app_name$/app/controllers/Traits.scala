package controllers

import play.api.mvc._
import models.User
import org.slf4j.LoggerFactory
import play.api.db.DB
import play.api.Play.current

case class AuthenticatedRequest[A](user: User, private val request: Request[A]) extends WrappedRequest(request)

trait Authentication extends Controller {

  def Authenticated[A](p: BodyParser[A])(f: AuthenticatedRequest[A] => Result) = {
    Action(p) { request =>
      Application.getCurrentUser(request).map { user =>
        f(AuthenticatedRequest(user, request))
      }.getOrElse(Unauthorized)
    }
  }

  // Overloaded method to use the default body parser
  import play.api.mvc.BodyParsers._
  def Authenticated(f: AuthenticatedRequest[AnyContent] => Result): Action[AnyContent]  = {
    Authenticated(parse.anyContent)(f)
  }

}

trait Logging {
  lazy val logger = LoggerFactory.getLogger("controllers")

  def Log(message:String) = {
    logger.error(message)
  }
}

trait DBAccess {
  lazy val database = scala.slick.session.Database.forDataSource(DB.getDataSource())
}
