package test

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import models._
import sys.process._
import scala.slick.session.Database
import play.api.db.DB
import play.api.{Play, Application}
import com.typesafe.config._

class UserSpec extends Specification {


  val conf:Config = ConfigFactory.load()
  val database = Database.forURL(conf.getString("db.default.url"), driver = conf.getString("db.default.driver"))

  "User" should	{

    "have valid username" in {
      database withSession {
        val user = User("", "test", "webmaster@food.com")
        val validationMessage = UserDAO.isValid(user)
        validationMessage must equalTo("Username is required")
      }
    }

    "have valid password" in {
      database withSession {
        val user = User("testuser", "", "webmaster@food.com")
        val validationMessage = UserDAO.isValid(user)
        validationMessage must equalTo("Password is required")
      }
    }

    "have valid email" in {
      database withSession {
        val user = User("testuser", "test", "")
        val validationMessage = UserDAO.isValid(user)
        validationMessage must equalTo("Email is required")
      }
    }

    "be createable" in {
      val user = User("testuser", "password", "testuser@food.com")

      database withSession {
        UserDAO.save(user)
        val user2 = UserDAO.get("testuser").get()
        user2.username must equalTo("testuser")
      }
    }

    "be retrieved by username" in {

      database withSession {
        val user = UserDAO.get("testuser").get()
        user.username must equalTo("testuser")
      }

    }

    "be retrieved by email" in {

      database withSession {
        val user = UserDAO.get("testuser@food.com").get()
        user.username must equalTo("testuser")
      }

    }

    "be retrieved by username and password" in{

      database withSession {
        val user = UserDAO.get("testuser", "password").get()
        user.username must equalTo("testuser")
      }

    }


    "be deletable" in {

      database withSession {
        UserDAO.delete("testuser")
        val user = UserDAO.get("testuser").get()
        user must equalTo(None)
      }

    }

  }


}
