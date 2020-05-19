# inferenceql.query

## Usage

```clojure
(require '[inferenceql.query :as query])
```

`inferenceql.query` exposes the function `inferenceql.query/q`, which can be used to execute queries. It accepts three positional arguments:

1. a query to be executed, a string
2. a data table, a possibly empty vector of maps
3. (optional) models, a map from model names to model values

### `SELECT`

`SELECT` behaves as it does in SQL. The table provided as the second argument to `inferenceql.query/q` is bound to `data`.

```clojure
(query/q "SELECT * FROM data"
         [{:x 0 :y 2}
          {:x 1 :y 1}
          {:x 2 :y 0}])
=> [{:x 0 :y 2}
    {:x 1 :y 1}
    {:x 2 :y 0}]
```

Individual columns can be selected.

```clojure
(query/q "SELECT x FROM data"
         [{:x 0 :y 2}
          {:x 1 :y 1}
          {:x 2 :y 0}])
=> [{:x 0}
    {:x 1}
    {:x 2}]
```

The `FROM` clause can be omitted, in which case the data source is assumed to be `data`.

```clojure
(query/q "SELECT x"
         [{:x 0}
          {:x 1}
          {:x 2}])
=> [{:x 0}
    {:x 1}
    {:x 2}]
```

### `ORDER BY`

By default, results are returned in the order that they were provided. `ORDER BY` behaves as it does in SQL. If `ORDER BY` is provided without either `ASC` or `DESC`, results are returned in ascending order.

```clojure
(query/q "SELECT * FROM data ORDER BY y"
         [{:x 0 :y 2}
          {:x 1 :y 1}
          {:x 2 :y 0}])
=> [{:x 2 :y 0}
    {:x 1 :y 1}
    {:x 0 :y 2}]
```

```clojure
(query/q "SELECT * FROM data ORDER BY y ASC"
         [{:x 0 :y 2}
          {:x 1 :y 1}
          {:x 2 :y 0}])
=> [{:x 2 :y 0}
    {:x 1 :y 1}
    {:x 0 :y 2}]
```

```clojure
(query/q "SELECT * FROM data ORDER BY y DESC"
         [{:x 0 :y 2}
          {:x 1 :y 1}
          {:x 2 :y 0}])
=> [{:x 0 :y 2}
    {:x 1 :y 1}
    {:x 2 :y 0}]
```

### `LIMIT`

`LIMIT n` behaves as it does in SQL, returning the first `n` results if there are more than `n`. Otherwise all results will be returned. See above for information about the ordering of results, including the default sort order.

```clojure
(query/q "SELECT x FROM data LIMIT 2"
         [{:x 0}
          {:x 1}
          {:x 2}])
=> [{:x 0}
    {:x 1}]
```

### `WHERE`

#### `=`, `>`, `<`, `>=`, and `<=`

Five binary predicates can be used in `WHERE` clauses.

```clojure
(query/q "SELECT * FROM data WHERE x=1"
         [{:x 0 :y 2}
          {:x 1 :y 1}
          {:x 2 :y 0}])
=> [{:x 1 :y 1}]
```

```clojure
(query/q "SELECT * FROM data WHERE x>1"
         [{:x 0 :y 2}
          {:x 1 :y 1}
          {:x 2 :y 0}])
=> [{:x 2 :y 0}]
```

```clojure
(query/q "SELECT * FROM data WHERE x<1"
         [{:x 0 :y 2}
          {:x 1 :y 1}
          {:x 2 :y 0}])
=> [{:x 0 :y 2}]
```

```clojure
(query/q "SELECT * FROM data WHERE x>=1"
         [{:x 0 :y 2}
          {:x 1 :y 1}
          {:x 2 :y 0}])
=> [{:x 1 :y 1}
    {:x 2 :y 0}]
```

```clojure
(query/q "SELECT * FROM data WHERE x<=1"
         [{:x 0 :y 2}
          {:x 1 :y 1}
          {:x 2 :y 0}])
=> [{:x 0 :y 2}
    {:x 1 :y 1}]
```

#### `AND` and `OR`

Conditions can be joined together with `AND` and `OR`.

```clojure
(query/q "SELECT * FROM data WHERE x>0 AND y>0"
         [{:x 0 :y 2}
          {:x 1 :y 1}
          {:x 2 :y 0}])
=> [{:x 1 :y 1}]
```

`OR` has higher precedence than `AND`.

```clojure
(query/q "SELECT * FROM data WHERE x=0 AND x=1 OR x=0"
         [{:x 0}
          {:x 1}
          {:x 2}])
=> [{:x 0}]
```

#### `IS NULL`, `IS NOT NULL`

`IS NULL` and `IS NOT NULL` behave as they do in SQL.

```clojure
(query/q "SELECT x FROM data WHERE y IS NULL"
         [{:x 0 :y 2}
          {:x 1}
          {:x 2 :y 1}])
=> [{:x 1}]
```

