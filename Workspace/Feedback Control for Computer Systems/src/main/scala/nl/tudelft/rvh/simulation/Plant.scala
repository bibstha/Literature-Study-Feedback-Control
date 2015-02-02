package nl.tudelft.rvh.simulation

class Boiler(g: Double = 0.01, y: Double = 0)(implicit DT: Double) extends Component {

	def update(u: Double) = new Boiler(g, y + DT * (u - g * y))

	def action = y
}

class Spring(x: Double = 0, v: Double = 0, m: Double = 0.1, k: Double = 1, g: Double = 0.05)(implicit DT: Double) extends Component {

	def update(u: Double) = {
		val a = u - k * x - g * v
		val vv = v + DT * a
		val xx = x + DT * vv
		
		new Spring(xx, vv, m, k, g)
	}

	def action = x
}

class Cache(val size: Int, val demand: Long => Int, val internalTime: Long = 0, val cache: Map[Int, Long] = Map(), val res: Boolean = false) extends Component {

	def update(u: Double): Cache = {
		val time = internalTime + 1
		val newSize = math.max(0, math floor u)
		val item = demand(time)

		if (cache contains item) {
			val newCache = cache + (item -> time)

			new Cache(newSize toInt, demand, time, newCache, true)
		}
		else if (cache.size >= size) {
			val n = 1 + cache.size - size
			val vk = cache map { case (i, l) => (l, i) }
			val newCache = (cache /: vk.map { case (l, _) => l }.toList.sorted.take(n).map(vk(_)))(_ - _)

			new Cache(newSize.toInt, demand, time, newCache + (item -> time), false)
		}
		else {
			val newCache = cache + (item -> time)

			new Cache(newSize.toInt, demand, time, newCache, false)
		}
	}

	def action: Double = {
		if (res) 1.0 else 0.0
	}
}

class AdPublisher(scale: Int, minPrice: Int, relWidth: Double = 0.1, value: Double = 0.0) extends Component {
	
	def update(u: Double): AdPublisher = new AdPublisher(scale, minPrice, relWidth, u)

	def action: Double = {
		if (value <= minPrice) {
			0
		}
		else {
			val mean = scale * math.log(value / minPrice)
			val demand = math.floor(Loops.gaussian(mean, relWidth * mean))
			math.max(0, demand)
		}
	}
}
