package envy

import cats.implicits._

import java.time.Duration


trait Read[A] {
  def read(s: String) : Either[String, A]
}

object Read {
  def apply[A : Read] : Read[A] = implicitly

  implicit val readString   : Read[String]   = Right(_)
  implicit val readChar     : Read[Char]     = x => Either.cond(x.length == 1, x.head, x)
  implicit val readByte     : Read[Byte]     = x => x.toByteOption.toRight(x)
  implicit val readInt      : Read[Int]      = x => x.toIntOption.toRight(x)
  implicit val readShort    : Read[Short]    = x => x.toShortOption.toRight(x)
  implicit val readLong     : Read[Long]     = x => x.toLongOption.toRight(x)
  implicit val readFloat    : Read[Float]    = x => x.toFloatOption.toRight(x)
  implicit val readDouble   : Read[Double]   = x => x.toDoubleOption.toRight(x)
  implicit val readBool     : Read[Boolean]  = x => x.toBooleanOption.toRight(x)
  implicit val readDuration : Read[Duration] = x => x.toLongOption.map(Duration.ofMillis).toRight(x)

  implicit def readList[A : Read] : Read[List[A]] =
    _.split(",").toList.traverse(Read[A].read)

  implicit def readOption[A : Read] : Read[Option[A]] = {
    case "" => Right(None)
    case s  => Read[A].read(s).map(Some(_))
  }

  /**
   * Tries to read type A. If A cannot be read it tries to read B
   */
  implicit def readEither[A : Read, B : Read] : Read[Either[B, A]] = s =>
    Read[A].read(s).map(Right(_) : Either[B, A]) <+>
    Read[B].read(s).map(Left(_)  : Either[B, A])

  def readFromSource[A: Read](name : String, source : Map[String, String]) : Either[Throwable, A] =
    source.get(name)
      .toRight(new RuntimeException(s"Environment variable '$name' not found"))
      .flatMap(Read[A].read(_).left.map(x => new RuntimeException(s"Couldn't convert $x")))
}
