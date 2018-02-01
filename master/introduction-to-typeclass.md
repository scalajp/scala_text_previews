# 型クラスへの誘い

本章では[Implicitの章](./implicit.md)で、少しだけ触れた型クラスについて、より深く掘り下げます。

Implicitの章では、`Additive`という型クラスを定義することで、任意のコレクション（`Additive`が存在するもの）に対して、
要素の合計値を計算することができたのでした。本章では、このような仕組みを利用して、色々なアルゴリズムをライブラリ
化できることを見ていきます。

まず、`sum`に続いて、要素の平均値を計算するための`average`メソッドを作成することを考えます。`average`メソッドの
素朴な実装は次のようになるでしょう。

```scala
scala> def average(list: List[Int]): Int = list.foldLeft(0)(_ + _) / list.size
average: (list: List[Int])Int
```

ここで、`List`の`size`を計算するのは若干効率が悪いですが、その点については気にしないことにします。これを、前章のように
`Additive`を使ってみます。

```scala
scala> trait Additive[A] {
     |   def plus(a: A, b: A): A
     |   def zero: A
     | }
defined trait Additive

scala> object Additive {
     |   implicit object IntAdditive extends Additive[Int] {
     |     def plus(a: Int, b: Int): Int = a + b
     |     def zero: Int = 0
     |   }
     |   implicit object DoubleAdditive extends Additive[Double] {
     |     def plus(a: Double, b: Double): Double = a + b
     |     def zero: Double = 0.0
     |   }
     | }
defined object Additive
warning: previously defined trait Additive is not a companion to object Additive.
Companions must be defined together; you may wish to use :paste mode for this.
```

```scala
def average[A](lst: List[A])(implicit m: Additive[A]): A = {
  val length = lst.foldLeft(m.zero)((x, _) => m.plus(x, 1))
  lst.foldLeft(m.zero)((x, y) => m.plus(x, y)) / length
}
```

残念ながら、このコードはコンパイルを通りません。何故ならば、 `A` 型の値を `A` 型で割ろうとしているのですが、その方法がわからないから
です。ここで、`Additive` をより一般化して、引き算、掛け算、割り算をサポートした型 `Num` を考えてみます。掛け算の
メソッドを `multiply` 、割り算のメソッドを `divide` とすると、 `Num` は次のようになるでしょう。
ここで、 `Nums` は、対話環境でコンパニオンクラス/オブジェクトを扱うために便宜的に作った名前空間であり、通常のScalaプログラムでは必要ありません。

```scala
scala> object Nums {
     |   trait Num[A] {
     |     def plus(a: A, b: A): A
     |     def minus(a: A, b: A): A
     |     def multiply(a: A, b: A): A
     |     def divide(a: A, b: A): A 
     |     def zero: A
     |   }
     |   object Num{
     |     implicit object IntNum extends Num[Int] {
     |       def plus(a: Int, b: Int): Int = a + b
     |       def minus(a: Int, b: Int): Int = a - b
     |       def multiply(a: Int, b: Int): Int = a * b
     |       def divide(a: Int, b: Int): Int = a / b
     |       def zero: Int = 0
     |     }
     |     implicit object DoubleNum extends Num[Double] {
     |       def plus(a: Double, b: Double): Double = a / b
     |       def minus(a: Double, b: Double): Double = a - b
     |       def multiply(a: Double, b: Double): Double = a * b
     |       def divide(a: Double, b: Double): Double = a / b
     |       def zero: Double = 0.0
     |     }
     |   }
     | }
defined object Nums
```

また、 `average` メソッドは、リストの長さ、つまり整数で割る必要があるので、整数を `A` 型に変換するための型
`FromInt` も用意します。 `FromInt` は次のようになります。 `to` は `Int` 型を対象の型に変換するメソッドです。

