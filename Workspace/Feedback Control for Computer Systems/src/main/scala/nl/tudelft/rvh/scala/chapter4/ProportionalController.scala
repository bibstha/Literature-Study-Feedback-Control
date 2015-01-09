package nl.tudelft.rvh.scala.chapter4

import scala.concurrent.duration.DurationInt
import javafx.event.ActionEvent
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import nl.tudelft.rvh.rxscalafx.Observables
import nl.tudelft.rvh.scala.ScalaChartTab
import rx.lang.scala.Observable
import rx.lang.scala.subjects.BehaviorSubject

class ProportionalController extends ScalaChartTab("Chapter 4 - Proportional controller", "Cruise control", "time", "speed") {

	private var k: Double = 0.5
	
	override def bottomBox(): HBox = {
		this.k = 0.5
		
		val box = super.bottomBox()
		val kTF = new TextField(this.k.toString)
		
		box.getChildren.add(kTF)
		
		Observables.fromNodeEvents(kTF, ActionEvent.ACTION)
			.map { _ => kTF.getText }
			.map { _.toDouble }
			.subscribe(i => this.k = i)
		
		box
	}

	def seriesName(): String = s"k = $k"
	
	def simulation(): Observable[(Number, Number)] = {
		val time = Observable.interval(50 milliseconds).take(60)
		def setPoint(time: Long): Int = if (time < 20) 15 else if (time < 40) 5 else 20
		val cc = new SpeedSystem
		
		val feedbackLoop = Observable[Double](subscriber => {
			val speed = BehaviorSubject(cc.speed)
			speed.subscribe(subscriber)
			
			time.map(setPoint)
				.zipWith(speed)(_ - _)
				.map { this.k * _ }
				.map(cc.interact)
				.subscribe(speed)
		})
		time.zipWith(feedbackLoop)((_, _))
	}

	def simulationForGitHub(): Observable[Double] = {
		def setPoint(time: Int): Int = if (time < 20) 15 else if (time < 40) 5 else 20
		val cc = new SpeedSystem
		
		Observable(subscriber => {
			val speed = BehaviorSubject(cc.speed)
			speed.subscribe(subscriber)
			
			Observable.from(0 until 60)
				.map(setPoint)
				.zipWith(speed)(_ - _)
				.map { this.k * _ }
				.map(cc.interact)
				.subscribe(speed)
		})
	}
}