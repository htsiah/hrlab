import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.data._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka

import akka.actor.Actor
import akka.actor.Props

import scala.concurrent.duration._
import scala.concurrent.Future
import jobs._
import views._

import models._
import utilities._

object Global extends GlobalSettings {
  
  // 400 - bad request
  override def onBadRequest(request: RequestHeader, error: String) = {
    Future.successful(BadRequest("Bad Request: " + error))
  } 
  
  // 500 - internal server error
  override def onError(request: RequestHeader, throwable: Throwable) = {
    Future.successful(InternalServerError(
        html.error.onerror(throwable.getMessage())
    ))
  }
  
  // 404 - page not found error
  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound(
        html.error.onhandlernotfound()  
    ))  
  }
    
  override def onStart(app: Application) {
        
    // Parameter 1: initial delat
    // Parameter 2: interval
    Akka.system(app).scheduler.schedule(1 seconds, 10 seconds) {
      // MonthlyLeaveProfileUpdateJob.run
    }
    
    Akka.system(app).scheduler.schedule(1 seconds, 30 seconds) {
      // MonthlyLeaveProfileUpdateJob.run
    }
    
    Akka.system(app).scheduler.schedule(1 seconds, 4 hours) {
      MonthlyLeaveProfileUpdateJob.run
    }
          	
  }
  
  override def onStop(app: Application) {
    
    // Shutdown database connection
    DbConnUtility.close()
  
  }  
   
}