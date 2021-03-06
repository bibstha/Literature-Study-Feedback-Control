package nl.tudelft.rvh

import java.io.File

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.concurrent.duration.DurationInt

import javafx.embed.swing.SwingFXUtils
import javafx.event.ActionEvent
import javafx.geometry.Insets
import javafx.scene.SnapshotParameters
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart.Data
import javafx.scene.chart.XYChart.Series
import javafx.scene.control.Button
import javafx.scene.control.Tab
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import javax.imageio.ImageIO
import nl.tudelft.rvh.rxjavafx.JavaFxScheduler
import nl.tudelft.rvh.rxscalafx.Observables
import rx.lang.scala.JavaConversions
import rx.lang.scala.Observable
import rx.lang.scala.ObservableExtensions
import rx.lang.scala.schedulers.ComputationScheduler

abstract class StepTestTab(tabName: String, chartTitle: String, xName: String, yName: String)(implicit DT: Double = 1.0) extends Tab(tabName) {

	private val simulate = new Button("Start simulation")
	private val print = new Button("Print data")
	private val save = new Button("Save chart")
	private val clear = new Button("Clear chart")

	val chart = initChart(chartTitle, xName, yName)
	chart setAnimated false
	chart setCreateSymbols false

	print setDisable true
	save setDisable true
	
	val box = new VBox(chart, bottomBox)
	box.setPadding(new Insets(5, 5, 15, 5))
	VBox.setVgrow(chart, Priority.ALWAYS)
	
	this setContent box

	Observables.fromNodeEvents(simulate, ActionEvent.ACTION)
		.doOnNext(event => {
			simulate setDisable true
			clear setDisable true
			print setDisable true
			save setDisable true
		})
		.map(_ => new Series[Number, Number])
		.doOnNext(_.setName(this.seriesName))
		.doOnNext(chart.getData add _)
		.flatMap(series => this.runSimulation
			.map(tuple => new Data(tuple._1, tuple._2))
			.observeOn(JavaConversions.javaSchedulerToScalaScheduler(JavaFxScheduler.getInstance))
			.doOnNext(series.getData add _)
			.doOnCompleted({
				simulate setDisable false
				clear setDisable false
				print setDisable false
				save setDisable false
			}))
		.doOnError(_ printStackTrace)
		.subscribe

	Observables.fromNodeEvents(clear, ActionEvent.ACTION)
		.subscribe(_ => chart.getData.clear)

	Observables.fromNodeEvents(print, ActionEvent.ACTION)
		.flatMap(_ => chart.getData.asScala toObservable)
		.map(series => series.getData.asScala.map(data => data.getXValue + ", " + data.getYValue)
			.foldLeft(series.getName + ":")((sum, current) => sum + "\n" + current))
		.subscribe(println(_))

	Observables.fromNodeEvents(save, ActionEvent.ACTION)
		.map(_ => chart.snapshot(new SnapshotParameters, null))
		.map(SwingFXUtils.fromFXImage(_, null))
		.flatMap(img => getFile.map(f => ImageIO.write(img, "png", f)))
		.subscribe

	def initChart(title: String, xName: String, yName: String) = {
		val xAxis = new NumberAxis
		val yAxis = new NumberAxis
		val chart = new LineChart(xAxis, yAxis)

		xAxis setLabel xName
		yAxis setLabel yName
		chart setTitle title

		chart
	}

	def getFile: Observable[File] = {
		Observable(subscriber => {
			val fileChooser = new FileChooser
			fileChooser setTitle "Save image"
			fileChooser.getExtensionFilters add new ExtensionFilter("PNG files (*.png)", "*.png")

			Option(fileChooser showSaveDialog null)
				.map(f => if (f.getPath endsWith ".png") f else new File(f.getPath + ".png"))
				.foreach(subscriber onNext _)
		})
	}

	def bottomBox = new HBox(simulate, clear, print, save)

	def seriesName: String

	def time = Observable interval (50 milliseconds, ComputationScheduler())

	def runSimulation: Observable[(Number, Number)] = {
		time.map(DT *).zipWith(simulation)((_, _))
			.onBackpressureBuffer
			.asInstanceOf[Observable[(Number, Number)]]
	}

	def simulation: Observable[_ <: AnyVal]
}