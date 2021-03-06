package ranking

import controllers.routes
import models.db
import ranking.common.{Ranking, RankingElement}
import scalikejdbc._

/**
 *
 * @author ponkotuy
 * Date: 14/12/05.
 */
case object FirstShipRate extends Ranking {
  import ranking.common.Ranking._
  override val title: String = "初期艦"

  override def rankingQuery(limit: Int): List[RankingElement] = {
    val counts = db.Ship.countAllShip(sqls"s.id = 1")
      .groupBy(t => EvolutionBase(t._1))
      .mapValues(_.values.sum)
    val masters = db.MasterShipBase.findAllBy(sqls.in(db.MasterShipBase.ms.id, counts.keys.toSeq))
      .map { it => it.id -> it }.toMap
    val sum = counts.values.sum
    counts.toList.sortBy(-_._2).map { case (sid, count) =>
      val master = masters(sid)
      val url = routes.ViewSta.shipBook(sid).toString()
      RankingElement(master.name, <span><strong>{f"$count%,d"}</strong>{s" / $sum"}</span>, url, count)
    }
  }

  override def comment: List[String] = List("進化前で集計しています")

  override def divClass: String = colmd3
}
