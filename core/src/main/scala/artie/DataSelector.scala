package artie

import RandomGenerator._

import scala.annotation.tailrec
import scala.util.Random
import scala.concurrent.{ExecutionContext, Future}

trait DataSelector[A] {

  /** Randomly select next element from some `DataGenerator`. */
  def next(implicit rand: Random): A

  /** Randomly select and wrap next element. */
  final def nextOpt(implicit rand: Random, exists: Rand[Unit, Boolean]): Option[A] =
    if (exists(rand)(())) Some(next)
    else                  None

  /** Randomly create a sequence of elements.
    * 
    * @param length number of elements
    */
  @tailrec
  final def nextSeq(length: Int, acc: Seq[A] = Nil)(implicit rand: Random): Seq[A] = 
    if (length == 0) acc
    else             nextSeq(length - 1, next +: acc)

  /** Randomly create a set of elements with 1 <= set.size <= size.
    * 
    * @param size maximum size of set
    */
  @tailrec
  final def nextSet(size: Int, tries: Int = 0, maxTries: Int = 3, acc: Set[A] = Set.empty)(implicit rand: Random): Set[A] = 
    if (size == 0 || tries == maxTries)
      acc
    else {
      val updatedAcc = acc + next

      // element already part of `Set`
      if (updatedAcc.size == acc.size)
        nextSet(size, tries + 1, maxTries, acc)
      else
        nextSet(size - 1, tries, maxTries, updatedAcc)
    }
}

final class DataSelectorOps[A] {

  /** Create a selector from static data.
    * 
    * @param data sequence of elements
    */
  def static(data: A*)(implicit randF: Rand[Int, Int]) = new DataSelector[A] {
    def next(implicit rand: Random): A =
      data(randF(rand)(data.length))
  }

  /** Create a selector form randomly generated data.
    * 
    * @param min smallest element value
    * @param max largest element value
    */
  def random(min: A, max: A)
            (implicit gen: RandomGenerator[A], randF: Rand[Unit, Double]) = new DataSelector[A] {
    def next(implicit rand: Random): A = gen(min, max, randF(rand))
  }

  import DatabaseGenerator._

  /** Create a selector from database-fetched data.
    * 
    * @param query data query
    * @param db database descriptor
    */
  def database(query: String, db: Database)
              (implicit gen: DatabaseGenerator[A], ec: ExecutionContext, randF: Rand[Int, Int]): Future[DataSelector[A]] =
    Future(static(gen(query, db): _*))

  object database {

    /** Create a selector deom randomly fetch data.
      * 
      * @param table target table
      * @param column column to be retrieved
      * @param limit maximum number of elements
      * @param db database descriptor
      */
    def random(table: String, column: String, limit: Int, db: Database)
              (implicit gen: DatabaseGenerator[A], ec: ExecutionContext, randF: Rand[Int, Int]): Future[DataSelector[A]] =
      Future(static(gen(db.randomQuery(table, column, limit), db): _*))
  }
}
