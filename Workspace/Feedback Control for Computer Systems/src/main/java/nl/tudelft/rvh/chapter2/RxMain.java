package nl.tudelft.rvh.chapter2;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

public class RxMain {

	public static void main(String[] args) {
		experimentCumulative(160).subscribe(System.out::println);
//		experimentNonCumulative(160).subscribe(System.out::println);
	}

	public static Observable<Double> experimentCumulative(float k) {
		Func1<Integer, Double> setPoint = time -> time < 50 ? 0.6
				: time < 100 ? 0.8
						: time < 150 ? 0.1
								: 0.9;
		Func1<Double, Double> cache = size -> size < 0 ? 0
				: size > 100 ? 1
						: size / 100;

		return Observable.create((Subscriber<? super Double> subscriber) -> {
			PublishSubject<Double> hitrate = PublishSubject.create();
			

			Observable.zip(Observable.range(0, 200).map(setPoint), hitrate, (a, b) -> a - b)
					.doOnNext(error -> System.out.print(error + "\t"))
					.scan((e, cum) -> e + cum)
					.doOnNext(cumError -> System.out.print(cumError + "\t"))
					.map(cum -> k * cum)
					.doOnNext(action -> System.out.print(action + "\t"))
					.map(cache)
					.subscribe(hitrate::onNext);

			hitrate.take(200).subscribe(subscriber);
			hitrate.onNext(0.0);
		});
	}
	
	public static Observable<Double> experimentNonCumulative(float k) {
		Func1<Integer, Double> setPoint = time -> time < 50 ? 0.6
				: time < 100 ? 0.8
						: time < 150 ? 0.1
								: 0.9;
		Func1<Double, Double> cache = size -> size < 0 ? 0
				: size > 100 ? 1
						: size / 100;

		return Observable.create((Subscriber<? super Double> subscriber) -> {
			PublishSubject<Double> hitrate = PublishSubject.create();

			Observable.zip(Observable.range(0, 200).map(setPoint), hitrate, (a, b) -> a - b)
					.doOnNext(error -> System.out.print(error + "\t"))
					.map(e -> k * e)
					.doOnNext(action -> System.out.print(action + "\t"))
					.map(cache)
					.subscribe(hitrate::onNext);

			hitrate.take(200).subscribe(subscriber);
			hitrate.onNext(0.0);
		});
	}
}
