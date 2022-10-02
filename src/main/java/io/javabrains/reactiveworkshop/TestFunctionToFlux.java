package io.javabrains.reactiveworkshop;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

public class TestFunctionToFlux {

  private static int COUNTER = 0;

  public static void main(String[] args) throws IOException {

    fluxExternalIntGen()
      .parallel(2)
      .runOn(Schedulers.boundedElastic())
      .log()
      .doOnError(System.err::println)
      .subscribe(slowConsumer());

    System.out.println("Press a key to end");
    System.in.read();
  }

  private static Integer externalIntGen(Integer previous) {
    return RandomGenerator.getDefault().ints(previous < 0 ? previous * -1 : previous)
      .map(i -> {
        try {
          System.out.println("[ GEN] sleeping with previous " + previous + " and current " + i);
          Thread.sleep(500);
          ++COUNTER;
          System.out.println("[ GEN] returning " + COUNTER + "th current number " + i);
          return i;
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      })
      .findAny().getAsInt();
  }

  private static Flux<Integer> fluxExternalIntGen() {
    return Flux.generate(
      () -> externalIntGen(1),
      (state, sink) -> {
        sink.next(state);
        if (COUNTER == 10) {
          sink.complete();
        }
        return externalIntGen(state);
      });
  }

  private static Consumer<Integer> slowConsumer() {
    return value -> {
      try {
        System.out.println("[ CON] " + Thread.currentThread().getName() + " -> Slowly consuming " + value);
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    };
  }

}