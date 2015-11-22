package models

import java.util.UUID

object TicketStatus extends Enumeration {
  val accepted, processing, processed, error = Value
}

case class Ticket(id: UUID, status: TicketStatus.Value)