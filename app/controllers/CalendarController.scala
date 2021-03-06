package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._

import models.{KeywordModel, OfficeModel}
import reactivemongo.bson.BSONDocument

class CalendarController extends Controller with Secured {

  def company = withAuth { username => implicit request => {
    for {
      maybe_departments <- KeywordModel.findOne(BSONDocument("n" -> "Department"), request)
      offices <- OfficeModel.getAllOfficeName(request)
    } yield {
      val departments = maybe_departments.getOrElse(KeywordModel.doc)
      Ok(views.html.calendar.company(departments.v, offices)).withSession(
          (request.session - "path") + ("path"->((routes.CalendarController.company).toString))
      )
    } 
  }}
  
}