```scala
scala> object FromInts {
     |   trait FromInt[A] {
     |     def to(from: Int): A
     |   }
     |   object FromInt {
     |     implicit object FromIntToInt extends FromInt[Int] {
     |       def to(from: Int): Int = from
     |     }
     |     implicit object FromIntToDouble extends FromInt[Double] {
     |       def to(from: Int): Double = from
     |     }
     |   }
     | }
defined object FromInts
```

`Num` と `FromInt` を使うと、 `average` 関数は次のように書くことができます。

```scala
scala> import Nums._
import Nums._

scala> import FromInts._
import FromInts._

scala> def average[A](lst: List[A])(implicit a: Num[A], b: FromInt[A]): A = {
     |   val length = lst.length
     |   a.divide(lst.foldLeft(a.zero)((x, y) => a.plus(x, y)), b.to(length))
     | }
average: [A](lst: List[A])(implicit a: Nums.Num[A], implicit b: FromInts.FromInt[A])A
```

この `average` 関数は次のようにして使うことができます。

```scala
scala> average(List(1, 3, 5))
res0: Int = 3

scala> average(List(1.5, 2.5, 3.5))
res1: Double = 0.0
```

このようにして、複数の型クラスを組み合わせることで、より大きな柔軟性を手に入れることができました。ちなみに、
上記のコードは、context boundsというシンタックスシュガーを使うことで、次のように書き換えることもできます。

```scala
scala> import Nums._
import Nums._

scala> import FromInts._
import FromInts._

scala> def average[A:Num:FromInt](lst: List[A]): A = {
     |   val a = implicitly[Num[A]]
     |   val b = implicitly[FromInt[A]]
     |   val length = lst.length
     |   a.divide(lst.foldLeft(a.zero)((x, y) => a.plus(x, y)), b.to(length))
     | }
average: [A](lst: List[A])(implicit evidence$1: Nums.Num[A], implicit evidence$2: FromInts.FromInt[A])A
```

implicit parameterの名前 `a` と `b` が引数から見えなくなりましたが、 `implicitly[Type]` とすることで、
`Type` 型のimplicit paramerterの値を取得することができます。

別のアルゴリズムをライブラリ化した例を、Scalaの標準ライブラリから紹介します。コレクションから
最大値を取得する `max` と最小値を取得する `min` です。これらは、次のようにして使うことができます。

```scala
scala> List(1, 3, 4, 2).max
res2: Int = 4

scala> List(1, 3, 2, 4).min
res3: Int = 1
```

比較できない要素のリストに対して `max` 、 `min` を求めようとするとコンパイルエラーになります。この `max`
と `min` も型クラスで実現されています。

`max` と `min` のシグネチャは次のようになっています。

```scala
def max[B >: A](implicit cmp: Ordering[B]): A
```

```scala
def min[B >: A](implicit cmp: Ordering[B]): A
```

`B >: A` の必要性についてはおいておくとして、ポイントは、 `Ordering[B]` 型のimplicit parameterを要求
するところです。 `Ordering[B]` のimplicitなインスタンスがあれば、 `B` 型同士の大小関係を比較できるため、最大値
と最小値を求めることができます。

さらに、別のアルゴリズムをライブラリ化してみます。リストの中央値を求めるメソッド `median` を定義する
ことを考えます。中央値は、要素数が奇数の場合、リストをソートした場合のちょうど真ん中の値を、偶数の
場合、真ん中の2つの値を足して2で割ったものになります。ここで、中央値の最初のケースには `Ordering` が、
2番目のケースにはそれに加えて、先程定義した `Num` と `FromInt` があれば良さそうです。

この3つを使って、 `median` メソッドを定義してみます。先程出てきたcontext boundsを使って、シグネチャ
が見やすいようにしています。

