package models

import scala.concurrent.Await
import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.api._
import reactivemongo.bson._
import utilities.{System,SystemDataStore,Tools,DbConnUtility}
import scala.util.{Success, Failure}
import scala.collection.mutable.ArrayBuffer
import org.joda.time.DateTime
import org.joda.time.Months

case class LeaveProfile (
    _id: BSONObjectID,
    pid: String,	// person object id
    pn: String,
    lt: String,
    cal: LeaveProfileCalculation,
    me: LeaveProfileMonthEarn,
    set_ent: Entitlement, // Case class from leave policy model
    sys: Option[System]
)

case class LeaveProfileCalculation (
    ent: Int,
    ear: Double,
    adj: Double,
    uti: Double,
    cf: Double,
    cfuti: Double,
    cfexp: Double,
    papr: Double,
    bal: Double,
    cbal: Double
)

case class LeaveProfileMonthEarn (
    jan: Double,
    feb: Double,
    mar: Double,
    apr: Double,
    may: Double,
    jun: Double,
    jul: Double,
    aug: Double,
    sep: Double,
    oct: Double,
    nov: Double,
    dec: Double
)
    
object LeaveProfileModel {

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
  
  implicit object LeaveProfileMonthEarnBSONReader extends BSONDocumentReader[LeaveProfileMonthEarn] {
    def read(p_doc: BSONDocument): LeaveProfileMonthEarn = {
      LeaveProfileMonthEarn(
          p_doc.getAs[Double]("jan").get,
          p_doc.getAs[Double]("feb").get,
          p_doc.getAs[Double]("mar").get,
          p_doc.getAs[Double]("apr").get,
          p_doc.getAs[Double]("may").get,
          p_doc.getAs[Double]("jun").get,
          p_doc.getAs[Double]("jul").get,
          p_doc.getAs[Double]("aug").get,
          p_doc.getAs[Double]("sep").get,
          p_doc.getAs[Double]("oct").get,
          p_doc.getAs[Double]("nov").get,
          p_doc.getAs[Double]("dec").get
      )
    }
  }
  
  implicit object EntitlementValueBSONReader extends BSONDocumentReader[EntitlementValue] {
    def read(p_doc: BSONDocument): EntitlementValue = {
      EntitlementValue(
          p_doc.getAs[Int]("e").get,
          p_doc.getAs[Int]("s").get,
          p_doc.getAs[Int]("cf").get
      )
    }
  }
  
  implicit object EntitlementBSONReader extends BSONDocumentReader[Entitlement] {
    def read(p_doc: BSONDocument): Entitlement = {
      Entitlement(
          p_doc.getAs[EntitlementValue]("e1").get,
          p_doc.getAs[EntitlementValue]("e2").get,
          p_doc.getAs[EntitlementValue]("e3").get,
          p_doc.getAs[EntitlementValue]("e4").get,
          p_doc.getAs[EntitlementValue]("e5").get,
          p_doc.getAs[EntitlementValue]("e6").get,
          p_doc.getAs[EntitlementValue]("e7").get,
          p_doc.getAs[EntitlementValue]("e8").get,
          p_doc.getAs[EntitlementValue]("e9").get,
          p_doc.getAs[EntitlementValue]("e10").get
      )
    }
  }
  
  implicit object LeaveProfileCalculationBSONReader extends BSONDocumentReader[LeaveProfileCalculation] {
    def read(p_doc: BSONDocument): LeaveProfileCalculation = {
      LeaveProfileCalculation(
          p_doc.getAs[Int]("ent").get,
          p_doc.getAs[Double]("ear").get,
          p_doc.getAs[Double]("adj").get,
          p_doc.getAs[Double]("uti").get,
          p_doc.getAs[Double]("cf").get,
          p_doc.getAs[Double]("cfuti").get,
          p_doc.getAs[Double]("cfexp").get,
          p_doc.getAs[Double]("papr").get,
          p_doc.getAs[Double]("bal").get,
          p_doc.getAs[Double]("cbal").get
      )
    }
  }
  
  implicit object LeaveProfileBSONReader extends BSONDocumentReader[LeaveProfile] {
    def read(p_doc: BSONDocument): LeaveProfile = {
      LeaveProfile(
          p_doc.getAs[BSONObjectID]("_id").get,
          p_doc.getAs[String]("pid").get,
          p_doc.getAs[String]("pn").get,
          p_doc.getAs[String]("lt").get,
          p_doc.getAs[LeaveProfileCalculation]("cal").get,
          p_doc.getAs[LeaveProfileMonthEarn]("me").get,
          p_doc.getAs[Entitlement]("set_ent").get,
          p_doc.getAs[System]("sys").map(o => o)
      )
    }
  }
  
  // Use Writer to serialize document automatically
  implicit object SystemBSONWriter extends BSONDocumentWriter[System] {
    def write(p_doc: System): BSONDocument = {
      BSONDocument(
          "eid" -> p_doc.eid,
          "cdat" -> p_doc.cdat.map(date => BSONDateTime(date.getMillis)),
          "mdat" -> p_doc.mdat.map(date => BSONDateTime(date.getMillis)),
          "mby" -> p_doc.mby,
          "ddat" -> p_doc.ddat.map(date => BSONDateTime(date.getMillis)),
          "dby" -> p_doc.dby,
          "ll" -> p_doc.ll.map(date => BSONDateTime(date.getMillis))
      )     
    }
  }
  
