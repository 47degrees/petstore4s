package petstore

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

import org.http4s.QueryParamEncoder

package object client {

  def localDateTimeQueryParamEncoder(formatter: DateTimeFormatter): QueryParamEncoder[LocalDateTime] =
    QueryParamEncoder[String].contramap[LocalDateTime] { i: LocalDateTime =>
      formatter.format(i)
    }

  def localDateQueryParamEncoder(formatter: DateTimeFormatter): QueryParamEncoder[LocalDate] =
    QueryParamEncoder[String].contramap[LocalDate] { i: LocalDate =>
      formatter.format(i)
    }

  implicit val isoLocalDateTimeEncoder: QueryParamEncoder[LocalDateTime] =
    localDateTimeQueryParamEncoder(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  implicit val isoLocalDateEncoder: QueryParamEncoder[LocalDate] =
    localDateQueryParamEncoder(DateTimeFormatter.ISO_LOCAL_DATE)

}
