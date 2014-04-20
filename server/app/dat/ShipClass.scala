package dat

import scalikejdbc.SQLInterpolation._
import models.MasterShipBase
import scalikejdbc.WrappedResultSet

/**
 * @author ponkotuy
 * Date: 14/04/10
 */
case class ShipClass(ctype: Int, name: String)

object ShipClass {
  def apply(ms: SyntaxProvider[MasterShipBase])(rs: WrappedResultSet): Option[ShipClass] = {
    for {
      ctype <- rs.intOpt(ms.ctype)
      name <- rs.stringOpt(ms.name)
    } yield ShipClass(ctype, name + "級")
  }

  def getOneClass(ms: SyntaxProvider[MasterShipBase])(rs: WrappedResultSet): ShipClass = {
    val ctype = rs.int(ms.ctype)
    val name = rs.string(ms.name)
    ShipClass(ctype, name)
  }
}
