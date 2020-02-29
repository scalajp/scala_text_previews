#!/bin/bash
find . -name '*.md' -type f -exec perl -pi -e '
  s/```tut:book/```scala mdoc:nest/g;
  s/```tut/```scala mdoc:nest/g;
' {} +
