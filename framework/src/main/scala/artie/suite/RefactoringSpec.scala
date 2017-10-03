package artie.suite

import artie._
import artie.Util._

import shapeless._

import scala.util.Random
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.global

/** Framework to build refactoring specs which can be combined into
  * a one suite to be executed together.
  * 
  * @param service name of the service which was refactored
  */
abstract class RefactoringSpec(val service: String) {

  implicit val ec = global

  // if set to `false` checks will be executed in parallel
  var isSequential = true

  /** Checks will be executed in parallel. */
  def parallel: Unit = isSequential = false

  val checks = Seq.newBuilder[(String, LazyCheck)]

  protected val rand = new Random(System.currentTimeMillis())

  /** Adds a test case to this spec.
    * 
    * @param endpoint short description of service endpoint
    * @param providersF collection of `DataProvider`s
    * @param config test case configuration
    * @param read json to instance mapping
    */
  def check[P <: HList, A](endpoint: String, providersF: Future[P], config: TestConfig, read: Read[A])
                          (f: Random => P => RequestT)
                          (implicit diff: GenericDiffRunner[A]): Unit = {
    checks += (endpoint -> LazyCheck(() => artie.check(providersF, config, read, rand)(f)))
  }
}
