package models

import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import reactivemongo.api._
import reactivemongo.bson._

import utilities.{System, SystemDataStore, DbConnUtility}

import scala.util.{Success, Failure}
import org.joda.time.DateTime

case class ConfigLeavePolicy (
    _id: BSONObjectID,
    lt: String,
    set: ConfigLeavePolicySetting,
    ent: ConfigEntitlement,
    sys: Option[System]
)

case class ConfigLeavePolicySetting (
    g: String,
    acc: String,
    ms: String,
    dt: String,
    nwd: Boolean,
    cexp: Int,
    scal: Boolean,
    msd: Boolean
)

case class ConfigEntitlementValue (
    e: Int,
    s: Int,
    cf: Int
)

case class ConfigEntitlement (
    e1: ConfigEntitlementValue,
    e2: ConfigEntitlementValue,
    e3: ConfigEntitlementValue,
    e4: ConfigEntitlementValue,
    e5: ConfigEntitlementValue, 
    e6: ConfigEntitlementValue,
    e7: ConfigEntitlementValue,
    e8: ConfigEntitlementValue,
    e9: ConfigEntitlementValue,
    e10: ConfigEntitlementValue
)

object ConfigLeavePolicyModel {

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
  
  implicit object ConfigLeavePolicySettingBSONReader extends BSONDocumentReader[ConfigLeavePolicySetting] {
    def read(p_doc: BSONDocument): ConfigLeavePolicySetting = {
      ConfigLeavePolicySetting(
          p_doc.getAs[String]("g").get,
          p_doc.getAs[String]("acc").get,
          p_doc.getAs[String]("ms").get,
          p_doc.getAs[String]("dt").get,
          p_doc.getAs[Boolean]("nwd").get,
          p_doc.getAs[Int]("cexp").get,
          p_doc.getAs[Boolean]("scal").get,
          p_doc.getAs[Boolean]("msd").get
      )
    }
  }
  
  implicit object ConfigEntitlementValueBSONReader extends BSONDocumentReader[ConfigEntitlementValue] {
    def read(p_doc: BSONDocument): ConfigEntitlementValue = {
      ConfigEntitlementValue(
          p_doc.getAs[Int]("e").get,
          p_doc.getAs[Int]("s").get,
          p_doc.getAs[Int]("cf").get
      )
    }
  }
    
  implicit object ConfigEntitlementBSONReader extends BSONDocumentReader[ConfigEntitlement] {
    def read(p_doc: BSONDocument): ConfigEntitlement = {
      ConfigEntitlement(
          p_doc.getAs[ConfigEntitlementValue]("e1").get,
          p_doc.getAs[ConfigEntitlementValue]("e2").get,
          p_doc.getAs[ConfigEntitlementValue]("e3").get,
          p_doc.getAs[ConfigEntitlementValue]("e4").get,
          p_doc.getAs[ConfigEntitlementValue]("e5").get,
          p_doc.getAs[ConfigEntitlementValue]("e6").get,
          p_doc.getAs[ConfigEntitlementValue]("e7").get,
          p_doc.getAs[ConfigEntitlementValue]("e8").get,
          p_doc.getAs[ConfigEntitlementValue]("e9").get,
          p_doc.getAs[ConfigEntitlementValue]("e10").get
      )
    }
  }
  
  implicit object ConfigLeavePolicyBSONReader extends BSONDocumentReader[ConfigLeavePolicy] {
    def read(p_doc: BSONDocument): ConfigLeavePolicy = {
      ConfigLeavePolicy(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[String]("lt").get,
          p_doc.getAs[ConfigLeavePolicySetting]("set").get,
          p_doc.getAs[ConfigEntitlement]("ent").get,
          p_doc.getAs[System]("sys").map(o => o)
      )
    }
  }

  private val col = DbConnUtility.config_db.collection("leavepolicy")

  private def updateSystem(p_doc:ConfigLeavePolicy) = {
    val eid = p_doc.sys.get.eid.getOrElse(None)
    val cdat = p_doc.sys.get.cdat.getOrElse(None)
    val mdat = p_doc.sys.get.mdat.getOrElse(None)
    val mby = p_doc.sys.get.mby.getOrElse(None)
    val ddat = p_doc.sys.get.ddat.getOrElse(None)
    val dby = p_doc.sys.get.dby.getOrElse(None)
    val ll = p_doc.sys.get.ll.getOrElse(None)
    val sys_doc = System(
        eid = if (eid!=None) {Some(p_doc.sys.get.eid.get)} else {None},
        cdat = if (cdat!=None) {Some(p_doc.sys.get.cdat.get)} else {None},
        mdat = if (mdat!=None) {Some(p_doc.sys.get.mdat.get)} else {None},
        mby = if (mby!=None) {Some(p_doc.sys.get.mby.get)} else {None},
        ddat = if (ddat!=None) {Some(p_doc.sys.get.ddat.get)} else {None},
        dby = if (dby!=None) {Some(p_doc.sys.get.dby.get)} else {None},
        ll= if (ll!=None) {Some(p_doc.sys.get.ll.get)} else {None}
    ) 
    sys_doc
  }
  
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[ConfigLeavePolicy](ReadPreference.primary).collect[List]()
  }
  
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[ConfigLeavePolicy]
  }
  
}