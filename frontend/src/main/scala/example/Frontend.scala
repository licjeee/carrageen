package example

import caliban.client.laminext._
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import graphql.Client._
import org.scalajs.dom.html

object CalibanView {

  val uri = "http://localhost:8088/api/graphql"

  val allExpenses = Queries
    .expenses(1L)(ExpenseGraph.id ~ ExpenseGraph.category(CategoryGraph.name ~ CategoryGraph.tags(TagGraph.name)))
    .toEventStream(uri)

  def body: ReactiveHtmlElement[html.Div] = div(
    child <-- allExpenses.map {
      case Right(Some(expenses)) =>
        div(expenses.map { expense => div(div(expense._1), div(div(expense._2._1), div(expense._2._2))) })
      case oops => div(s"ERROR ${oops}")
    }
  )
}

object Frontend {
  def view = CalibanView.body

}