```clojure
(query/q "SELECT x FROM data WHERE y IS NOT NULL"
         [{:x 0 :y 2}
          {:x 1}
          {:x 2 :y 1}])
=> [{:x 0}
    {:x 2}]
```

### `PROBABILITY OF` and `PROBABILITY DENSITY OF`

In addition to columns, one can also select the probability of, or probability density of, a set of events occurring under an `inferenceql.inference` model. `PROBABILITY OF` will return the (normalized) likelihood of the events occurring under the model as a number between `0` and `1`. `PROBABILITY DENSITY OF` will return a number that expresses relative likelihood of the event occurring under the model. The behavior of `PROBABILITY OF` is only defined when it is used with categorical variables (variables that can take on one of a limited number of possible values).

To make models available for querying a map with model names as keys and models as values may be passed to `inferenceql.query/q` via an optional third argument.

```clojure
(require '[inferenceql.inference.gpm :as gpm])

(def model
  (gpm/Multimixture
   {:vars {:x :categorical
           :y :categorical}
    :views [[{:probability 0.75
              :parameters  {:x {"yes" 1.0 "no" 0.0}
                            :y {"yes" 1.0 "no" 0.0}}}
             {:probability 0.25
              :parameters  {:x {"yes" 0.0 "no" 1.0}
                            :y {"yes" 0.0 "no" 1.0}}}]]}))
```

If a variable appears alone, then the probability of the variable with that name, taking on the value in the column with the same name, is returned.

```clojure
(query/q "SELECT (PROBABILITY OF x UNDER model AS p) FROM data"
         [{:x "yes" :y "yes"}
          {:x "yes" :y "no"}
          {:x "no"  :y "yes"}
          {:x "no"  :y "no"}]
         {:model model})
=> [{:p 0.75}
    {:p 0.75}
    {:p 0.25}
    {:p 0.25}]
```

#### `GIVEN`

```clojure
(query/q "SELECT (PROBABILITY OF x GIVEN y UNDER model AS p) FROM data"
         [{:x "yes" :y "yes"}
          {:x "yes" :y "no"}
          {:x "no"  :y "yes"}
          {:x "no"  :y "no"}]
         {:model model})
=> [{:p 1.0}
    {:p 0.0}
    {:p 0.0}
    {:p 1.0}]
```

Literal events may also be provided.

```clojure
(query/q "SELECT (PROBABILITY OF x=\"yes\" GIVEN y UNDER model AS p) FROM data"
         [{:x "yes" :y "yes"}
          {:x "yes" :y "no"}
          {:x "no"  :y "yes"}
          {:x "no"  :y "no"}]
         {:model model})
=> [{:p 1.0}
    {:p 0.0}
    {:p 1.0}
    {:p 0.0}]
```

```clojure
(query/q "SELECT (PROBABILITY OF x GIVEN y=\"yes\" UNDER model AS p) FROM data"
         [{:x "yes" :y "yes"}
          {:x "yes" :y "no"}
          {:x "no"  :y "yes"}
          {:x "no"  :y "no"}]
         {:model model})
=> [{:p 1.0}
    {:p 1.0}
    {:p 0.0}
    {:p 0.0}]
```

Though their semantics differ, the only difference between the _syntax_ of `PROBABILITY DENSITY OF` and the syntax of `PROBABILITY OF` is the addition of the keyword `DENSITY`.

```clojure
(query/q "SELECT (PROBABILITY DENSITY OF x GIVEN y UNDER model AS p) FROM data"
         [{:x "yes" :y "yes"}
          {:x "yes" :y "no"}
          {:x "no"  :y "yes"}
          {:x "no"  :y "no"}]
         {:model model})
=> [{:p 1.0}
    {:p 0.0}
    {:p 0.0}
    {:p 1.0}]
```

### `GENERATE`

One can also use generated values from a model as a data source.

```clojure
(def always-yes-model
  (gpm/Multimixture
   {:vars {:x :categorical}
    :views [[{:probability 1.0
              :parameters {:x {"yes" 1.0}}}]]}))
```

```clojure
(query/q "SELECT * FROM (GENERATE x UNDER model) LIMIT 3"
         [{:x "no"}]
         {:model always-yes-model})
=> [{:x "yes"}
    {:x "yes"}
    {:x "yes"}]
```

One can also generate values that are subject to constraints.

```clojure
(query/q "SELECT * FROM (GENERATE x GIVEN y=\"yes\" UNDER model) LIMIT 3"
         [{:x "no"}]
         {:model model})
=> [{:x "yes"}
    {:x "yes"}
    {:x "yes"}]
```

One can also compute the probability of an event under a model that is subject to constraints.

```clojure
(query/q "SELECT (PROBABILITY OF x UNDER (GENERATE x GIVEN y=\"yes\" UNDER model) AS p) FROM data"
         [{:x "yes" :y "yes"}
          {:x "yes" :y "no"}
          {:x "no"  :y "yes"}
          {:x "no"  :y "no"}]
         {:model model})
=> [{:p 1.0}
    {:p 1.0}
    {:p 0.0}
    {:p 0.0}]
```
