package controllers

import scala.util.{Success, Failure,Try,Random}
import scala.concurrent.{Future, Await}
import org.joda.time.DateTime

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import models.{LeaveProfileModel, LeaveProfile, LeaveProfileMonthEarn, LeaveProfileCalculation, Entitlement, EntitlementValue, LeavePolicyModel, PersonModel, LeaveModel, LeaveSettingModel, AuditLogModel}
import utilities.{System, AlertUtility, Tools}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument,BSONArray}

case class LeaveProfileScope(n: String)

class LeaveProfileController extends Controller with Secured {
  
  val leaveprofileform = Form(
      mapping(
          "_id" -> ignored(BSONObjectID.generate: BSONObjectID),
          "pid" -> text,
          "pn" -> text,
          "lt" -> text,
          "cal" -> mapping(
              "ent" -> number,
              "ear" -> of[Double],
              "adj" -> of[Double],
              "uti" -> of[Double],
              "cf" -> of[Double],
              "cfuti" -> of[Double],
              "cfexp" -> of[Double],
              "papr" -> of[Double],
              "bal" -> of[Double],
              "cbal" -> of[Double]
          )(LeaveProfileCalculation.apply)(LeaveProfileCalculation.unapply),
          "me" -> mapping(
              "jan" -> of[Double],
              "feb" -> of[Double],
              "mar" -> of[Double],
              "apr" -> of[Double],
              "may" -> of[Double],
              "jun" -> of[Double],
              "jul" -> of[Double],
              "aug" -> of[Double],
              "sep" -> of[Double],
              "oct" -> of[Double],
              "nov" -> of[Double],
              "dec" -> of[Double]
          )(LeaveProfileMonthEarn.apply)(LeaveProfileMonthEarn.unapply),
          "set_ent" -> mapping(
              "e1" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e2" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e3" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e4" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e5" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e6" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e7" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e8" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e9" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply),
              "e10" -> mapping(
                  "e" -> number,
                  "s" -> number,
                  "cf" -> number
              )(EntitlementValue.apply)(EntitlementValue.unapply)
          )(Entitlement.apply)(Entitlement.unapply),
          "sys" -> optional(mapping(
                  "eid" -> optional(text),
                  "cdat" -> optional(jodaDate),
                  "mdat" -> optional(jodaDate),
                  "mby" -> optional(text),
                  "ddat" -> optional(jodaDate),
                  "dby" -> optional(text),
                  "ll" -> optional(jodaDate)
          )(System.apply)(System.unapply))
      ){(_id,pid,pn,lt,cal,me,set_ent,sys)=>LeaveProfile(_id,pid,pn,lt,cal,me,set_ent,sys)}
      {leaveprofile:LeaveProfile=>
        Some(leaveprofile._id,
            leaveprofile.pid,
            leaveprofile.pn,
            leaveprofile.lt,
            leaveprofile.cal,
            leaveprofile.me,
            leaveprofile.set_ent,
            leaveprofile.sys)      
      }
  )
  
  implicit val leaveprofilescopeWrites = new Writes[LeaveProfileScope] {
    def writes(leaveprofilescope: LeaveProfileScope) = Json.obj(
        "n" -> leaveprofilescope.n
    )
  }
  
  def create(p_pid:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_pid)), request) 
        leavepolicies <- {
          LeavePolicyModel.getLeavePolicies(maybeperson.get.p.g + " only", maybeperson.get.p.ms + " only", request)
        }
      } yield {
        maybeperson.map( person => {
          val leaveprofile_doc = LeaveProfileModel.doc.copy(
              pid = p_pid,
              pn = person.p.fn + " " + person.p.ln
          )
          Ok(views.html.leaveprofile.form(leaveprofileform.fill(leaveprofile_doc), leavepolicies, p_pid))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def insert(p_pid:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      leaveprofileform.bindFromRequest.fold(
          formWithError => {
            for {
              maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_pid)), request) 
              leavepolicies <- {
                LeavePolicyModel.getLeavePolicies(maybeperson.get.p.g + " only", maybeperson.get.p.ms + " only", request)
              }
            } yield {
              maybeperson.map( person => {
                Ok(views.html.leaveprofile.form(formWithError, leavepolicies, p_pid))
              }).getOrElse(NotFound)
            }
          },
          formWithData => {
            for {
              leaveprofileunique <- LeaveProfileModel.isUnique(formWithData, request)
              maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_pid)), request)
              leavepolicies <- {
                LeavePolicyModel.getLeavePolicies(maybeperson.get.p.g + " only", maybeperson.get.p.ms + " only", request)
              }
              maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid" ->formWithData.pid, "lt"->formWithData.lt), request)
              maybe_alert <- AlertUtility.findOne(BSONDocument("k"->1005))
            } yield {
              if (leaveprofileunique) {
                val doc_objectID = BSONObjectID.generate
                val eligbleleaveentitlement = LeaveProfileModel.sortEligbleLeaveEntitlement(formWithData, request)
                Await.result(
                  LeaveProfileModel.insert(                  
                      formWithData.copy(
                          _id=doc_objectID, 
                          set_ent=Entitlement(
                              EntitlementValue(s=eligbleleaveentitlement(0)(0), e=eligbleleaveentitlement(0)(1), cf=eligbleleaveentitlement(0)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(1)(0), e=eligbleleaveentitlement(1)(1), cf=eligbleleaveentitlement(1)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(2)(0), e=eligbleleaveentitlement(2)(1), cf=eligbleleaveentitlement(2)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(3)(0), e=eligbleleaveentitlement(3)(1), cf=eligbleleaveentitlement(3)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(4)(0), e=eligbleleaveentitlement(4)(1), cf=eligbleleaveentitlement(4)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(5)(0), e=eligbleleaveentitlement(5)(1), cf=eligbleleaveentitlement(5)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(6)(0), e=eligbleleaveentitlement(6)(1), cf=eligbleleaveentitlement(6)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(7)(0), e=eligbleleaveentitlement(7)(1), cf=eligbleleaveentitlement(7)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(8)(0), e=eligbleleaveentitlement(8)(1), cf=eligbleleaveentitlement(8)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(9)(0), e=eligbleleaveentitlement(9)(1), cf=eligbleleaveentitlement(9)(2))
                          )
                      ),
                      p_request=request),
                  Tools.db_timeout
                )
                AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=doc_objectID.stringify, c="Create document."), p_request=request)
                Redirect(routes.PersonController.view(p_pid))
              } else {
                val replaceMap = Map(
                    "NAME"-> (maybeperson.get.p.fn + " " + maybeperson.get.p.ln),
                    "LEAVEPROFILE"-> formWithData.lt,
                    "URL"->(Tools.hostname+"/leaveprofile/view?p_id=" + maybeleaveprofile.get._id.stringify)
                )
                val alert = if ((maybe_alert.getOrElse(null))!=null) { maybe_alert.get.copy(m=Tools.replaceSubString(maybe_alert.get.m, replaceMap.toList)) } else { null }
                maybeperson.map( person => {
                  Ok(views.html.leaveprofile.form(leaveprofileform.fill(formWithData), leavepolicies, p_pid, "", alert))
                }).getOrElse(NotFound)
              }
            }
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def edit(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
        maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleaveprofile.get.pid)), request)
      } yield {
        maybeleaveprofile.map( leaveprofile => {
            Ok(views.html.leaveprofile.form(leaveprofileform.fill(leaveprofile), List(), leaveprofile.pid, p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def update(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      leaveprofileform.bindFromRequest.fold(
          formWithError => {
            for {
              maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
              maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleaveprofile.get.pid)), request) 
            } yield {
              Ok(views.html.leaveprofile.form(formWithError, List(), maybeleaveprofile.get.pid, p_id))
            }
          },
          formWithData => {
            val eligbleleaveentitlement = LeaveProfileModel.sortEligbleLeaveEntitlement(formWithData, request)
            Await.result(
              LeaveProfileModel.update(
                  BSONDocument("_id" -> BSONObjectID(p_id)), 
                  formWithData.copy(                      
                      _id=BSONObjectID(p_id), 
                      set_ent=Entitlement(
                          EntitlementValue(s=eligbleleaveentitlement(0)(0), e=eligbleleaveentitlement(0)(1), cf=eligbleleaveentitlement(0)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(1)(0), e=eligbleleaveentitlement(1)(1), cf=eligbleleaveentitlement(1)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(2)(0), e=eligbleleaveentitlement(2)(1), cf=eligbleleaveentitlement(2)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(3)(0), e=eligbleleaveentitlement(3)(1), cf=eligbleleaveentitlement(3)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(4)(0), e=eligbleleaveentitlement(4)(1), cf=eligbleleaveentitlement(4)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(5)(0), e=eligbleleaveentitlement(5)(1), cf=eligbleleaveentitlement(5)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(6)(0), e=eligbleleaveentitlement(6)(1), cf=eligbleleaveentitlement(6)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(7)(0), e=eligbleleaveentitlement(7)(1), cf=eligbleleaveentitlement(7)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(8)(0), e=eligbleleaveentitlement(8)(1), cf=eligbleleaveentitlement(8)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(9)(0), e=eligbleleaveentitlement(9)(1), cf=eligbleleaveentitlement(9)(2))
                      )
                  ), 
                  request),
                Tools.db_timeout
            )
            AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Modify document."), p_request=request)
            Future.successful(Redirect(routes.PersonController.view(formWithData.pid)))
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def delete(p_id:String, p_lt:String, p_pid:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      Await.result(LeaveProfileModel.remove(BSONDocument("_id" -> BSONObjectID(p_id)), request), Tools.db_timeout)
      LeaveModel.setLockDown(BSONDocument("pid" -> p_pid, "lt" -> p_lt), request)
      AuditLogModel.remove(BSONDocument("lk"->p_id), request)
      Future.successful(Redirect(routes.PersonController.view(p_pid)))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def view(p_id:String) = withAuth { username => implicit request => {
    for {
      maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleaveprofile.get.pid)), request)
    } yield {
      if(request.session.get("roles").get.contains("Admin") | request.session.get("id").get == maybeperson.get._id.stringify | request.session.get("id").get == maybeperson.get.p.mgrid | request.session.get("id").get == maybeperson.get.p.smgrid){
        maybeleaveprofile.map( leaveprofile => {
          Ok(views.html.leaveprofile.view(leaveprofile))
        }).getOrElse(NotFound)
      } else {
        Ok(views.html.error.unauthorized())
      }
    }
  }}
  
  def myprofilecreate = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request) 
        leavepolicies <- {
          LeavePolicyModel.getLeavePolicies(maybeperson.get.p.g + " only", maybeperson.get.p.ms + " only", request)
        }
      } yield {
        maybeperson.map( person => {
          val leaveprofile_doc = LeaveProfileModel.doc.copy(
              pid = request.session.get("id").get,
              pn = person.p.fn + " " + person.p.ln
          )
          Ok(views.html.leaveprofile.myprofileform(leaveprofileform.fill(leaveprofile_doc), leavepolicies))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myprofileinsert = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      leaveprofileform.bindFromRequest.fold(
          formWithError => {
            for {
              maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request) 
              leavepolicies <- {
                LeavePolicyModel.getLeavePolicies(maybeperson.get.p.g + " only", maybeperson.get.p.ms + " only", request)
              }
            } yield {
              maybeperson.map( person => {
                Ok(views.html.leaveprofile.myprofileform(formWithError, leavepolicies))
              }).getOrElse(NotFound)
            }
          },
          formWithData => {
            for {
              leaveprofileunique <- LeaveProfileModel.isUnique(formWithData, request)
              maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request)
              leavepolicies <- {
                LeavePolicyModel.getLeavePolicies(maybeperson.get.p.g + " only", maybeperson.get.p.ms + " only", request)
              }
              maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("pid" ->formWithData.pid, "lt"->formWithData.lt), request)
              maybe_alert <- AlertUtility.findOne(BSONDocument("k"->1005))
            } yield {
              if (leaveprofileunique) {
                val doc_objectID = BSONObjectID.generate
                val eligbleleaveentitlement = LeaveProfileModel.sortEligbleLeaveEntitlement(formWithData, request)
                Await.result(
                  LeaveProfileModel.insert(                  
                      formWithData.copy(
                          _id=doc_objectID, 
                          set_ent=Entitlement(
                              EntitlementValue(s=eligbleleaveentitlement(0)(0), e=eligbleleaveentitlement(0)(1), cf=eligbleleaveentitlement(0)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(1)(0), e=eligbleleaveentitlement(1)(1), cf=eligbleleaveentitlement(1)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(2)(0), e=eligbleleaveentitlement(2)(1), cf=eligbleleaveentitlement(2)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(3)(0), e=eligbleleaveentitlement(3)(1), cf=eligbleleaveentitlement(3)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(4)(0), e=eligbleleaveentitlement(4)(1), cf=eligbleleaveentitlement(4)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(5)(0), e=eligbleleaveentitlement(5)(1), cf=eligbleleaveentitlement(5)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(6)(0), e=eligbleleaveentitlement(6)(1), cf=eligbleleaveentitlement(6)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(7)(0), e=eligbleleaveentitlement(7)(1), cf=eligbleleaveentitlement(7)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(8)(0), e=eligbleleaveentitlement(8)(1), cf=eligbleleaveentitlement(8)(2)),
                              EntitlementValue(s=eligbleleaveentitlement(9)(0), e=eligbleleaveentitlement(9)(1), cf=eligbleleaveentitlement(9)(2))
                          )
                      ),
                      p_request=request),
                  Tools.db_timeout
                )
                AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=doc_objectID.stringify, c="Create document."), p_request=request)
                Redirect(routes.PersonController.myprofileview)
              } else {
                val replaceMap = Map(
                    "NAME"-> (maybeperson.get.p.fn + " " + maybeperson.get.p.ln),
                    "LEAVEPROFILE"-> formWithData.lt,
                    "URL"->(Tools.hostname+"/leaveprofile/myprofile/view?p_id=" + maybeleaveprofile.get._id.stringify)
                )
                val alert = if ((maybe_alert.getOrElse(null))!=null) { maybe_alert.get.copy(m=Tools.replaceSubString(maybe_alert.get.m, replaceMap.toList)) } else { null }
                maybeperson.map( person => {
                  Ok(views.html.leaveprofile.myprofileform(leaveprofileform.fill(formWithData), leavepolicies, "", alert))
                }).getOrElse(NotFound)
              }
            }
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myprofileedit(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
        maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request) 
      } yield {
        maybeleaveprofile.map( leaveprofile => {
          Ok(views.html.leaveprofile.myprofileform(leaveprofileform.fill(leaveprofile), List(), p_id))
        }).getOrElse(NotFound)
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def myprofileupdate(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      leaveprofileform.bindFromRequest.fold(
          formWithError => {
            for {
              maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
              maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(request.session.get("id").get)), request) 
            } yield {
              Ok(views.html.leaveprofile.myprofileform(formWithError, List(), p_id))
            }
          },
          formWithData => {
            val eligbleleaveentitlement = LeaveProfileModel.sortEligbleLeaveEntitlement(formWithData, request)
            Await.result(
              LeaveProfileModel.update(
                  BSONDocument("_id" -> BSONObjectID(p_id)), 
                  formWithData.copy(                      
                      _id=BSONObjectID(p_id), 
                      set_ent=Entitlement(
                          EntitlementValue(s=eligbleleaveentitlement(0)(0), e=eligbleleaveentitlement(0)(1), cf=eligbleleaveentitlement(0)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(1)(0), e=eligbleleaveentitlement(1)(1), cf=eligbleleaveentitlement(1)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(2)(0), e=eligbleleaveentitlement(2)(1), cf=eligbleleaveentitlement(2)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(3)(0), e=eligbleleaveentitlement(3)(1), cf=eligbleleaveentitlement(3)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(4)(0), e=eligbleleaveentitlement(4)(1), cf=eligbleleaveentitlement(4)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(5)(0), e=eligbleleaveentitlement(5)(1), cf=eligbleleaveentitlement(5)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(6)(0), e=eligbleleaveentitlement(6)(1), cf=eligbleleaveentitlement(6)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(7)(0), e=eligbleleaveentitlement(7)(1), cf=eligbleleaveentitlement(7)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(8)(0), e=eligbleleaveentitlement(8)(1), cf=eligbleleaveentitlement(8)(2)),
                          EntitlementValue(s=eligbleleaveentitlement(9)(0), e=eligbleleaveentitlement(9)(1), cf=eligbleleaveentitlement(9)(2))
                      )
                  ), 
                  request),
                Tools.db_timeout
            )
            AuditLogModel.insert(p_doc=AuditLogModel.doc.copy(_id =BSONObjectID.generate, pid=request.session.get("id").get, pn=request.session.get("name").get, lk=p_id, c="Modify document."), p_request=request)
            Future.successful(Redirect(routes.PersonController.myprofileview))
          }
      )
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}

  def myprofileview(p_id:String) = withAuth { username => implicit request => {
    for {
      maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleaveprofile.get.pid)), request)
    } yield {
      if(request.session.get("id").get == maybeperson.get._id.stringify){
        maybeleaveprofile.map( leaveprofile => {
          Ok(views.html.leaveprofile.myprofileview(leaveprofile))
        }).getOrElse(NotFound) 
      } else {
        Ok(views.html.error.unauthorized())
      }
    }
  }}
  
  def myprofiledelete(p_id:String, p_lt:String, p_pid:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      Await.result(LeaveProfileModel.remove(BSONDocument("_id" -> BSONObjectID(p_id)), request), Tools.db_timeout)
      LeaveModel.setLockDown(BSONDocument("pid" -> p_pid, "lt" -> p_lt), request)
      AuditLogModel.remove(BSONDocument("lk"->p_id), request)
      Future.successful(Redirect(routes.PersonController.myprofileview))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
 def getValuesOnLeaveTypeChangeJSON(p_pid:String, p_lt:String) = withAuth { username => implicit request => {
    for {
      maybe_person <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(p_pid)), request)
      maybe_leavepolicy <- LeavePolicyModel.findOne(BSONDocument("lt" -> p_lt), request)
      maybe_leavesetting <- LeaveSettingModel.findOne(BSONDocument(), request)
    } yield {
      val person = maybe_person.getOrElse(PersonModel.doc.copy(_id=BSONObjectID.generate))
      val leavepolicy= maybe_leavepolicy.getOrElse(LeavePolicyModel.doc)
      val leavesetting = maybe_leavesetting.getOrElse(LeaveSettingModel.doc)
      val previouscutoffdate = LeaveSettingModel.getPreviousCutOffDate(leavesetting.cfm)
      val cutoffdate = LeaveSettingModel.getCutOffDate(leavesetting.cfm)
      val leaveearned = leavepolicy.set.acc match {
        case "No accrue" => LeaveProfileModel.getEligibleEntitlement(leavepolicy, PersonModel.getServiceMonths(person))
        case "Monthly - utilisation based on earned" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to now.
            LeaveProfileModel.getTotalMonthlyEntitlementEarn(person.p.edat.get, leavepolicy, leavesetting, person)
          } else {
            // Earn calculate from previous cut of date to now.
            LeaveProfileModel.getTotalMonthlyEntitlementEarn(previouscutoffdate, leavepolicy, leavesetting, person)
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            // Earn calculate from join date to now.
            LeaveProfileModel.getTotalMonthlyEntitlementEarn(person.p.edat.get, leavepolicy, leavesetting, person)
          } else {
            // Earn calculate from previous cut of date to now.
            LeaveProfileModel.getTotalMonthlyEntitlementEarn(previouscutoffdate, leavepolicy, leavesetting, person)
          }
        }
        case "Yearly" => {
          if (person.p.edat.get.isAfter(cutoffdate.minusMonths(1).dayOfMonth().withMaximumValue())) {
            0.0
          } else {
            LeaveProfileModel.getEligibleEntitlement(leavepolicy, PersonModel.getServiceMonths(person))
          }
        }
      }
      val cbal = leavepolicy.set.acc match {
        case "No accrue" => LeaveProfileModel.getEligibleEntitlement(leavepolicy, PersonModel.getServiceMonths(person))
        case "Monthly - utilisation based on earned" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            LeaveProfileModel.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, person.p.edat.get, leavepolicy, leavesetting, person)
          } else {
            LeaveProfileModel.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, previouscutoffdate, leavepolicy, leavesetting, person)
          }
        }
        case "Monthly - utilisation based on closing balance" => {
          if (person.p.edat.get.isAfter(previouscutoffdate)) {
            LeaveProfileModel.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, person.p.edat.get, leavepolicy, leavesetting, person)
          } else {
            LeaveProfileModel.getTotalMonthlyEntitlementEarnUntilCutOff(cutoffdate, previouscutoffdate, leavepolicy, leavesetting, person)
          }
        }
        case "Yearly" => leaveearned
      }      
      val ent = LeaveProfileModel.getEligibleEntitlement(leavepolicy, PersonModel.getServiceMonths(person))
      val m_jan = LeaveProfileModel.getMonthEntitlementEarn(leavepolicy, leavesetting, person, 1)
      val m_feb = LeaveProfileModel.getMonthEntitlementEarn(leavepolicy, leavesetting, person, 2)
      val m_mar = LeaveProfileModel.getMonthEntitlementEarn(leavepolicy, leavesetting, person, 3)
      val m_apr = LeaveProfileModel.getMonthEntitlementEarn(leavepolicy, leavesetting, person, 4)
      val m_may = LeaveProfileModel.getMonthEntitlementEarn(leavepolicy, leavesetting, person, 5)
      val m_jun = LeaveProfileModel.getMonthEntitlementEarn(leavepolicy, leavesetting, person, 6)
      val m_jul = LeaveProfileModel.getMonthEntitlementEarn(leavepolicy, leavesetting, person, 7)
      val m_aug = LeaveProfileModel.getMonthEntitlementEarn(leavepolicy, leavesetting, person, 8)
      val m_sep = LeaveProfileModel.getMonthEntitlementEarn(leavepolicy, leavesetting, person, 9)
      val m_oct = LeaveProfileModel.getMonthEntitlementEarn(leavepolicy, leavesetting, person, 10)
      val m_nov = LeaveProfileModel.getMonthEntitlementEarn(leavepolicy, leavesetting, person, 11)
      val m_dec = LeaveProfileModel.getMonthEntitlementEarn(leavepolicy, leavesetting, person, 12)
      maybe_leavepolicy.map( leavepolicy => {
        val json = Json.parse("{\"e1\":" + leavepolicy.ent.e1.e + ", \"e1_s\":" + leavepolicy.ent.e1.s + ", \"e1_cf\":" + leavepolicy.ent.e1.cf +
          ",\"e2\":" + leavepolicy.ent.e2.e + ", \"e2_s\":" + leavepolicy.ent.e2.s + ", \"e2_cf\":" + leavepolicy.ent.e2.cf +
          ",\"e3\":" + leavepolicy.ent.e3.e + ", \"e3_s\":" + leavepolicy.ent.e3.s + ", \"e3_cf\":" + leavepolicy.ent.e3.cf +
          ",\"e4\":" + leavepolicy.ent.e4.e + ", \"e4_s\":" + leavepolicy.ent.e4.s + ", \"e4_cf\":" + leavepolicy.ent.e4.cf +
          ",\"e5\":" + leavepolicy.ent.e5.e + ", \"e5_s\":" + leavepolicy.ent.e5.s + ", \"e5_cf\":" + leavepolicy.ent.e5.cf +
          ",\"e6\":" + leavepolicy.ent.e6.e + ", \"e6_s\":" + leavepolicy.ent.e6.s + ", \"e6_cf\":" + leavepolicy.ent.e6.cf +
          ",\"e7\":" + leavepolicy.ent.e7.e + ", \"e7_s\":" + leavepolicy.ent.e7.s + ", \"e7_cf\":" + leavepolicy.ent.e7.cf +
          ",\"e8\":" + leavepolicy.ent.e8.e + ", \"e8_s\":" + leavepolicy.ent.e8.s + ", \"e8_cf\":" + leavepolicy.ent.e8.cf +
          ",\"e9\":" + leavepolicy.ent.e9.e + ", \"e9_s\":" + leavepolicy.ent.e9.s + ", \"e9_cf\":" + leavepolicy.ent.e9.cf +
          ",\"e10\":" + leavepolicy.ent.e10.e + ", \"e10_s\":" + leavepolicy.ent.e10.s + ", \"e10_cf\":" + leavepolicy.ent.e10.cf +
          ",\"earned\":" + leaveearned + ",\"ent\":" + ent + ",\"bal\":" + leaveearned + ",\"cbal\":" + cbal + ",\"m_jan\":" + m_jan +
          ",\"m_feb\":" + m_feb + ",\"m_mar\":" + m_mar + ",\"m_apr\":" + m_apr +
          ",\"m_may\":" + m_may + ",\"m_jun\":" + m_jun + ",\"m_jul\":" + m_jul +
          ",\"m_aug\":" + m_aug + ",\"m_sep\":" + m_sep + ",\"m_oct\":" + m_oct +
          ",\"m_nov\":" + m_nov + ",\"m_dec\":" + m_dec +
          "}");
        Ok(json).as("application/json")
      }).getOrElse({        
          val json = Json.parse("{\"e1\":0,\"e1_s\":0,\"e1_cf\":0,\"e2\":0,\"e2_s\":0,\"e2_cf\":0,\"e3\":0,\"e3_s\":0,\"e3_cf\":0,\"e4\":0,\"e4_s\":0,\"e4_cf\":0,\"e5\":0,\"e5_s\":0,\"e5_cf\":0,\"e6\":0,\"e6_s\":0,\"e6_cf\":0,\"e7\":0,\"e7_s\":0,\"e7_cf\":0,\"e8\":0,\"e8_s\":0,\"e8_cf\":0,\"e9\":0,\"e9_s\":0,\"e9_cf\":0,\"e10\":0,\"e10_s\":0,\"e10_cf\":0}");
          Ok(json).as("application/json")
      })
    }
 }}
 
 def getLeaveProfile(p_pid:String) = withAuth { username => implicit request => {
   
   if(request.session.get("roles").get.contains("Admin")){
     for {
       leavetypes <- LeaveProfileModel.getLeaveTypesSelection(p_pid, request)
     } yield {
       render {
         case Accepts.Html() => Ok(views.html.error.unauthorized())
         case Accepts.Json() => Ok(Json.toJson(leavetypes))
       }
     }
   } else {
     Future.successful(Ok(views.html.error.unauthorized()))
   }
   
 }}
    
}