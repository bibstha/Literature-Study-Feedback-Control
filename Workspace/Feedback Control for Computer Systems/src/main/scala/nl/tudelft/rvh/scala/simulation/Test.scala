package nl.tudelft.rvh.scala.simulation

import scala.concurrent.duration.DurationDouble
import scala.io.StdIn

import nl.tudelft.rvh.scala.simulation.controller.PIDController
import nl.tudelft.rvh.scala.simulation.plant.Boiler
import rx.lang.scala.Observable

object Test extends App {

	implicit val DT = 1.0
	def setpoint(t: Int) = 10 * Setpoint.doubleStep(t, 1000, 6000)

	val time = Observable interval (DT milliseconds) map (_ toInt) take 15000
	val p = new Boiler
	val c = new PIDController(0.45, 0.01)

	val res = Loops.closedLoop(time, setpoint, c, p)
	time.zipWith(res)((t, r) => s"$t\t$r").subscribe(println(_))

	StdIn.readLine()
}