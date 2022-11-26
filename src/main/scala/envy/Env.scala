package envy

import shapeless._
import shapeless.labelled._
import cats.implicits._

trait Env[A] {
  def load(name : String) : Either[Throwable, A]
}

object Env {
  def load[A : Env: Manifest]: Either[Throwable, A] =
    implicitly[Env[A]].load(configPrefix[A])

  // Don't use directly
  trait EnvField[A, D] {
    def load(name : String, default : D) : Either[Throwable, A]
  }

  def format(name: String): String =
    "[A-Z\\d]".r
      .replaceAllIn(name, m => (if (m.start == 0) "" else "_") + m.group(0))
      .toUpperCase

  def configPrefix[A](implicit man: Manifest[A]): String =
    format(man.runtimeClass.getSimpleName.replaceAll("(?i)config", ""))

  implicit def hNilEnvF[D] : EnvField[HNil, D] = (_, _) => Right(HNil)

  implicit def envFieldFromEnv[A](implicit e : Env[A]) : EnvField[A, Some[A]]   = (name, default) => Right(e.load(name).getOrElse(default.value))
  implicit def envFieldFromEnvN[A](implicit e : Env[A]): EnvField[A, None.type] = (name, _) => e.load(name)

  implicit def envDFromReadS[A : Read]: EnvField[A, Some[A]]   = (name, d) => Right(Read.readFromEnv(name).getOrElse(d.value))
  implicit def envDFromReadN[A : Read]: EnvField[A, None.type] = (name, _) => Read.readFromEnv(name)

  implicit def hListBaseEnv[K <: Symbol, H, T <: HList, DH, DT <: HList]
  ( implicit
    witness : Witness.Aux[K]
  , hEnv    : Lazy[EnvField[H, DH]]
  , tEnv    : EnvField[T, DT]
  ) : EnvField[FieldType[K, H] :: T, DH :: DT] =
  (prefix, default) =>
    ( hEnv.value.load(s"${prefix}_${format(witness.value.name)}", default.head)
    , tEnv.load(prefix, default.tail)
    ).mapN(field[K](_) :: _)

  implicit def genericEnv[A, R, D <: HList]
  ( implicit
    gen  : LabelledGeneric.Aux[A, R]
  , defs : Default.Aux[A, D]
  , rEnv : Lazy[EnvField[R, D]]
  ) : Env[A] =
    rEnv.value.load(_, defs()).map(gen.from)
}

trait Read[A] {
  def read(s: String) : Either[String, A]
}

object Read {
  def apply[A : Read] : Read[A] = implicitly

  implicit val readString : Read[String]  = Right(_)
  implicit val readInt    : Read[Int]     = x => x.toIntOption.toRight(x)
  implicit val readBool   : Read[Boolean] = x => x.toBooleanOption.toRight(x)

  implicit def readList[A : Read] : Read[List[A]] =
    _.split(",").toList.traverse(Read[A].read)

  implicit def readOption[A : Read] : Read[Option[A]] = {
    case "" => Right(None)
    case s  => Read[A].read(s).map(Some(_))
  }

  def readFromEnv[A: Read](name : String) : Either[Throwable, A] =
    sys.env.get(name)
      .toRight(new RuntimeException(s"Environment variable '$name' not found"))
      .flatMap(Read[A].read(_).left.map(x => new RuntimeException(s"Couldn't convert $x")))
}

case class UltraNested(nestedList : List[Option[Int]])

case class Nested(a: Int, ultraNested: UltraNested, b : String = "asdasd")

case class ServerConfig(nested : Nested, hosts : List[String] , port : Int = 5050, secure : Boolean)

object Hello extends App {
  println(Env.load[ServerConfig])
}
