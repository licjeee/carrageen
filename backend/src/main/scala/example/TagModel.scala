package example

import example.CategoryModel.CategoryID
import example.ExpenseModel.{Expense, ExpenseID}
import example.UserModel.UserID
import zio.{Has, RIO, Ref, Task, UIO, ULayer, ZIO}

import java.util.NoSuchElementException

object TagModel {

  case class TagID(value: Long) extends AnyVal

  case class Tag(
      id: TagID,
      name: String,
      categoryID: CategoryID,
      userID: UserID
  )

  trait TagRepository {

    def getByID(id: TagID): Task[Option[Tag]]

    def getByIDs(ids: List[TagID]): Task[List[Tag]]

    def getByCategoryIDs(
        categoryIDs: List[CategoryID]
    ): Task[Map[CategoryID, List[Tag]]]

    def getByExpenses(expenses: List[Expense]): Task[Map[ExpenseID, Tag]]
  }

  object TagRepository {

    val data = List(
      Tag(TagID(1), "tag1", CategoryID(1), UserID(1)),
      Tag(TagID(2), "tag2", CategoryID(1), UserID(1)),
      Tag(TagID(3), "tag3", CategoryID(2), UserID(1))
    )

    val test: ULayer[Has[TagRepository]] =
      TagRepositoryTest(data).toLayer

    def getByID(id: TagID): RIO[Has[TagRepository], Option[Tag]] =
      ZIO.accessM(_.get.getByID(id))

    def getByIDs(ids: List[TagID]): RIO[Has[TagRepository], List[Tag]] =
      ZIO.accessM(_.get.getByIDs(ids))

    def getByCategoryIDs(
        categoryIDs: List[CategoryID]
    ): RIO[Has[TagRepository], Map[CategoryID, List[Tag]]] =
      ZIO.accessM(_.get.getByCategoryIDs(categoryIDs))

    def getByExpenses(
        expenses: List[Expense]
    ): RIO[Has[TagRepository], Map[ExpenseID, Tag]] =
      ZIO.accessM(_.get.getByExpenses(expenses))
  }

  class TagRepositoryTest(data: Ref[List[Tag]]) extends TagRepository {

    override def getByID(id: TagID): Task[Option[Tag]] =
      data.map(_.find(_.id == id)).get

    private def intersect(a: List[Tag], b: List[TagID]): List[Tag] = for {
      aa <- a
      bb <- b
      if (aa.id == bb)
    } yield aa

    override def getByIDs(ids: List[TagID]): Task[List[Tag]] =
      data.map(intersect(_, ids)).get

    override def getByCategoryIDs(
        categoryIDs: List[CategoryID]
    ): Task[Map[CategoryID, List[Tag]]] =
      data
        .map(
          _.filter(tag => categoryIDs.contains(tag.categoryID))
            .groupMap(tag => tag.categoryID)(identity)
        )
        .get

    override def getByExpenses(expenses: List[Expense]): Task[Map[ExpenseID, Tag]] =
      ZIO
        .foreach(expenses)(e =>
          getByID(e.tagID).collect(new NoSuchElementException()) { case Some(tag) => (e.id, tag) }
        )
        .map(_.toMap[ExpenseID, Tag])

    /*
    ZIO
        .foreach(expenses)(e =>
          getByID(e.tagID).map { case Some(tag) =>
            (e.id, tag)
          }
        )
        .map(_.toMap[ExpenseID, Tag])
     */
  }

  object TagRepositoryTest {
    def apply(table: List[Tag]): UIO[TagRepositoryTest] = for {
      data <- Ref.make(table)
    } yield new TagRepositoryTest(data)
  }

}
