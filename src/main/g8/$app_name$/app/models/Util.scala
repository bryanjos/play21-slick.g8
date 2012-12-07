package models

import org.mindrot.jbcrypt._
import annotation.tailrec

object Util {

  def encryptPassword(password:String): String = {
    BCrypt.hashpw(password, BCrypt.gensalt())
  }


  def checkPassword(passwordToCheck:String, password:String):Boolean = {
    BCrypt.checkpw(passwordToCheck, password)
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

}