```scala
scala> import Nums._
import Nums._

scala> import FromInts._
import FromInts._

scala> def median[A:Num:Ordering:FromInt](lst: List[A]): A = {
     |   val num = implicitly[Num[A]]
     |   val ord = implicitly[Ordering[A]]
     |   val int = implicitly[FromInt[A]]
     |   val size = lst.size
     |   require(size > 0)
     |   val sorted = lst.sorted
     |   if(size % 2 == 1) {
     |     sorted(size / 2)
     |   } else {
     |     val fst = sorted((size / 2) - 1)
     |     val snd = sorted((size / 2))
     |     num.divide(num.plus(fst, snd), int.to(2))
     |   }
     | }
median: [A](lst: List[A])(implicit evidence$1: Nums.Num[A], implicit evidence$2: Ordering[A], implicit evidence$3: FromInts.FromInt[A])A
```

このメソッドは次のようにして使うことができます。

```scala
scala> assert(2 == median(List(1, 3, 2)))

scala> assert(2.5 == median(List(1.5, 2.5, 3.5)))

scala> assert(3 == median(List(1, 3, 4, 5)))
```

次はより複雑な例を考えてみます。次のように、オブジェクトをシリアライズする
メソッド `string` を定義したいとします。

```scala
import Serializers.string
string(List(1, 2, 3)) // [1,2,3]
string(List(List(1),List(2),List(3)) // [[1],[2],[3]]
string(1) // 1
string("Foo") // Foo
class MyClass(val x: Int)
string(new MyClass(1)) // Compile Error!
class MyKlass(val x: Int)
implicit object MyKlassSerializer extends Serializer[MyKlass] {
  def serialize(klass: MyKlass): String = s"MyKlass(${klass.x})"
}
string(new MyKlass(1)) // OK
```

この `string` メソッドは、

* 整数をシリアライズ可能
* 文字列をシリアライズ可能
* 要素がシリアライズ可能なリストをシリアライズ可能

であり、自分で作成したクラスについては、次のトレイト `Serializer` を
継承して `serialize` メソッドを実装するオブジェクトをimplicitにすることで、
シリアライズ可能にできます。

```scala
trait Serializer[A] {
  def serialize(obj: A): String
}
```

これを仮に `Serializer` 型クラスと呼びます。

この `string` メソッドのシグニチャをまず考えてみます。このメソッドは
`Serializer` 型クラスを必要としているので、 `Serializer[A]` のような
implicit parameterを必要としているはずです。また、引数は `A` 型の値
で、返り値は `String` なので、結果として次のようになります。

```scala
def string[A:Serializer](obj: A): String = ???
```

次に実装ですが、 `Serializer` 型クラスを要求しているということは、
`Serializer` の `serialize` メソッドを呼びだせばいいだけなので、
次のようになります。

```scala
scala> object Serializers {
     |   trait Serializer[A] {
     |     def serialize(obj: A): String
     |   }
     |   def string[A:Serializer](obj: A): String = {
     |     implicitly[Serializer[A]].serialize(obj)
     |   }
     | }
defined object Serializers
```

`Serializers` という `object` を作っていますが、これをimportすることで：

* `string` メソッドを使える
* `Serializer` 型クラスが公開される

ようになります。

さて、これでシグネチャの部分はできたので実装に入ります。とはいっても、今回の範囲内では、ほとんど
オブジェクトを `toString` するだけのものなのですが…。

```scala
implicit object IntSerializer extends Serializer[Int] {
  def serialize(obj: Int): String = obj.toString
}
implicit object StringSerializer extends Serializer[Int] {
  def serialize(obj: String): String = obj
}
```

以上は、整数と文字列の `Serializer` です。単に `toString` を呼び出しているか、自身を返しているだけなのが
わかります。次が少しわかりにくいです。要素がシリアライズ可能なときだけ、リストがシリアライズ可能でなければ
いけないのですから、単純に以下のようにしてもだめです。

```scala
implicit def ListSerializer[A]: Serializer[List[A]] = {
  def serialize(obj: List[A]): String = ???
}
```

この定義では `A` にどのような操作が可能なのかわからないため、中身を単純に `toString` するくらいしか
実装しようがないですし、また、そのような実装では要素型の `Serializer` の実装と整合性が取れません。
これを解決するには、 `ListSerializer` がimplicit parameterを取るようにします。

