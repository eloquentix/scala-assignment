package eloquentix

/**
 * A roll-up is an aggregation of a set of datapoints over an interval of time.
 *
 * This sub-package gathers a number of such aggregations, all relying on the
 * [[eloquentix.opentsdb.OpenTSDB]] trait to fetch the data they need.
 *
 * One of them depends on metris A and B, the other on metrics A and C. Both
 * require data over the same time interval. This set-up is meant to verify
 * that when using the [[eloquentix.opentsdb.BatchingOpenTSDB]] client, we
 * obtain query de-duplication and batching — the final query should request
 * data for three metrics: A, B and C; not four: A, B, A and C. It should also
 * batch them together inside the same JSON payload.
 */
package object rollup