  implicit object EntitlementValueBSONWriter extends BSONDocumentWriter[EntitlementValue] {
    def write(p_doc: EntitlementValue): BSONDocument = {
      BSONDocument(
          "e" -> p_doc.e,
          "s" -> p_doc.s,
          "cf" -> p_doc.cf
      )     
    }
  }
  
  implicit object EntitlementBSONWriter extends BSONDocumentWriter[Entitlement] {
    def write(p_doc: Entitlement): BSONDocument = {
      BSONDocument(
          "e1" -> p_doc.e1,
          "e2" -> p_doc.e2,
          "e3" -> p_doc.e3,
          "e4" -> p_doc.e4,
          "e5" -> p_doc.e5,
          "e6" -> p_doc.e6,
          "e7" -> p_doc.e7,
          "e8" -> p_doc.e8,
          "e9" -> p_doc.e9,
          "e10" -> p_doc.e10
      )     
    }
  }

  implicit object LeaveProfileMonthEarnBSONWriter extends BSONDocumentWriter[LeaveProfileMonthEarn] {
    def write(p_doc: LeaveProfileMonthEarn): BSONDocument = {
      BSONDocument(
          "jan" -> p_doc.jan,
          "feb" -> p_doc.feb,
          "mar" -> p_doc.mar,
          "apr" -> p_doc.apr,
          "may" -> p_doc.may,
          "jun" -> p_doc.jun,
          "jul" -> p_doc.jul,
          "aug" -> p_doc.aug,
          "sep" -> p_doc.sep,
          "oct" -> p_doc.oct,
          "nov" -> p_doc.nov,
          "dec" -> p_doc.dec
      )     
    }
  }
  
  implicit object LeaveProfileCalculationBSONWriter extends BSONDocumentWriter[LeaveProfileCalculation] {
    def write(p_doc: LeaveProfileCalculation): BSONDocument = {
      BSONDocument(
          "ent" -> p_doc.ent,
          "ear" -> p_doc.ear,
          "adj" -> p_doc.adj,
          "uti" -> p_doc.uti,
          "cf" -> p_doc.cf,
          "cfuti" -> p_doc.cfuti,
          "cfexp" -> p_doc.cfexp,
          "papr" -> p_doc.papr,
          "bal" -> p_doc.bal,
          "cbal" -> p_doc.cbal
      )     
    }
  }
      
  implicit object LeaveProfileBSONWriter extends BSONDocumentWriter[LeaveProfile] {
    def write(p_doc: LeaveProfile): BSONDocument = {
      BSONDocument(
          "_id" -> p_doc._id,
          "pid" -> p_doc.pid,
          "pn" -> p_doc.pn,
          "lt" -> p_doc.lt,
          "cal" -> p_doc.cal,
          "me" -> p_doc.me,
          "set_ent" -> p_doc.set_ent,
          "sys" -> p_doc.sys
      )     
    }
  }
  
  private val col = DbConnUtility.leave_db.collection("leaveprofile")
  
  val doc = LeaveProfile(
      _id = BSONObjectID.generate,
      pid = "",
      pn = "",
      lt = "",
      cal = LeaveProfileCalculation (ent = 0, ear = 0.0, adj = 0.0, uti = 0.0, cf = 0.0, cfuti = 0.0, cfexp = 0.0, papr = 0.0, bal = 0.0, cbal = 0.0),
      me = LeaveProfileMonthEarn(jan=0.0, feb=0.0, mar=0.0, apr=0.0, may=0.0, jun=0.0, jul=0.0, aug=0.0, sep = 0.0, oct=0.0, nov=0.0, dec=0.0),
      set_ent = Entitlement(
          e1=EntitlementValue(e=0, s=0, cf=0),
          e2=EntitlementValue(e=0, s=0, cf=0),
          e3=EntitlementValue(e=0, s=0, cf=0),
          e4=EntitlementValue(e=0, s=0, cf=0),
          e5=EntitlementValue(e=0, s=0, cf=0),
          e6=EntitlementValue(e=0, s=0, cf=0),
          e7=EntitlementValue(e=0, s=0, cf=0),
          e8=EntitlementValue(e=0, s=0, cf=0),
          e9=EntitlementValue(e=0, s=0, cf=0),
          e10=EntitlementValue(e=0, s=0, cf=0)  
      ),
      sys=None
  )
  
  private def updateSystem(p_doc:LeaveProfile) = {
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
    
  // Soft deletion by setting deletion flag in document
  def remove(p_query:BSONDocument, p_request:RequestHeader) = {
    for {
      docs <- this.find(p_query, p_request)
    } yield {
      docs.foreach { doc => 
        val future = col.update(BSONDocument("_id" -> doc._id, "sys.ddat"->BSONDocument("$exists"->false)), doc.copy(sys = SystemDataStore.setDeletionFlag(this.updateSystem(doc), p_request)))
        future.onComplete {
          case Failure(e) => throw e
          case Success(lastError) => {}
        }
      }
    }
  }
	
  // Delete document
  def removePermanently(p_query:BSONDocument) = {
    val future = col.remove(p_query)
  }
	
  // Find all documents
  def find(p_query:BSONDocument) = {
    col.find(p_query).cursor[LeaveProfile](ReadPreference.primary).collect[List]()
  }
  
  // Find all documents using session
  def find(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).cursor[LeaveProfile](ReadPreference.primary).collect[List]()
  }
  
