package example

import example.CategoryModel.CategoryID
import example.TagModel.TagID
import example.UserModel.UserID
import zio.{Has, RIO, Ref, Task, UIO, ULayer, ZIO}

object ExpenseModel {

  case class ExpenseID(value: Long) extends AnyVal

  case class Expense(
      id: ExpenseID,
      userID: UserID,
      categoryID: CategoryID,
      tagID: TagID
  )

  trait ExpenseRepository {

    def getByUserID(userID: UserID): Task[List[Expense]]
  }

  object ExpenseRepository {

    val data = List(
      Expense(ExpenseID(1), UserID(1), CategoryID(1), TagID(1)),
      Expense(ExpenseID(2), UserID(1), CategoryID(1), TagID(2)),
      Expense(ExpenseID(3), UserID(1), CategoryID(2), TagID(3)),
      Expense(ExpenseID(4), UserID(1), CategoryID(2), TagID(3))
    )

    val test: ULayer[Has[ExpenseRepository]] =
      ExpenseRepositoryTest(data).toLayer

    def getByUserID(
        userID: UserID
    ): RIO[Has[ExpenseRepository], List[Expense]] =
      ZIO.accessM(_.get.getByUserID(userID))
  }

  class ExpenseRepositoryTest(data: Ref[List[Expense]]) extends ExpenseRepository {

    override def getByUserID(userID: UserID): Task[List[Expense]] =
      data.map(_.filter(_.userID == userID)).get
  }

  object ExpenseRepositoryTest {
    def apply(table: List[Expense]): UIO[ExpenseRepositoryTest] = for {
      data <- Ref.make(table)
    } yield new ExpenseRepositoryTest(data)
  }
}
