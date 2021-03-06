package utilities

import org.joda.time.DateTime
import scala.util.{Success, Failure}

import play.api.Logger
import play.api.Play.current
import play.api.libs.mailer.Email
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Success, Failure}

import views._

import reactivemongo.api._
import reactivemongo.bson._

case class MailTemplate (
     _id: BSONObjectID,
     k: Int,
     s: String,
     b: String,
     sys: Option[System]
)

object MailUtility {
  
  // Use Reader to deserialize document automatically
  implicit object SystemBSONReader extends BSONDocumentReader[System] {
    def read(p_doc: BSONDocument): System = {
      System(
          p_doc.getAs[String]("eid").map(v => v),
          p_doc.getAs[BSONDateTime]("cdat").map(dt => new DateTime(dt.value)),
          p_doc.getAs[BSONDateTime]("mdat").map(dt => new DateTime(dt.value)),
          p_doc.getAs[String]("mby").map(v => v),
          p_doc.getAs[BSONDateTime]("ddat").map(dt => new DateTime(dt.value)),
          p_doc.getAs[String]("dby").map(v => v),
          p_doc.getAs[BSONDateTime]("ll").map(dt => new DateTime(dt.value))
      )
    }
  }
  
  implicit object MailTemplateBSONReader extends BSONDocumentReader[MailTemplate] {
    def read(p_doc: BSONDocument): MailTemplate = {
      MailTemplate(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[BSONInteger]("k").get.value,
          p_doc.getAs[String]("s").get,
          p_doc.getAs[String]("b").get,
          p_doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  private val col = DbConnUtility.config_db.collection("mailtemplate")
      
  private def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[MailTemplate]
  }
  
  // MailUtility.getEmail(List("htsiah@hotmail.com"), "Test", "Test")
  def getEmail(p_recipient: Seq[String], p_subject: String, p_body: String):Email =  {
    Email(
        subject = p_subject,
        from = "System - HRSifu.com <noreply@hrsifu.com>",
        to = p_recipient,
        bodyHtml = Some(html.mail.defaultv2(Tools.hostname ,p_body).toString)
    )
  }
  
  // MailUtility.getEmail(List("htsiah@hotmail.com"), List("htsiah@hotmail.com"), "Test", "Test")
  def getEmail(p_recipient: Seq[String], p_cc: Seq[String], p_subject: String, p_body: String):Email =  {
    Email(
        subject = p_subject,
        from = "System - HRSifu.com <noreply@hrsifu.com>",
        to = p_recipient,
        cc = p_cc,
        bodyHtml = Some(html.mail.defaultv2(Tools.hostname ,p_body).toString)
    )
  }
  
  // MailUtility.getEmailConfig(List("htsiah@hotmail.com"), 1, Map[Something->Something])
  def getEmailConfig(p_recipient: Seq[String], p_key: Int, p_replaceMap: Map[String, String]) =  {
    for {
      maybeEmail <- this.findOne(BSONDocument("k"->p_key))
    } yield {
      this.getEmail(
          p_recipient, 
          Tools.replaceSubString(maybeEmail.get.s,p_replaceMap.toList), 
          Tools.replaceSubString(maybeEmail.get.b,p_replaceMap.toList)         
      )
    }
  }
      
  // MailUtility.getEmailConfig(List("htsiah@hotmail.com"), List("htsiah@hotmail.com"), 1, Map[Something->Something])
  def getEmailConfig(p_recipient: Seq[String], p_cc: Seq[String], p_key: Int, p_replaceMap: Map[String, String]) =  {
    for {
      maybeEmail <- this.findOne(BSONDocument("k"->p_key))
    } yield {
      this.getEmail(
          p_recipient, 
          p_cc, 
          Tools.replaceSubString(maybeEmail.get.s,p_replaceMap.toList), 
          Tools.replaceSubString(maybeEmail.get.b,p_replaceMap.toList)
      )
    }
  }
  
  //*********** Obsolute v1.9 ***********//
  
  /**

  // MailUtility.sendEmail(List("htsiah@hotmail.com"), "Test", "Test")
  def sendEmail(p_recipient: Seq[String], p_subject: String, p_body: String) =  {
    val email = Email(
        subject = p_subject,
        from = "HRSifu <noreply@hrsifu.my>",
        to = p_recipient,
        bodyHtml = Some(html.mail.default(Tools.hostname ,p_body).toString)
    )
    val future = Future( MailerPlugin.send(email) )
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
  // MailUtility.sendEmail(List("htsiah@hotmail.com"), List("htsiah@hotmail.com"), "Test", "Test")
  def sendEmail(p_recipient: Seq[String], p_cc: Seq[String], p_subject: String, p_body: String) =  {
    val email = Email(
        subject = p_subject,
        from = "HRSifu <noreply@hrsifu.my>",
        to = p_recipient,
        cc = p_cc,
        bodyHtml = Some(html.mail.default(Tools.hostname ,p_body).toString)
    )
    val future = Future( MailerPlugin.send(email) )
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
  // MailUtility.sendEmailConfig(List("htsiah@hotmail.com"), 1, Map[Something->Something])
  def sendEmailConfig(p_recipient: Seq[String], p_key: Int, p_replaceMap: Map[String, String]) =  {
    val future = MailUtility.findOne(BSONDocument("k"->p_key))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {
        future.map( emailtemplate => {
          val mailsubject = Tools.replaceSubString(emailtemplate.get.s,p_replaceMap.toList)
          val mailsbody = Tools.replaceSubString(emailtemplate.get.b,p_replaceMap.toList)
          this.sendEmail(p_recipient, mailsubject, mailsbody)
        }) 
      }
    }
  }
  
  // MailUtility.sendEmailConfig(List("htsiah@hotmail.com"), 1, Map[Something->Something])
  def sendEmailConfig(p_recipient: Seq[String], p_cc: Seq[String], p_key: Int, p_replaceMap: Map[String, String]) =  {
    val future = MailUtility.findOne(BSONDocument("k"->p_key))
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {
        future.map( emailtemplate => {
          val mailsubject = Tools.replaceSubString(emailtemplate.get.s,p_replaceMap.toList)
          val mailsbody = Tools.replaceSubString(emailtemplate.get.b,p_replaceMap.toList)
          this.sendEmail(p_recipient, p_cc, mailsubject, mailsbody)
        }) 
      }
    }
  }

  */
  
}