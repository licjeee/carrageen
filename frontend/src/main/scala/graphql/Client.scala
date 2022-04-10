package graphql

import caliban.client.FieldBuilder._
import caliban.client._

object Client {

  type LocalDate = String

  type CategoryGraph
  object CategoryGraph {

    final case class CategoryGraphView[TagsSelection](id: Long, name: String, tags: List[TagsSelection])

    type ViewSelection[TagsSelection] = SelectionBuilder[CategoryGraph, CategoryGraphView[TagsSelection]]

    def view[TagsSelection](tagsSelection: SelectionBuilder[TagGraph, TagsSelection]): ViewSelection[TagsSelection] =
      (id ~ name ~ tags(tagsSelection)).map { case ((id, name), tags) => CategoryGraphView(id, name, tags) }

    def id: SelectionBuilder[CategoryGraph, Long]     = _root_.caliban.client.SelectionBuilder.Field("id", Scalar())
    def name: SelectionBuilder[CategoryGraph, String] = _root_.caliban.client.SelectionBuilder.Field("name", Scalar())
    def tags[A](innerSelection: SelectionBuilder[TagGraph, A]): SelectionBuilder[CategoryGraph, List[A]] =
      _root_.caliban.client.SelectionBuilder.Field("tags", ListOf(Obj(innerSelection)))
  }

  type ExpenseGraph
  object ExpenseGraph {

    final case class ExpenseGraphView[CategorySelection, TagSelection](
        id: Long,
        date: LocalDate,
        category: CategorySelection,
        tag: TagSelection
    )

    type ViewSelection[CategorySelection, TagSelection] =
      SelectionBuilder[ExpenseGraph, ExpenseGraphView[CategorySelection, TagSelection]]

    def view[CategorySelection, TagSelection](
        categorySelection: SelectionBuilder[CategoryGraph, CategorySelection],
        tagSelection: SelectionBuilder[TagGraph, TagSelection]
    ): ViewSelection[CategorySelection, TagSelection] =
      (id ~ date ~ category(categorySelection) ~ tag(tagSelection)).map { case (((id, date), category), tag) =>
        ExpenseGraphView(id, date, category, tag)
      }

    def id: SelectionBuilder[ExpenseGraph, Long]        = _root_.caliban.client.SelectionBuilder.Field("id", Scalar())
    def date: SelectionBuilder[ExpenseGraph, LocalDate] = _root_.caliban.client.SelectionBuilder.Field("date", Scalar())
    def category[A](innerSelection: SelectionBuilder[CategoryGraph, A]): SelectionBuilder[ExpenseGraph, A] =
      _root_.caliban.client.SelectionBuilder.Field("category", Obj(innerSelection))
    def tag[A](innerSelection: SelectionBuilder[TagGraph, A]): SelectionBuilder[ExpenseGraph, A] =
      _root_.caliban.client.SelectionBuilder.Field("tag", Obj(innerSelection))
  }

  type TagGraph
  object TagGraph {

    final case class TagGraphView(id: Long, name: String)

    type ViewSelection = SelectionBuilder[TagGraph, TagGraphView]

    def view: ViewSelection = (id ~ name).map { case (id, name) => TagGraphView(id, name) }

    def id: SelectionBuilder[TagGraph, Long]     = _root_.caliban.client.SelectionBuilder.Field("id", Scalar())
    def name: SelectionBuilder[TagGraph, String] = _root_.caliban.client.SelectionBuilder.Field("name", Scalar())
  }

  type Queries = _root_.caliban.client.Operations.RootQuery
  object Queries {
    def expenses[A](userID: Long)(innerSelection: SelectionBuilder[ExpenseGraph, A])(implicit
        encoder0: ArgEncoder[Long]
    ): SelectionBuilder[_root_.caliban.client.Operations.RootQuery, Option[List[A]]] =
      _root_.caliban.client.SelectionBuilder.Field(
        "expenses",
        OptionOf(ListOf(Obj(innerSelection))),
        arguments = List(Argument("userID", userID, "Long!")(encoder0))
      )
  }

}

