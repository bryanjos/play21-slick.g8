import play.api._
import models._
import play.api.db.DB
import play.api.GlobalSettings
import play.api.Application
import play.api.Play.current
import scala.slick.session.{ Database, Session }
import Database.threadLocalSession
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.meta.MTable
import scala.slick.jdbc.StaticQuery
import play.api.mvc._
import play.api.mvc.Results._


object Global extends GlobalSettings {

  override def onStart(app: Application) {
    InitialData.init()
  }

  // called when a route is found, but it was not possible to bind the request parameters
  override def onBadRequest(request: RequestHeader, error: String) = {
    BadRequest("Bad Request: " + error)
  }

  // 500 - internal server error
  override def onError(request: RequestHeader, throwable: Throwable) = {
    InternalServerError(views.html.errors.onError(throwable))
  }

  // 404 - page not found error
  override def onHandlerNotFound(request: RequestHeader): Result = {
    NotFound(views.html.errors.onHandlerNotFound(request))
  }

}

/**
 * Initial set of data to be imported
 */
object InitialData {

  def init() = {

    lazy val database = Database.forDataSource(DB.getDataSource())

    database withSession {

      val tableList = MTable.getTables.list()
      val tableMap = tableList.map {
        t => (t.name.name, t)
      }.toMap

      if (!tableMap.contains("user")) {
        UserDAO.ddl.create
        UserDAO.save(User("admin","admin","admin@$app_name$.com","Admin","Admin",isAdmin = true))
      }

    }
  }
}