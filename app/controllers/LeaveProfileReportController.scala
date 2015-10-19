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

import models.{LeaveProfileModel, LeaveProfile, LeaveProfileMonthEarn, LeaveProfileCalculation, Entitlement, PersonModel, LeaveModel}
import utilities.{System, Tools}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument,BSONArray}

object LeaveProfileReportController extends Controller with Secured {
  
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
              "e1" -> number,
              "e1_s" -> number,
              "e1_cf" -> number,
              "e2" -> number,
              "e2_s" -> number,
              "e2_cf" -> number,
              "e3" -> number,
              "e3_s" -> number,
              "e3_cf" -> number,
              "e4" -> number,
              "e4_s" -> number,
              "e4_cf" -> number,
              "e5" -> number,
              "e5_s" -> number,
              "e5_cf" -> number
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
  
  def view(p_id:String) = withAuth { username => implicit request => {
    for {
      maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
      maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleaveprofile.get.pid)), request)
    } yield {
      if(request.session.get("roles").get.contains("Admin") | request.session.get("id").get == maybeperson.get.p.mgrid){
        maybeleaveprofile.map( leaveprofile => {
          Ok(views.html.leaveprofilereport.view(leaveprofile))
        }).getOrElse(NotFound)
      } else {
        Ok(views.html.error.unauthorized())
      }
    }
  }}

  def edit(p_id:String) = withAuth { username => implicit request => {
    if(request.session.get("roles").get.contains("Admin")){
      for {
        maybeleaveprofile <- LeaveProfileModel.findOne(BSONDocument("_id" -> BSONObjectID(p_id)), request)
        maybeperson <- PersonModel.findOne(BSONDocument("_id" -> BSONObjectID(maybeleaveprofile.get.pid)), request)
      } yield {
        maybeleaveprofile.map( leaveprofile => {
            Ok(views.html.leaveprofilereport.form(leaveprofileform.fill(leaveprofile), List(), leaveprofile.pid, "", p_id))
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
              Ok(views.html.leaveprofilereport.form(formWithError, List(), maybeleaveprofile.get.pid, "", p_id))
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
                          e1_s=eligbleleaveentitlement(0)(0),
                          e1=eligbleleaveentitlement(0)(1),
                          e1_cf=eligbleleaveentitlement(0)(2),
                          e2_s=eligbleleaveentitlement(1)(0),
                          e2=eligbleleaveentitlement(1)(1),
                          e2_cf=eligbleleaveentitlement(1)(2),
                          e3_s=eligbleleaveentitlement(2)(0),
                          e3=eligbleleaveentitlement(2)(1),
                          e3_cf=eligbleleaveentitlement(2)(2),
                          e4_s=eligbleleaveentitlement(3)(0),
                          e4=eligbleleaveentitlement(3)(1),
                          e4_cf=eligbleleaveentitlement(3)(2),
                          e5_s=eligbleleaveentitlement(4)(0),
                          e5=eligbleleaveentitlement(4)(1),
                          e5_cf=eligbleleaveentitlement(4)(2)
                      )
                  ), 
                  request),
                Tools.db_timeout
            )
            Future.successful(Redirect(request.session.get("path").get))
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
      Future.successful(Redirect(request.session.get("path").get))
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
}