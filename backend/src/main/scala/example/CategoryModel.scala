package example

import example.ExpenseModel.{Expense, ExpenseID}
import example.UserModel.UserID
import zio.{Has, RIO, Ref, Task, UIO, ULayer, ZIO}

object CategoryModel {

  case class CategoryID(value: Long) extends AnyVal

  case class Category(id: CategoryID, name: String, userID: UserID)

  trait CategoryRepository {

    def getByID(id: CategoryID): Task[Option[Category]]

    def getByIDs(ids: List[CategoryID]): Task[List[Category]]

    def getByUserID(userID: UserID): Task[List[Category]]

    def getByExpenses(expenses: List[Expense]): Task[Map[ExpenseID, Category]]

  }

  object CategoryRepository {

    val data = List(
      Category(CategoryID(1), "category1", UserID(1)),
      Category(CategoryID(2), "category2", UserID(1))
    )

    val test: ULayer[Has[CategoryRepository]] =
      CategoryRepositoryTest(data).toLayer

    def getByID(
        id: CategoryID
    ): RIO[Has[CategoryRepository], Option[Category]] =
      ZIO.accessM(_.get.getByID(id))

    def getByIDs(
        ids: List[CategoryID]
    ): RIO[Has[CategoryRepository], List[Category]] =
      ZIO.accessM(_.get.getByIDs(ids))

    def getByExpenses(
        expenses: List[Expense]
    ): RIO[Has[CategoryRepository], Map[ExpenseID, Category]] =
      ZIO.accessM(_.get.getByExpenses(expenses))
  }

  class CategoryRepositoryTest(data: Ref[List[Category]]) extends CategoryRepository {

    override def getByID(id: CategoryID): Task[Option[Category]] =
      data.map(_.find(_.id == id)).get

    private def intersect(
        a: List[Category],
        b: List[CategoryID]
    ): List[Category] = for {
      aa <- a
      bb <- b
      if (aa.id == bb)
    } yield (aa)

    override def getByIDs(ids: List[CategoryID]): Task[List[Category]] =
      data.map(intersect(_, ids)).get
//      data.map(_.filter(ids.contains(_)).distinct).get

    override def getByUserID(userID: UserID): Task[List[Category]] =
      data.map(_.filter(_.userID == userID)).get
    /*
    override def getByExpenses(
        expenses: List[Expense]
    ): Task[Map[ExpenseID, Category]] = ZIO
      .foreach(expenses)(e => getByID(e.categoryID).map { case Some(category) => (e.id, category) })
      .map(_.toMap[ExpenseID, Category])
     */

    override def getByExpenses(
        expenses: List[Expense]
    ): Task[Map[ExpenseID, Category]] = ZIO
      .foreach(expenses)(e =>
        getByID(e.categoryID).collect(new NoSuchElementException) { case Some(category) => (e.id, category) }
      )
      .map(_.toMap[ExpenseID, Category])
  }

  object CategoryRepositoryTest {
    def apply(table: List[Category]): UIO[CategoryRepositoryTest] = for {
      data <- Ref.make(table)
    } yield new CategoryRepositoryTest(data)
  }

}
