package example

import caliban.{GraphQL, RootResolver}
import caliban.schema.GenericSchema
import example.CategoryModel.{Category, CategoryID, CategoryRepository}
import example.ExpenseModel.{Expense, ExpenseID, ExpenseRepository}
import example.TagModel.{Tag, TagID, TagRepository}
import example.UserModel.UserID
import zio.console.Console
import zio.query.{CompletedRequestMap, DataSource, Request, ZQuery}
import zio.{Has, RIO, UIO, ZIO}

import java.time.LocalDate

object GraphQLModel {

  //type ConsoleQuery[A] = ZQuery[Console, Nothing, A]

  case class UserGraph(id: UserID)

  case class TagGraph(id: TagID, name: String)

  case class CategoryGraph(
      id: CategoryID,
      name: String,
      tags: ZQuery[Has[TagRepository], Nothing, List[TagGraph]]
  )

  case class ExpenseGraph(
      id: ExpenseID,
      date: LocalDate,
      category: ZQuery[Has[CategoryRepository], Nothing, CategoryGraph],
      tag: ZQuery[Has[TagRepository], Nothing, TagGraph]
  )

  case class QueryArgs(userID: UserID)

  case class Queries(
      expenses: QueryArgs => RIO[Has[ExpenseRepository], List[ExpenseGraph]]
  )

  case class GetTag(id: TagID) extends Request[Nothing, TagGraph]
  val TagDataSource: DataSource[Has[TagRepository], GetTag] =
    DataSource.Batched.make("TagDataSource") { requests =>
      val result = TagRepository.getByIDs(requests.toList.map(_.id))
      result.fold(
        err => requests.toList.foldLeft(CompletedRequestMap.empty)((map, res) => map.insert(res)(Left(err))),
        _.foldLeft(CompletedRequestMap.empty)((map, tag) => map.insert(GetTag(tag.id))(Right(makeTagView(tag))))
      )
    }

  case class GetCategoryTags(id: CategoryID) extends Request[Nothing, List[TagGraph]]
  val CategoryTagsDataSource: DataSource[Has[TagRepository], GetCategoryTags] =
    DataSource.Batched.make("CategoryTagsDataSource") { requests =>
      val result = TagRepository.getByCategoryIDs(requests.toList.map(_.id))
      result.fold(
        err => requests.toList.foldLeft(CompletedRequestMap.empty)((map, res) => map.insert(res)(Left(err))),
        _.foldLeft(CompletedRequestMap.empty)((map, res) =>
          map.insert(GetCategoryTags(res._1))(Right(res._2.map(makeTagView)))
        )
      )
    }

  case class GetCategory(id: CategoryID) extends Request[Nothing, CategoryGraph]
  val CategoryDataSource: DataSource[Has[CategoryRepository], GetCategory] =
    DataSource.Batched.make("CategoryDataSource") { requests =>
      val result = CategoryRepository.getByIDs(requests.toList.map(_.id))
      result
        .fold(
          err =>
            requests.foldLeft(CompletedRequestMap.empty) { case (map, req) =>
              map.insert(req)(Left(err))
            },
          _.foldLeft(CompletedRequestMap.empty) { case (map, category) =>
            map.insert(GetCategory(category.id))(
              Right(makeCategoryView(category))
            )
          }
        )
    }

  private def makeTagView(tag: Tag) = TagGraph(tag.id, tag.name)

  private def makeCategoryView(category: Category): CategoryGraph =
    CategoryGraph(category.id, category.name, getTags(category.id))

  private def makeExpenseView(expense: Expense): ExpenseGraph =
    ExpenseGraph(
      expense.id,
      LocalDate.now(),
      getCategory(expense.categoryID),
      getTag(expense.tagID)
    )

  def getTag(id: TagID): ZQuery[Has[TagRepository], Nothing, TagGraph] =
    ZQuery.fromRequest(GetTag(id))(TagDataSource)

  private def getTags(id: CategoryID): ZQuery[Has[TagRepository], Nothing, List[TagGraph]] =
    ZQuery.fromRequest(GetCategoryTags(id))(CategoryTagsDataSource)

  def getCategory(id: CategoryID): ZQuery[Has[CategoryRepository], Nothing, CategoryGraph] =
    ZQuery.fromRequest(GetCategory(id))(CategoryDataSource)

  def getExpense(expense: Expense): UIO[ExpenseGraph] =
    ZIO.succeed(makeExpenseView(expense))

  def getExpenses(
      userID: UserID
  ): RIO[Has[ExpenseRepository], List[ExpenseGraph]] =
    for {
      expenses <- ExpenseRepository.getByUserID(userID)
      views    <- ZIO.foreach(expenses)(e => getExpense(e))
    } yield views

  val queriesResolver: Queries = Queries(args => getExpenses(args.userID))

  object schema extends GenericSchema[Has[TagRepository] with Has[CategoryRepository] with Has[ExpenseRepository]]

  import schema._

  val api = caliban.GraphQL.graphQL(RootResolver(queriesResolver))

  def main(args: Array[String]): Unit = println(api.render)

}
