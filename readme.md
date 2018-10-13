# Assignment: An Optimizing OpenTSDB Client

This project implements, partially, a small library for interacting with the
[OpenTSDB][0] database. There are some missing bits, though, and your task is
to fill these in.

We'll now proceed to give a small overview of the OpenTSDB database, of the
assignment and of each of the exercises, in part.

  - [OpenTSDB Overview](#open-t-s-d-b-overview)
  - [Assignment Overview](#project-overview)
  - [Exercises Overview](#exercises-overview)

## OpenTSDB Overview

OpenTSDB is a [time series database][1]. It allows one to store values associated
with a timestamp and several tags. The central concept in OpenTSDB is the
metric. A metric is similar to a table in a relational database. A record of
such a metric can be seen as a 2-tuple composed of a timestamp and the value
for that timestap, such a pair is also called a datapoint. Additionally, one can
attach tags to each such datapoint.

You can run OpenTSDB locally using the provided [docker-compose.yml](./docker-compose.yml)
file. Assuming you have already installed Docker and `docker-compose` installed,
execute the following command:

```bash
$ docker-compose up
```

You can now go to [localhost:4242](http://localhost:4242) and see the search UI
that ships with the OpenTSDB server. It usually takes a little while before
everything starts up, so give it a couple of minutes.

Interacting with OpenTSDB is done via its web API. For example, the following
command uses `cURL` to send a single record (assuming the server is running on
[localhost:4242](http://localhost:4242)) using a `POST` request:

```bash
curl -i -X POST 'localhost:4242/api/put' -d @- << EOF
[
  {
    "metric": "metric-a",
    "timestamp": 1525133400,
    "value": 10,
    "tags": {
      "t1": "v1",
      "t2": "v2"
    }
  }
]
EOF
```

Reading data is achieved in a similar fashion. The following request asks what
datapoints have been stored for two metrics, during a common time interval.

```bash
curl -i -X POST 'localhost:4242/api/query' -d @- << EOF
{
  "start": "2018/05/01 00:10:00",
  "end": "2018/05/01 00:10:02",
  "timezone": "UTC",
  "queries": [
    {
      "metric": "metric-a",
      "aggregator": "none",
      "tags": {}
    },
    {
      "metric": "metric-b",
      "aggregator": "none",
      "tags": {}
    }
  ]
}
EOF
```

This ability of OpenTSDB to allow querying for data from multiple metrics under
the same time interval is what we're exploiting in this little library.

## Assignment Overview

Part of the test is to understand the project structure and how things fit
together, which is why we're skipping a higher-level explanation of the
project's architecture here.

However, most of the components in the project have attached documentation that
explain their purposes and responsibilities in more detail. So, make sure you
browse all existing files before jumping to the exercises.

## Exercises Overview

Search for all `TODO` occurrences we've left in the project and start working
on the tasks described there. In general, they're numbered in increasing order
of difficulty, so you might want to respect that when approaching them.

There are five exercises as part of this assignment:

  1. **Exercise 1** is mostly algorithmic. You are required to transform a
  few data structures. For this exercise we've provided a small suite of tests
  to verify your implementation. It can, and should, be used as a specification
  for the exercise.

  2. **Exercise 2** evaluates your ability to **use** functional abstractions.
  You are required to modify the code in the project's `Main` object to use the
  by-now-complete [`BatchingOpenTSDB`](./src/main/scala/opentsdb/BatchingOpenTSDB.scala)
  client.

  3. **Exercise 3** and **Exercise 4** are meant to asses your ability to
  understand and modify an existing code base, especially one written in a more
  functional style. The two exercises are related and can be done in an
  interleaved fashion. One of them requires writing the implementation and the
  other tests.

  4. **Exercise 5** requires no coding, but does require writing. Just let your
  imagination fly with this one.

Good luck and have fun!

[0]: http://opentsdb.net
[1]: https://en.wikipedia.org/wiki/Time_series_database
