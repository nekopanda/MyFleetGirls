
import akka.actor.Props
import com.github.nscala_time.time.StaticDateTimeZone
import models.db._
import org.joda.time.{DateTimeConstants, LocalDate}
import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.libs.Akka
import scalikejdbc._
import util.{Cron, CronSchedule, CronScheduler}

import scala.collection.breakOut
import scala.concurrent.Future
import scala.concurrent.duration._

/**
 *
 * @author ponkotuy
 * Date: 14/05/12.
 */
object Global extends WithFilters(Cors) with GlobalSettings{
  import util.Cron._

  val Japan = StaticDateTimeZone.forOffsetHours(9)

  override def onStart(app: Application): Unit = {
    val cron = Akka.system().actorOf(Props[CronScheduler], "cron")
    cron ! CronSchedule(Cron(0, 5, aster, aster, aster), _ => deleteDailyQuest())
    cron ! CronSchedule(Cron(0, 5, aster, aster, DateTimeConstants.MONDAY), _ => deleteWeeklyQuest())
    cron ! CronSchedule(Cron(0, 5, 1, aster, aster), _ => deleteMonthlyQuest())
    cron ! CronSchedule(Cron(17, 3, aster, aster, aster), _ => cutMaterialRecord())
    cron ! CronSchedule(Cron(23, 3, aster, aster, aster), _ => cutBasicRecord())
    cron ! CronSchedule(Cron(41, 3, aster, aster, aster), _ => addSnapIndex()) // TODO 1回実行するだけで良いので消す
    Akka.system().scheduler.schedule(0.seconds, 45.seconds, cron, "minutes")
  }

  private def deleteDailyQuest(): Unit = {
    Logger.info("Delete Daily Quest")
    Quest.deleteAllBy(sqls"typ in(2, 4, 5)")
  }

  private def deleteWeeklyQuest(): Unit = {
    Logger.info("Delete Weekly Quest")
    Quest.deleteAllBy(sqls"typ = 3")
    Logger.info("Delete AGOProgress")
    AGOProgress.deleteAll()
  }

  private def deleteMonthlyQuest(): Unit = {
    Logger.info("Delete Monthly Quest")
    Quest.deleteAllBy(sqls"typ = 6")
  }

  private def cutMaterialRecord(): Unit = {
    Logger.info("Cut Material Record")
    Admiral.findAll().foreach { user =>
      val monthAgo = System.currentTimeMillis() - 30.days.toMillis
      val materials = Material.findAllByUser(user.id, to = monthAgo)
      val days = materials.groupBy { mat =>
        new LocalDate(mat.created, Japan)
      }.values
      days.foreach { daily =>
        val rest = Set(daily.minBy(_.steel).id, daily.maxBy(_.steel).id)
        daily.filterNot(mat => rest.contains(mat.id)).foreach(_.destroy())
      }
    }
  }

  private def cutBasicRecord(): Unit = {
    Logger.info("Cut Basic Record")
    Admiral.findAll().foreach { user =>
      val monthAgo = System.currentTimeMillis() - 30.days.toMillis
      val basics = Basic.findAllByUser(user.id, to = monthAgo)
      val days = basics.groupBy { b =>
        new LocalDate(b.created, Japan)
      }.values
      days.foreach { daily =>
        val rest = Set(daily.minBy(_.experience).id, daily.maxBy(_.experience).id)
        daily.filterNot(b => rest.contains(b.id)).foreach(_.destroy())
      }
    }
  }

  private def addSnapIndex(): Unit = {
    Logger.info("Add Snapshot Fulltext Index")
    val snapIds = DeckSnapshot.findAll()
    val indexIds: Set[Long] = SnapshotText.findAll().map(_.id)(breakOut)
    snapIds.filterNot { s => indexIds.contains(s.id) }.foreach(SnapshotText.create)
  }
}

object Cors extends Filter {
  def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    val result = f(rh)
    result.map { r =>
      r.withHeaders("Access-Control-Allow-Origin" -> "http://ponkotuy.github.io")
    }
  }
}