```scala
implicit def ListSerializer[A](implicit serializer: Serializer[A]): Serializer[List[A]] = {
  def serialize(obj: List[A]): String = {
    val serializedList = obj.map{o => serializer.serialize(o)}
    serializedList.mkString("[",",","]")
  }
}
```

このように定義したとき、コンパイラは、要素型 `A` がシリアライズ可能でない場合（あらかじめimplicit def/objectで
そう定義されていない場合）コンパイルエラーにしてくれます。つまり、型安全にオブジェクトをシリアライズできるの
です。

ここまでで、一通りの実装ができたので、定義を一箇所にまとめて実行結果を確認してみましょう。この節の最初の
方の入力例を使って動作確認をします。

```scala
scala> object Serializers {
     |   trait Serializer[A] {
     |     def serialize(obj: A): String
     |   }
     |   def string[A:Serializer](obj: A): String = {
     |     implicitly[Serializer[A]].serialize(obj)
     |   }
     |   implicit object IntSerializer extends Serializer[Int] {
     |     def serialize(obj: Int): String = obj.toString
     |   }
     |   implicit object StringSerializer extends Serializer[String] {
     |     def serialize(obj: String): String = obj
     |   }
     |   implicit def ListSerializer[A](implicit serializer: Serializer[A]): Serializer[List[A]] = new Serializer[List[A]]{
     |     def serialize(obj: List[A]): String = {
     |       val serializedList = obj.map{o => serializer.serialize(o)}
     |       serializedList.mkString("[",",","]")
     |     }
     |   }
     | }
defined object Serializers

scala> import Serializers._
import Serializers._

scala> string(List(1, 2, 3)) // [1,2,3]
res7: String = [1,2,3]

scala> string(List(List(1),List(2),List(3))) // [[1],[2],[3]]
res8: String = [[1],[2],[3]]

scala> string(1) // 1
res9: String = 1

scala> string("Foo") // Foo
res10: String = Foo

scala> // class MyClass(val x: Int)
     | // string(new MyClass(1)) // Compile Error!
     | class MyKlass(val x: Int)
defined class MyKlass

scala> implicit object MyKlassSerializer extends Serializer[MyKlass] {
     |   def serialize(klass: MyKlass): String = s"MyKlass(${klass.x})"
     | }
defined object MyKlassSerializer

scala> string(new MyKlass(1)) // OK
res13: String = MyKlass(1)
```

行コメントに書いた想定通りの動作をしていることがわかります。ここで重要なのは、 `MyClass` に
対しては `Serializer` を定義していないので、コンパイルエラーになる点です。多くの言語のシリアライズ
ライブラリでは、リフレクションを駆使してシリアライズしようとするため、実行時になって初めてエラーが
わかることが多いです。特に、静的型付き言語では、そのような場合、型によって安全を保証できないのは
デメリットです。一方、今回用いた手法では、後付けでシリアライズする型を追加でき、かつコンパイル時に
その正当性を検査できるのです。

ここまでで紹介したように、型クラス（≒implicit parameter）は、うまく使うと、後付けのデータ型に対して
既存のアルゴリズムを型安全に適用するのに使うことができます。この特徴は、特にライブラリ設計のときに
重要になってきます。ライブラリ設計時点で定義されていないデータ型に対していかにしてライブラリのアルゴリズム
を適用するか、つまり、拡張性が高いように作るかというのは、なかなか難しい問題です。簡潔に書けることを重視すると、
拡張性が狭まりがちですし、拡張性が高いように作ると、デフォルトの動作でいいところを毎回書かなくてはいけなくて
利用者にとって不便です。型クラスを使ったライブラリを提供することによって、この問題をある程度緩和することが
できます。皆さんも、型クラスを使って、既存の問題をより簡潔に、拡張性が高く解決できないか考えてみてください。
