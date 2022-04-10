package example.protocol

import zio.UIO

case class User(name: String, int: Int)

trait FooService {
  def magicNumber: UIO[Int]
  def processUser(user: User): UIO[String]
}