  // Find and sort all documents using session
  def find(p_query:BSONDocument, p_sort:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).sort(p_sort).cursor[LeaveProfile](ReadPreference.primary).collect[List]()
  }
	
  // Find one document
  // Return the first found document
  def findOne(p_query:BSONDocument) = {
    col.find(p_query).one[LeaveProfile]
  }
  
  // Find one document using session
  // Return the first found document
  def findOne(p_query:BSONDocument, p_request:RequestHeader) = {
    col.find(p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false)))).one[LeaveProfile]
  }
  
  /** Custom Model Methods **/ 
  
  // Insert new document
  def insert(p_doc:LeaveProfile, p_eid:String="", p_request:RequestHeader=null)= {
    for {
      maybe_person <- if (p_eid=="") PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_doc.pid)), p_request) else PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_doc.pid), "sys.eid" -> p_eid))
      maybe_leavepolicy <- if (p_eid=="") LeavePolicyModel.findOne(BSONDocument("lt"->p_doc.lt), p_request) else LeavePolicyModel.findOne(BSONDocument("lt"->p_doc.lt, "sys.eid" -> p_eid))
      maybe_leavesetting <- if (p_eid=="") LeaveSettingModel.findOne(BSONDocument(), p_request) else LeaveSettingModel.findOne(BSONDocument("sys.eid" -> p_eid))
    } yield {
      val person = maybe_person.getOrElse(PersonModel.doc.copy(_id=BSONObjectID.generate))
      val leavepolicy= maybe_leavepolicy.getOrElse(LeavePolicyModel.doc)
      val leavesetting = maybe_leavesetting.getOrElse(LeaveSettingModel.doc)
      val cutoffdate = LeaveSettingModel.getCutOffDate(leavesetting.cfm)
      val previouscutoffdate = LeaveSettingModel.getPreviousCutOffDate(leavesetting.cfm)
      val leaveearned = leavepolicy.set.acc match {
        case "No accrue" => this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person))
        case "Monthly - utilisation based on earned" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to now.
            this.getTotalMonthlyEntitlementEarn(person.p.edat.get, p_doc, leavepolicy, leavesetting, person)
          } else {
            // Earn calculate from previous cut of date to now.
            this.getTotalMonthlyEntitlementEarn(previouscutoffdate, p_doc, leavepolicy, leavesetting, person)
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to now.
            this.getTotalMonthlyEntitlementEarn(person.p.edat.get, p_doc, leavepolicy, leavesetting, person)
          } else {
            // Earn calculate from previous cut of date to now.
            this.getTotalMonthlyEntitlementEarn(previouscutoffdate, p_doc, leavepolicy, leavesetting, person)
          }
        }
        case "Yearly" => {
          if (person.p.edat.get.isAfter(cutoffdate.minusMonths(1).dayOfMonth().withMaximumValue())) {
            0.0
          } else {
            this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person))
          }
        }
      }
      val balance = leaveearned + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
      val cbalance = leavepolicy.set.acc match {
        case "No accrue" => balance
        case "Monthly - utilisation based on earned" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to cut off.
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, person.p.edat.get, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
          } else {
            // Earn calculate from previous cut of date to cut off.
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, previouscutoffdate, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to cut off.
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, person.p.edat.get, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
          } else {
            // Earn calculate from previous cut of date to cut off.
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, previouscutoffdate, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
          }
        }
        case "Yearly" => balance
      }
      val future = col.insert(
          p_doc.copy(
              cal = p_doc.cal.copy(
                  ent = this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person)),
                  ear = leaveearned,
                  bal = BigDecimal(balance).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble,
                  cbal = BigDecimal(cbalance).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
              ),
              me = LeaveProfileMonthEarn(
                  jan = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 1),
                  feb = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 2),
                  mar = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 3),
                  apr = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 4),
                  may = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 5),
                  jun = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 6),
                  jul = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 7),
                  aug = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 8),
                  sep = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 9),
                  oct = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 10),
                  nov = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 11),
                  dec = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 12)
              ),
              sys = SystemDataStore.creation(p_eid,p_request)
          )
      )
      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {}
      }
    }
  }
  
  // Update document
  def update(p_query:BSONDocument,p_doc:LeaveProfile,p_request:RequestHeader) = {
    for {
      maybe_person <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_doc.pid)), p_request)
      maybe_leavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt"->p_doc.lt), p_request)
      maybe_leavesetting <- LeaveSettingModel.findOne(BSONDocument(), p_request)
    } yield {
      val person = maybe_person.getOrElse(PersonModel.doc.copy(_id=BSONObjectID.generate))
      val leavepolicy= maybe_leavepolicy.getOrElse(LeavePolicyModel.doc)
      val leavesetting = maybe_leavesetting.getOrElse(LeaveSettingModel.doc)
      val previouscutoffdate = LeaveSettingModel.getPreviousCutOffDate(leavesetting.cfm)
      val cutoffdate = LeaveSettingModel.getCutOffDate(leavesetting.cfm)
      val leaveearned = leavepolicy.set.acc match {
        case "No accrue" => p_doc.cal.ear
        case "Monthly - utilisation based on earned" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to now.
            this.getTotalMonthlyEntitlementEarn(person.p.edat.get, p_doc, leavepolicy, leavesetting, person)
          } else {
            // Earn calculate from previous cut of date to now.
            this.getTotalMonthlyEntitlementEarn(previouscutoffdate, p_doc, leavepolicy, leavesetting, person)
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to now.
            this.getTotalMonthlyEntitlementEarn(person.p.edat.get, p_doc, leavepolicy, leavesetting, person)
          } else {
            // Earn calculate from previous cut of date to now.
            this.getTotalMonthlyEntitlementEarn(previouscutoffdate, p_doc, leavepolicy, leavesetting, person)
          }
        }
        case "Yearly" => {
          if (person.p.edat.get.isAfter(cutoffdate.minusMonths(1).dayOfMonth().withMaximumValue())) {
            0.0
          } else {
            this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person))
          }
        }
      }
      val balance = leaveearned + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
      val cbalance = leavepolicy.set.acc match {
        case "No accrue" => balance
        case "Monthly - utilisation based on earned" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, person.p.edat.get, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
          } else {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, previouscutoffdate, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, person.p.edat.get, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
          } else {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, previouscutoffdate, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
          }
        }
        case "Yearly" => balance
      }
      val future = col.update(
          p_query.++(BSONDocument("sys.eid" -> p_request.session.get("entity").get, "sys.ddat"->BSONDocument("$exists"->false))), 
          p_doc.copy(
              cal = p_doc.cal.copy(
                  ent = this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person)),
                  ear = leaveearned,
                  bal = BigDecimal(balance).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble,
                  cbal = BigDecimal(cbalance).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
              ), 
              me = LeaveProfileMonthEarn(
                  jan = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 1),
                  feb = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 2),
                  mar = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 3),
                  apr = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 4),
                  may = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 5),
                  jun = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 6),
                  jul = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 7),
                  aug = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 8),
                  sep = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 9),
                  oct = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 10),
                  nov = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 11),
                  dec = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 12)
              ),
              sys = SystemDataStore.modifyWithSystem(this.updateSystem(p_doc), p_request)
          )
      )
      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {}
      }
    }
  }
  
  // Update document
  def update(p_query:BSONDocument, p_doc:LeaveProfile, p_eid:String) = {
    for {
      maybe_person <- PersonModel.findOne(BSONDocument("_id"->BSONObjectID(p_doc.pid), "sys.eid"->p_eid))
      maybe_leavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt"->p_doc.lt, "sys.eid"->p_eid))
      maybe_leavesetting <- LeaveSettingModel.findOne(BSONDocument("sys.eid"->p_eid))
    } yield {
      val person = maybe_person.getOrElse(PersonModel.doc.copy(_id=BSONObjectID.generate))
      val leavepolicy= maybe_leavepolicy.getOrElse(LeavePolicyModel.doc)
      val leavesetting = maybe_leavesetting.getOrElse(LeaveSettingModel.doc)
      val previouscutoffdate = LeaveSettingModel.getPreviousCutOffDate(leavesetting.cfm)
      val cutoffdate = LeaveSettingModel.getCutOffDate(leavesetting.cfm)
      val leaveearned = leavepolicy.set.acc match {
        case "No accrue" => p_doc.cal.ear
        case "Monthly - utilisation based on earned" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to now.
            this.getTotalMonthlyEntitlementEarn(person.p.edat.get, p_doc, leavepolicy, leavesetting, person)
          } else {
            // Earn calculate from previous cut of date to now.
            this.getTotalMonthlyEntitlementEarn(previouscutoffdate, p_doc, leavepolicy, leavesetting, person)
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to now.
            this.getTotalMonthlyEntitlementEarn(person.p.edat.get, p_doc, leavepolicy, leavesetting, person)
          } else {
            // Earn calculate from previous cut of date to now.
            this.getTotalMonthlyEntitlementEarn(previouscutoffdate, p_doc, leavepolicy, leavesetting, person)
          }
        }
        case "Yearly" => {
          if (person.p.edat.get.isAfter(cutoffdate.minusMonths(1).dayOfMonth().withMaximumValue())) {
            0.0
          } else {
            this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person))
          }
        }
      }
      val balance = leaveearned + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
      val cbalance = leavepolicy.set.acc match {
        case "No accrue" => balance
        case "Monthly - utilisation based on earned" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, person.p.edat.get, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
          } else {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, previouscutoffdate, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, person.p.edat.get, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
          } else {
            this.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, previouscutoffdate, p_doc, leavepolicy, leavesetting, person) + p_doc.cal.adj - p_doc.cal.uti + p_doc.cal.cf - p_doc.cal.cfuti - p_doc.cal.cfexp - p_doc.cal.papr
          }
        }
        case "Yearly" => balance
      }
      val future = col.update(
          p_query.++(BSONDocument("sys.eid" -> p_eid, "sys.ddat"->BSONDocument("$exists"->false))), 
          p_doc.copy(
              cal = p_doc.cal.copy(
                  ent = this.getEligibleEntitlement(p_doc, PersonModel.getServiceMonths(person)),
                  ear = leaveearned,
                  bal = BigDecimal(balance).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble,
                  cbal = BigDecimal(cbalance).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
              ),
              me = LeaveProfileMonthEarn(
                  jan = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 1),
                  feb = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 2),
                  mar = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 3),
                  apr = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 4),
                  may = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 5),
                  jun = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 6),
                  jul = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 7),
                  aug = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 8),
                  sep = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 9),
                  oct = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 10),
                  nov = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 11),
                  dec = this.getMonthEntitlementEarn(p_doc, leavepolicy, leavesetting, person, 12)
              ),
              sys = SystemDataStore.modifyWithSystem(this.updateSystem(p_doc))
          )
      )
      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {}
      }
    }
  }
  
  def isUnique(p_doc:LeaveProfile, p_request:RequestHeader) = {
    for{
      maybe_leavepolicy <- this.findOne(BSONDocument("pid" ->p_doc.pid, "lt"->p_doc.lt), p_request)
    } yield {
      if (maybe_leavepolicy.isEmpty) true else false
    }
  }
  
  def sortEligbleLeaveEntitlement(p_doc:LeaveProfile, p_request:RequestHeader) = {
        
    var eligbleleaveentitlement = ArrayBuffer.fill(10,3)(0)
    
    // Replace 0 value to 1000
    if (p_doc.set_ent.e1.s == 0) {
      eligbleleaveentitlement(0) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(0) = ArrayBuffer(p_doc.set_ent.e1.s, p_doc.set_ent.e1.e, p_doc.set_ent.e1.cf)
    }

    if (p_doc.set_ent.e2.s == 0) {
      eligbleleaveentitlement(1) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(1) = ArrayBuffer(p_doc.set_ent.e2.s, p_doc.set_ent.e2.e, p_doc.set_ent.e2.cf)
    }
    
    if (p_doc.set_ent.e3.s == 0) {
      eligbleleaveentitlement(2) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(2) = ArrayBuffer(p_doc.set_ent.e3.s, p_doc.set_ent.e3.e, p_doc.set_ent.e3.cf)
    }
    
    if (p_doc.set_ent.e4.s == 0) {
    	eligbleleaveentitlement(3) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(3) = ArrayBuffer(p_doc.set_ent.e4.s, p_doc.set_ent.e4.e, p_doc.set_ent.e4.cf)
    }
    
    if (p_doc.set_ent.e5.s == 0) {
      eligbleleaveentitlement(4) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(4) = ArrayBuffer(p_doc.set_ent.e5.s, p_doc.set_ent.e5.e, p_doc.set_ent.e5.cf)
    }
    
    if (p_doc.set_ent.e6.s == 0) {
      eligbleleaveentitlement(5) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(5) = ArrayBuffer(p_doc.set_ent.e6.s, p_doc.set_ent.e6.e, p_doc.set_ent.e6.cf)
    }
    
    if (p_doc.set_ent.e7.s == 0) {
      eligbleleaveentitlement(6) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(6) = ArrayBuffer(p_doc.set_ent.e7.s, p_doc.set_ent.e7.e, p_doc.set_ent.e7.cf)
    }
    
    if (p_doc.set_ent.e8.s == 0) {
      eligbleleaveentitlement(7) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(7) = ArrayBuffer(p_doc.set_ent.e8.s, p_doc.set_ent.e8.e, p_doc.set_ent.e8.cf)
    }
    
    if (p_doc.set_ent.e9.s == 0) {
      eligbleleaveentitlement(8) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(8) = ArrayBuffer(p_doc.set_ent.e9.s, p_doc.set_ent.e9.e, p_doc.set_ent.e9.cf)
    }
    
    if (p_doc.set_ent.e10.s == 0) {
      eligbleleaveentitlement(9) = ArrayBuffer(1000, 1000, 1000)
    } else {
      eligbleleaveentitlement(9) = ArrayBuffer(p_doc.set_ent.e10.s, p_doc.set_ent.e10.e, p_doc.set_ent.e10.cf)
    }
    
    val eligbleleaveentitlementsorted = eligbleleaveentitlement.sortBy(_(0))
    var eligbleleaveentitlementsorted_update = ArrayBuffer.fill(10,3)(0)
    
    // Replace 1000 back to 0
    for (i <- 0 to 9) {
      if (eligbleleaveentitlementsorted(i)(0) == 1000) {
        eligbleleaveentitlementsorted_update(i) = ArrayBuffer(0, 0, 0)
      } else {
        eligbleleaveentitlementsorted_update(i) = ArrayBuffer(eligbleleaveentitlementsorted(i)(0), eligbleleaveentitlementsorted(i)(1), eligbleleaveentitlementsorted(i)(2))
      }
    }
    
    eligbleleaveentitlementsorted_update
  }
  
  // Get entitlement using leave profile
  // For update leave profile
  def getEligibleEntitlement(p_doc:LeaveProfile, p_servicemonth:Int) = { 
    p_servicemonth match {
      // case servicemonth if servicemonth < 0 => 0
      case servicemonth if servicemonth < p_doc.set_ent.e1.s => p_doc.set_ent.e1.e
      case servicemonth if servicemonth <= p_doc.set_ent.e2.s => p_doc.set_ent.e2.e
      case servicemonth if servicemonth <= p_doc.set_ent.e3.s => p_doc.set_ent.e3.e
      case servicemonth if servicemonth <= p_doc.set_ent.e4.s => p_doc.set_ent.e4.e
      case servicemonth if servicemonth <= p_doc.set_ent.e5.s => p_doc.set_ent.e5.e
      case servicemonth if servicemonth <= p_doc.set_ent.e6.s => p_doc.set_ent.e6.e
      case servicemonth if servicemonth <= p_doc.set_ent.e7.s => p_doc.set_ent.e7.e
      case servicemonth if servicemonth <= p_doc.set_ent.e8.s => p_doc.set_ent.e8.e
      case servicemonth if servicemonth <= p_doc.set_ent.e9.s => p_doc.set_ent.e9.e
      case servicemonth if servicemonth <= p_doc.set_ent.e10.s => p_doc.set_ent.e10.e
      case _ => 0
    }
  }
  
  // Get entitlement using leave policy
  // For new leave profile
  def getEligibleEntitlement(p_leavepolicy:LeavePolicy, p_servicemonth:Int) = { 
    p_servicemonth match {
      // case servicemonth if servicemonth < 0 => 0
      case servicemonth if servicemonth < p_leavepolicy.ent.e1.s => p_leavepolicy.ent.e1.e
      case servicemonth if servicemonth <= p_leavepolicy.ent.e2.s => p_leavepolicy.ent.e2.e
      case servicemonth if servicemonth <= p_leavepolicy.ent.e3.s => p_leavepolicy.ent.e3.e
      case servicemonth if servicemonth <= p_leavepolicy.ent.e4.s => p_leavepolicy.ent.e4.e
      case servicemonth if servicemonth <= p_leavepolicy.ent.e5.s => p_leavepolicy.ent.e5.e
      case servicemonth if servicemonth <= p_leavepolicy.ent.e6.s => p_leavepolicy.ent.e6.e
      case servicemonth if servicemonth <= p_leavepolicy.ent.e7.s => p_leavepolicy.ent.e7.e
      case servicemonth if servicemonth <= p_leavepolicy.ent.e8.s => p_leavepolicy.ent.e8.e
      case servicemonth if servicemonth <= p_leavepolicy.ent.e9.s => p_leavepolicy.ent.e9.e
      case servicemonth if servicemonth <= p_leavepolicy.ent.e10.s => p_leavepolicy.ent.e10.e
      case _ => 0
    }
  }
  
  def getEligibleCarryForword(p_doc:LeaveProfile, p_servicemonth:Int) = {
    p_servicemonth match {
      // case servicemonth if servicemonth < 0 => 0
      case servicemonth if servicemonth <= p_doc.set_ent.e1.s => p_doc.set_ent.e1.cf 
      case servicemonth if servicemonth <= p_doc.set_ent.e2.s => p_doc.set_ent.e2.cf
      case servicemonth if servicemonth <= p_doc.set_ent.e3.s => p_doc.set_ent.e3.cf
      case servicemonth if servicemonth <= p_doc.set_ent.e4.s => p_doc.set_ent.e4.cf
      case servicemonth if servicemonth <= p_doc.set_ent.e5.s => p_doc.set_ent.e5.cf
      case servicemonth if servicemonth <= p_doc.set_ent.e6.s => p_doc.set_ent.e6.cf
      case servicemonth if servicemonth <= p_doc.set_ent.e7.s => p_doc.set_ent.e7.cf
      case servicemonth if servicemonth <= p_doc.set_ent.e8.s => p_doc.set_ent.e8.cf
      case servicemonth if servicemonth <= p_doc.set_ent.e9.s => p_doc.set_ent.e9.cf
      case servicemonth if servicemonth <= p_doc.set_ent.e10.s => p_doc.set_ent.e10.cf
      case _ => 0
    }
  }
  
  def getEligibleCarryForwordEarn(p_leaveprofile:LeaveProfile, p_servicemonth:Int) = {
    val eligblecarryforword = this.getEligibleCarryForword(p_leaveprofile, p_servicemonth)
    val carryforward = if (p_leaveprofile.cal.cbal > 0) p_leaveprofile.cal.cbal else 0
    if (carryforward > eligblecarryforword) eligblecarryforword else carryforward
  }
  
  // Get total leave earn from previous cut off date or employee start date until cut off date.
  // Get entitlement using leave policy
  // For update leave profile
  def getTotalMonthlyEntitlementEarnUntilCutOff(p_cutoffdate:DateTime, p_date:DateTime, p_leaveprofile:LeaveProfile, p_leavepolicy:LeavePolicy, p_leavesetting:LeaveSetting, p_person:Person):Double = {
    val earned = if (p_date.isAfter(p_cutoffdate.minusMonths(2).dayOfMonth().withMaximumValue())) {
      this.getMonthEntitlementEarn(p_leaveprofile, p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear)
    } else {
      this.getMonthEntitlementEarn(p_leaveprofile, p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear) + getTotalMonthlyEntitlementEarnUntilCutOff(p_cutoffdate, p_date.plusMonths(1), p_leaveprofile, p_leavepolicy, p_leavesetting, p_person)
    }
    BigDecimal(earned).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
  
  // Get total leave earn from previous cut off date or employee start date until cut off date.
  // Get entitlement using leave policy
  // For new leave profile
  def getTotalMonthlyEntitlementEarnUntilCutOff(p_cutoffdate:DateTime, p_date:DateTime, p_leavepolicy:LeavePolicy, p_leavesetting:LeaveSetting, p_person:Person):Double = {
    val earned = if (p_date.isAfter(p_cutoffdate.minusMonths(2).dayOfMonth().withMaximumValue())) {
      this.getMonthEntitlementEarn(p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear)
    } else {
      this.getMonthEntitlementEarn(p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear) + getTotalMonthlyEntitlementEarnUntilCutOff(p_cutoffdate, p_date.plusMonths(1), p_leavepolicy, p_leavesetting, p_person)
    }
    BigDecimal(earned).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
    
  // Get total leave earn from given parameter date until now.
  // Get entitlement using leave profile
  // For update leave profile
  def getTotalMonthlyEntitlementEarn(p_date:DateTime, p_leaveprofile:LeaveProfile, p_leavepolicy:LeavePolicy, p_leavesetting:LeaveSetting, p_person:Person):Double = {
    val earned = if (p_person.p.edat.get.dayOfMonth().withMaximumValue().isAfter(DateTime.now().dayOfMonth().withMaximumValue())) {
      0.0
    } else if (p_date.isAfter(DateTime.now().minusMonths(1).dayOfMonth().withMaximumValue())) {
      this.getMonthEntitlementEarn(p_leaveprofile, p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear)
    } else {
      this.getMonthEntitlementEarn(p_leaveprofile, p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear) + getTotalMonthlyEntitlementEarn(p_date.plusMonths(1), p_leaveprofile, p_leavepolicy, p_leavesetting, p_person)
    }
    BigDecimal(earned).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
      
  // Get total leave earn from given parameter date until now.
  // Get entitlement using leave policy
  // For new leave profile
  def getTotalMonthlyEntitlementEarn(p_date:DateTime, p_leavepolicy:LeavePolicy, p_leavesetting:LeaveSetting, p_person:Person):Double = {
    val earned = if (p_person.p.edat.get.dayOfMonth().withMaximumValue().isAfter(DateTime.now().dayOfMonth().withMaximumValue())) {
      0.0
    } else if (p_date.isAfter(DateTime.now().minusMonths(1).dayOfMonth().withMaximumValue())) {
      this.getMonthEntitlementEarn(p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear)
    } else {
      this.getMonthEntitlementEarn(p_leavepolicy, p_leavesetting, p_person, p_date.getMonthOfYear) + getTotalMonthlyEntitlementEarn(p_date.plusMonths(1), p_leavepolicy, p_leavesetting, p_person)
    }
    BigDecimal(earned).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
   
  // Get entitlement using leave profile
  // Entitlement is 0 when employee join after cut of month.
  // For update leave profile
  def getMonthEntitlementEarn(p_leaveprofile:LeaveProfile, p_leavepolicy:LeavePolicy, p_leavesetting:LeaveSetting, p_person:Person, p_month:Int) = {
    val cutoffmonth = p_leavesetting.cfm
    val nextcutofdate = LeaveSettingModel.getCutOffDate(p_leavesetting.cfm)
    val previouscutofdate = LeaveSettingModel.getPreviousCutOffDate(p_leavesetting.cfm)
    val calculate_date = if (cutoffmonth <= p_month) { previouscutofdate.plusMonths(p_month - cutoffmonth) } else { nextcutofdate.minusMonths(cutoffmonth - p_month) }
    val accruetype = p_leavepolicy.set.acc
    if (p_person.p.edat.get.isAfter(calculate_date.dayOfMonth().withMaximumValue()) && accruetype!="Yearly") {
      // Entitlement is 0 when employee join after cut of month.
      0.0      
    } else if (p_person.p.edat.get.isAfter(nextcutofdate.minusMonths(1).dayOfMonth().withMaximumValue()) && accruetype=="Yearly") {
      0.0
    } else {
      val servicemonth = PersonModel.getServiceMonths(p_person)
      val entitlement = this.getEligibleEntitlement(p_leaveprofile, servicemonth)
      val leavecutoff_mth = p_leavesetting.cfm
      accruetype match {
        case "No accrue" => 0.0
        case "Monthly - utilisation based on earned" => {
          val leavecutoff_lastmth = if(leavecutoff_mth == 1) { 12 } else { leavecutoff_mth - 1 }
          val entitlementindouble = entitlement.toDouble
          if(leavecutoff_lastmth == p_month) {
            val mthearn = entitlementindouble - (BigDecimal(entitlementindouble / 12)).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble * 11
            BigDecimal(mthearn).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
          } else {
            val mthearn = entitlementindouble / 12
            BigDecimal(mthearn).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          val leavecutoff_lastmth = if(leavecutoff_mth == 1) { 12 } else { leavecutoff_mth - 1 }
          val entitlementindouble = entitlement.toDouble
          if(leavecutoff_lastmth == p_month) {
            val mthearn = entitlementindouble - (BigDecimal(entitlementindouble / 12)).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble * 11
            BigDecimal(mthearn).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
          } else {
            val mthearn = entitlementindouble / 12
            BigDecimal(mthearn).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
          }
        }
        case "Yearly" => if(leavecutoff_mth == p_month) { entitlement.toDouble } else { 0.0 }
      }  
    }
  }
    
  // Get entitlement using leave policy
  // For new leave profile  
  def getMonthEntitlementEarn(p_leavepolicy:LeavePolicy, p_leavesetting:LeaveSetting, p_person:Person, p_month:Int) = {
    val cutoffmonth = p_leavesetting.cfm
    val nextcutofdate = LeaveSettingModel.getCutOffDate(p_leavesetting.cfm)
    val previouscutofdate = LeaveSettingModel.getPreviousCutOffDate(p_leavesetting.cfm)
    val calculate_date = if (cutoffmonth <= p_month) { previouscutofdate.plusMonths(p_month - cutoffmonth) } else { nextcutofdate.minusMonths(cutoffmonth - p_month) }
    val accruetype = p_leavepolicy.set.acc
    if (p_person.p.edat.get.isAfter(calculate_date.dayOfMonth().withMaximumValue()) && accruetype!="Yearly") {
      // Entitlement is 0 when employee join after cut of month.
      0.0      
    } else if (p_person.p.edat.get.isAfter(nextcutofdate.minusMonths(1).dayOfMonth().withMaximumValue()) && accruetype=="Yearly") {
      0.0
    } else {
      val servicemonth = PersonModel.getServiceMonths(p_person)
      val entitlement = this.getEligibleEntitlement(p_leavepolicy, servicemonth)
      val leavecutoff_mth = p_leavesetting.cfm
      accruetype match {
        case "No accrue" => 0.0
        case "Monthly - utilisation based on earned" => {
          val leavecutoff_lastmth = if(leavecutoff_mth == 1) { 12 } else { leavecutoff_mth - 1 }
          val entitlementindouble = entitlement.toDouble
          if(leavecutoff_lastmth == p_month) {
            val mthearn = entitlementindouble - (BigDecimal(entitlementindouble / 12)).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble * 11
            BigDecimal(mthearn).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
          } else {
            val mthearn = entitlementindouble / 12
            BigDecimal(mthearn).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          val leavecutoff_lastmth = if(leavecutoff_mth == 1) { 12 } else { leavecutoff_mth - 1 }
          val entitlementindouble = entitlement.toDouble
          if(leavecutoff_lastmth == p_month) {
            val mthearn = entitlementindouble - (BigDecimal(entitlementindouble / 12)).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble * 11
            BigDecimal(mthearn).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
          } else {
            val mthearn = entitlementindouble / 12
            BigDecimal(mthearn).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
          }
        }
        case "Yearly" => if(leavecutoff_mth == p_month) { entitlement.toDouble } else { 0.0 }
      }    
    }  
  }
  
  def getEligibleCarryForwardExpired(p_leaveprofile: LeaveProfile, p_leavesetting: LeaveSetting, p_leavepolicy: LeavePolicy) = {
    if (p_leavepolicy.set.cexp == 0) {
      0.0
    } else {
      val now = new DateTime
      val cutoffdate = LeaveSettingModel.getPreviousCutOffDate(p_leavesetting.cfm)
      val carryforwardexpireddate = cutoffdate.plusMonths(p_leavepolicy.set.cexp)
      if (now.isAfter(carryforwardexpireddate)) p_leaveprofile.cal.cf - p_leaveprofile.cal.cfuti else 0.0
    }
  }
    
  def getLeaveTypes(p_pid:String, p_request:RequestHeader) = {
    for {
      maybe_leavetypes <- this.find(BSONDocument("pid" -> p_pid), p_request)
    } yield {
      var typesList = List[String]()
      maybe_leavetypes.foreach(leavetype => typesList = typesList :+ leavetype.lt)
      typesList
    }
  }
  
  def getLeaveTypesSelection(p_pid:String, p_request:RequestHeader) = {
    for {
      leavetypes <- this.find(BSONDocument("pid" -> p_pid), BSONDocument("lt" -> 1), p_request)
    } yield {
      buildLeaveTypesSelection(leavetypes, Map[String, String](), p_request)
    }
  }
  
  // Recursive function to build Leave Type
  private def buildLeaveTypesSelection(leavetypes:List[LeaveProfile], leavetypesselection:Map[String, String], p_request:RequestHeader):Map[String, String] = {
    if (leavetypes.isEmpty) {
      leavetypesselection
    } else {
      val leavetype = leavetypes.head
      val leavepolicy = Await.result(LeavePolicyModel.findOne(BSONDocument("lt" -> leavetype.lt), p_request), Tools.db_timeout)
      val leavebalance = if (leavepolicy.get.set.acc == "Monthly - utilisation based on earned") { leavetype.cal.bal } else { leavetype.cal.cbal }
      buildLeaveTypesSelection(leavetypes.tail, leavetypesselection.++(Map(leavetype.lt -> (leavetype.lt + " (" + leavebalance.toString() + " days available)"))), p_request)
    }
  }
  
  // Notes:
  // 1 p_modifier format: 
  //   1.1 Replace - BSONDocument
  //   1.2 Update certain field - BSONDocument("$set"->BSONDocument("[FIELD]"->VALUE))
  // 2 No SystemDataStore update
  def updateUsingBSON(p_query:BSONDocument,p_modifier:BSONDocument) = {
    val future = col.update(selector=p_query, update=p_modifier, multi=true)
    future.onComplete {
      case Failure(e) => throw e
      case Success(lastError) => {}
    }
  }
  
  // Soft deletion by setting deletion flag in document on leave profile and audit log.
  def removeWithAuditLog(p_query:BSONDocument, p_request:RequestHeader) = {
    for {
      docs <- this.find(p_query, p_request)
    } yield {
      docs.map { doc => 
        val future = col.update(BSONDocument("_id" -> doc._id, "sys.ddat"->BSONDocument("$exists"->false)), doc.copy(sys = SystemDataStore.setDeletionFlag(this.updateSystem(doc), p_request)))
        AuditLogModel.remove(BSONDocument("lk"->doc._id.stringify), p_request)
        future.onComplete {
          case Failure(e) => throw e
          case Success(lastError) => {}
        }
      }
    }
  }
  
}