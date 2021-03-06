package models

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Logger
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.MongoController.readFileReads
import play.api.libs.json.{ Json, JsObject, JsString }

import reactivemongo.api._
import reactivemongo.api.gridfs.GridFS
import reactivemongo.api.gridfs.ReadFile
import reactivemongo.bson._

import org.joda.time.DateTime
import utilities.DbConnUtility

object PersonBulkImportModel {
  
  val gridFS = this.getGridFS
  val gridFSBSON = GridFS(DbConnUtility.dir_db)
  
  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]
  
  // let's build an index on our gridfs chunks collection if none
  gridFS.ensureIndex().onComplete {
    case index =>
      Logger.info(s"Checked index, result is $index")
  }
  
  private def getGridFS = {
    import play.modules.reactivemongo.json.collection._
    GridFS[JSONSerializationPack.type](DbConnUtility.dir_db)
  }
  
  def find(p_query:JsObject, p_request:RequestHeader) = {
    this.gridFS.find[JsObject, JSONReadFile]((p_query.++(Json.obj("metadata.eid" -> p_request.session.get("entity"), "metadata.dby" -> Json.obj("$exists" -> false)))))
  }
    
  def findById(p_id: String, p_request:RequestHeader) = {
    this.find(Json.obj("_id" -> p_id), p_request)
  }
  
  def findByLK(p_lk: String, p_request:RequestHeader) = {
    this.find(Json.obj("metadata.lk" -> p_lk), p_request)
  }
  
  def remove(p_query:JsObject, p_request:RequestHeader) = {
    this.find(p_query, p_request).collect[List]().foreach { files =>
      files.foreach { file => {
        LeaveFileModel.gridFS.files.update(
            Json.obj("_id" -> file.id),
            Json.obj("$set" -> Json.obj("metadata" -> Json.obj(     
                "eid" -> file.metadata.value.get("eid").get,
                "filename" -> file.metadata.value.get("filename").get,
                "lk" -> file.metadata.value.get("lk").get,
                "f" -> file.metadata.value.get("f").get,
                "cby" -> file.metadata.value.get("cby").get,
                "ddat" -> BSONDateTime(new DateTime().getMillis),
                "dby" -> p_request.session.get("username")
            )))
        )
      }}
    }
  }
  
  def removeById(p_id: String, p_request:RequestHeader) = {
    this.remove(Json.obj("_id" -> p_id), p_request)    
  }
  
  def removeByLK(p_lk: String, p_request:RequestHeader) = {
    this.remove(Json.obj("metadata.lk" -> p_lk), p_request)       
  }
  
  def removePermanently(p_id: String, p_request:RequestHeader) = {
    this.gridFS.remove(Json toJson p_id)
  }
  
}