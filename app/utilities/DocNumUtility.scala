package utilities

import org.joda.time.DateTime
import scala.util.{Success, Failure,Try}
import scala.concurrent.{Future,Await}
import scala.concurrent.duration.Duration

import play.api.Play
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
    def read(doc: BSONDocument): System = {
      System(
          doc.getAs[String]("eid").map(v => v),
          doc.getAs[BSONDateTime]("cdat").map(dt => new DateTime(dt.value)),
          doc.getAs[BSONDateTime]("mdat").map(dt => new DateTime(dt.value)),
          doc.getAs[String]("mby").map(v => v),
          doc.getAs[BSONDateTime]("ddat").map(dt => new DateTime(dt.value)),
          doc.getAs[String]("dby").map(v => v),
          doc.getAs[BSONDateTime]("ll").map(dt => new DateTime(dt.value))
      )
    }
  }
  
  implicit object DocNumBSONReader extends BSONDocumentReader[DocNum] {
    def read(doc: BSONDocument): DocNum = {
      DocNum(
          doc.getAs[BSONObjectID]("_id").get,
          doc.getAs[String]("f").get,
          doc.getAs[BSONInteger]("n").get.value,
          doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  private val dbname = Play.current.configuration.getString("mongodb_config").getOrElse("config")
  private val uri = Play.current.configuration.getString("mongodb_config_uri").getOrElse("mongodb://localhost")
  // private val driver = new MongoDriver(ActorSystem("DefaultMongoDbDriver"))
  private val driver = new MongoDriver()
  private val connection: Try[MongoConnection] = MongoConnection.parseURI(uri).map { 
    parsedUri => driver.connection(parsedUri)
  }
  private val db = connection.get.db(dbname)
  private val col = db.collection("docnum")
  	
  def init() = {
    println("Initialized Db Collection: " + col.name)
  }
  
  def close() = {
    driver.close()
  }
  
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