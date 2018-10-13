import java.time.Instant

/**
 * We're mixing-in all implicits instances and syntax enhancements provided
 * by the Cats library so that we don't have to import them explicitly when
 * used with nested packages. For example, splitting nested packages like
 * below will automatically bring in scope the Cats enhancements:
 *
 * ```
 * package eloquentix
 * package opentsdb
 * ```
 */
package object eloquentix
  extends cats.instances.AllInstances
     with cats.syntax.AllSyntax {

  type Interval = (Instant, Instant)
}
