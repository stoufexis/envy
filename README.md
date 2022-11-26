# Envy

A tiny, 0-boilerplate library for reading environment variables into case classes.

## Usage
* Can handle arbitrarily nested structures
* Can use default values of fields if an environment variable is not found
* The following naming conventions of environment variables are assumed
  * The root case class name will be the root prefix of the environment variables
  * Any nested case classes will use their parent's field name as a prefix
  * All names are converted to upper case, underscore-seperated from camel case
  * The word 'config' (case insensitive) is removed on any name

**Example**

Given:
```scala
import envy._

case class UltraNested(nestedList : List[Option[Int]])

case class Nested(a: Either[String, Int], ultraNested: UltraNested, b : String = "NOT THE B!")

case class ServerConfig(nested : Nested, hosts : List[String] , port : Int = 5050, secure : Boolean)

object Hello extends App {
  println(Env.load[ServerConfig])
}

```

this:
```
SERVER_NESTED_A=1a23 \
SERVER_NESTED_ULTRA_NESTED_NESTED_LIST=1,,2,3,,4 \
SERVER_NESTED_B='im a nested bee' \
SERVER_HOSTS=localhost,remotehost \
SERVER_SECURE=false \
sbt run
```

outputs:
```scala
Right(ServerConfig(Nested(Left(1a23),UltraNested(List(Some(1), None, Some(2), Some(3), None, Some(4))),im a nested bee),List(localhost, remotehost),5050,false))
```

## Wrapper types

If you define a Read instance of a case class 
it will be treated as a value and not as a nested variable list. 
This can be used to define custom wrapper types

Consider this example:

```scala
import envy._

case class SomeConfig(trimmed: Trimmed, pos : Positive)

case class Trimmed(value : String) extends AnyVal

case class Positive(value : Int) extends AnyVal

object Hello extends App {
  implicit val readMyString : Read[Trimmed]  =
    s => Right(Trimmed(s.trim))

  implicit val readPositive : Read[Positive] =
    s => Read[Int].read(s).flatMap(i => Either.cond(i >= 0, i, s)).map(Positive)

  println(Env.load[SomeConfig](sys.env))
}
```
output:
```
> SOME_TRIMMED=' spacious ' SOME_POS='5' sbt run
Right(SomeConfig(Trimmed(spacious),Positive(5)))
```

Had the read instances not been defined, the wrapper types would be considered nested environment variables 
(SOME_TRIMMED_VALUE, SOME_POS_VALUE)

Read instances can of course be defined for case classes of more than one members
