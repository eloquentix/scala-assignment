package eloquentix
package opentsdb

import eloquentix.opentsdb.data.{ Request, Result }

/**
 * This is the central abstraction that allows us to execute OpenTSDB requests
 * using different modes: direct, batching or something else.
 *
 * The salient point is the use of a higher-kinded type `F` that allows
 * implementing classes to define how the values enforced by this trait are
 * produced. For example, in the [[DirectOpenTSDB]] implementation, `F` is
 * set to be `Task`. In [[BatchingOpenTSDB]], the same `F` is set to be an
 * [[ExecutionPlan]] that saves seen requests and can then optimize them.
 */
trait OpenTSDB[F[_]] {
  /**
   * Send a request which is known to produce a single result and return it.
   * Violating this precondition will lead to incorrect or incomplete results.
   *
   * This method allows implementations to batch and deduplicate queries. due
   * to the 1-to-1 mapping between queries and responses, which allows us to
   * map back results to originating queries. Client code is responsible for
   * calling the right method of the two possibilities: [[fetchOne]] or
   * [[fetchAll]].
   *
   * @param req Request containing only one, single-result-producing query.
   */
  def fetchOne(req: Request): F[Result]

  /**
   * Send a request whose each individual query may produce more than one
   * result.
   *
   * For example, if the request contains a single query, the response may
   * contain two result sets for that query. Similarly, the request may
   * contain two queries and the server send back three results.
   *
   * Optimizing this kind of requests is impossible, because we can't map
   * back results to the original queries. Client code is responsible for
   * calling the right method of the two possibilities: [[fetchOne]] or
   * [[fetchAll]].
   *
   * @param req Request containing, possibly, multi-result producing queries.
   */
  def fetchAll(req: Request): F[List[Result]]
}
