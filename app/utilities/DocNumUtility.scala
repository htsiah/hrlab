package utilities

import org.joda.time.DateTime
import scala.util.{Success, Failure}
import scala.concurrent.{Future,Await}
import scala.concurrent.duration.Duration

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands.FindAndModify

case class DocNum(
    _id: BSONObjectID,
	f: String, //form
	n: Integer,	//number
	sys: Option[System]
)

object DocNumUtility {
  
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
  
  implicit object DocNumBSONReader extends BSONDocumentReader[DocNum] {
    def read(p_doc: BSONDocument): DocNum = {
      DocNum(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[String]("f").get,
          p_doc.getAs[BSONInteger]("n").get.value,
          p_doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  private val col = DbConnUtility.config_db.collection("docnum")
  	  
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[DocNum]
  }
    
  def getNumberText(p_form:String,p_eid:String="",p_format:String="%06d") = {
    val query = if(p_eid=="") { BSONDocument("f"->p_form)} else { BSONDocument("f"->p_form,"sys.eid"->p_eid)}
    Await.result(col.update(query, BSONDocument("$inc" -> BSONDocument("n" -> 1)), upsert = true), Tools.db_timeout)
    val doc = Await.result(this.findOne(query), Duration(5000, "millis"))
    p_format.format(doc.get.n)	
  }
  
}