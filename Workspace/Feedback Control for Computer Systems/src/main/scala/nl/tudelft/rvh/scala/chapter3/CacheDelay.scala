package nl.tudelft.rvh.scala.chapter3

import scala.concurrent.duration.DurationInt

import ObsExtensions.extendObservable
import javafx.event.ActionEvent
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import nl.tudelft.rvh.rxscalafx.Observables
import nl.tudelft.rvh.scala.ScalaChartTab
import rx.lang.scala.Observable
import rx.lang.scala.Subscriber
import rx.lang.scala.subjects.BehaviorSubject

class CacheDelay extends ScalaChartTab("Chapter 3 - Cache with delay", "Delay simulation", "time", "cache size") {

	private var k: Float = 50
	private var delay: Int = 2

	override def bottomBox(): HBox = {
		this.k = 50
		this.delay = 2

		val box = super.bottomBox()
		val kTF = new TextField(this.k.toString)
		val delayTF = new TextField((this.delay - 1).toString)
		
		box.getChildren.addAll(kTF, delayTF)

		Observables.fromNodeEvents(kTF, ActionEvent.ACTION)
			.map { _ => kTF.getText }
			.map { _.toInt }
			.subscribe(i => this.k = i, _ -> {})
		
		Observables.fromNodeEvents(delayTF, ActionEvent.ACTION)
			.map { _ => delayTF.getText }
			.map { _.toInt }
			.subscribe(i => this.delay = i + 1, _ -> {})

		box
	}

	def seriesName(): String = s"k = $k, delay = $delay"

	def simulation(): Observable[(Number, Number)] = {
		val time = Observable.interval(50 milliseconds).take(120)
		def setPoint(time: Long): Double = if (time < 30) 0.6 else if (time < 60) 0.8 else if (time < 90) 0.1 else 0.4
		def cache(size: Double): Double = math.max(0, math.min(1, size / 100))

		val feedbackLoop = Observable((subscriber: Subscriber[Double]) => {
			val hitrate = BehaviorSubject[Double]

			time.map(setPoint)
				.zipWith(hitrate)(_ - _)
				.scan((cum: Double, e: Double) => cum + e)
				.map { this.k * _ }
				.map(cache)
				.delay(this.delay, 0.0)
				.subscribe(hitrate)

			hitrate.subscribe(subscriber)
		})
		time.zipWith(feedbackLoop)((_, _))
	}
}