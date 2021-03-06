package controllers

import scala.concurrent.Future

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import models.{LeavePolicyModel, LeavePolicy, LeaveSettingModel, PersonModel, LeaveProfileModel}

import reactivemongo.api._
import reactivemongo.bson.{BSONObjectID,BSONDocument,BSONDateTime}

import play.api.mvc._

class LeaveSettingController extends Controller with Secured {
  
  def index = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        leavepolicies <- LeavePolicyModel.find(BSONDocument(), request)
        leavesetting <- LeaveSettingModel.findOne(BSONDocument(), request)
      } yield {
        Ok(views.html.leavesetting.index(leavepolicies, leavesetting.get)).withSession(
            (request.session - "path") + ("path"->((routes.LeaveSettingController.index).toString))
        )
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def updateCFM(p_mnth:String) = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybe_leavesetting <- LeaveSettingModel.findOne(BSONDocument(), request)
      } yield {
        render {
          case Accepts.Html() => Ok(views.html.error.unauthorized())
          case Accepts.Json() => {
            val leavesetting = maybe_leavesetting.get
            LeaveSettingModel.update(
                BSONDocument(), 
                leavesetting.copy(
                    _id=leavesetting._id,
                    cfm=p_mnth.toInt
                ), 
                request
            )
        
            // Update employee's leave profile
            if (leavesetting.cfm != p_mnth.toInt) {
              PersonModel.find(BSONDocument(), request).map { persons => 
                persons.map { person => {
                  LeaveProfileModel.find(BSONDocument("pid" -> person._id.stringify)).map { leaveprofiles =>  
                    leaveprofiles.map { leaveprofile => {
                      LeaveProfileModel.update(BSONDocument("_id" -> leaveprofile._id), leaveprofile, request)
                    } }
                  }
                } }
              }
            }
            Ok(Json.parse("""{"status":true}""")).as("application/json")
          }
        }
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def updateAprMthd(p_aprmthd:String) = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybe_leavesetting <- LeaveSettingModel.findOne(BSONDocument(), request)
      } yield {
        render {
          case Accepts.Html() => Ok(views.html.error.unauthorized())
          case Accepts.Json() => {
            val leavesetting = maybe_leavesetting.get
            LeaveSettingModel.update(
               BSONDocument(), 
               leavesetting.copy(
                   _id=leavesetting._id,
                   aprmthd=p_aprmthd
               ), 
               request
            )
            Ok(Json.parse("""{"status":true}""")).as("application/json")
          }
        }
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
  def updateFutureRequest(p_futurerequest:String) = withAuth { username => implicit request => { 
    if(request.session.get("roles").get.contains("Admin")){
      for { 
        maybe_leavesetting <- LeaveSettingModel.findOne(BSONDocument(), request)
      } yield {
        render {
          case Accepts.Html() => Ok(views.html.error.unauthorized())
          case Accepts.Json() => {
            val leavesetting = maybe_leavesetting.get
            LeaveSettingModel.update(
               BSONDocument(), 
               leavesetting.copy(
                   _id=leavesetting._id,
                   freq=if(p_futurerequest=="Allow future calendar year request") { true } else { false } 
               ), 
               request
            )
            Ok(Json.parse("""{"status":true}""")).as("application/json")
          }
        }
      }
    } else {
      Future.successful(Ok(views.html.error.unauthorized()))
    }
  }}
  
}