package models

import org.mindrot.jbcrypt._
import annotation.tailrec
import java.sql.Timestamp
import java.util.Date
import java.text.SimpleDateFormat
import util.Random

object Util {

  def encryptPassword(password:String): String = {
    BCrypt.hashpw(password, BCrypt.gensalt())
  }


  def checkPassword(passwordToCheck:String, password:String):Boolean = {
    BCrypt.checkpw(passwordToCheck, password)
  }

  def generatePassword(): String = {
    uniqueRandomKey(10)
  }

  lazy val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ ("-!£$")

  def uniqueRandomKey(length: Int) : String =
  {

    val newKey = (1 to length).map(
      x =>
      {
        val index = Random.nextInt(chars.length)
        chars(index)
      }
    ).mkString("")

    newKey

  }

  def slugify(str: String): String = {
    import java.text.Normalizer
    Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\w ]", "").replace(" ", "-").toLowerCase
  }

  @tailrec
  def generateUniqueSlug(slug: String, existingSlugs: Seq[String]): String = {
    if (!(existingSlugs contains slug)) {
      slug
    } else {
      val EndsWithNumber = "(.+-)([0-9]+)$".r
      slug match {
        case EndsWithNumber(s, n) => generateUniqueSlug(s + (n.toInt + 1), existingSlugs)
        case s => generateUniqueSlug(s + "-2", existingSlugs)
      }
    }
  }

  def formatDate(time:Timestamp):String = {
    val dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm")
    dateFormat.format(time)
  }

}
