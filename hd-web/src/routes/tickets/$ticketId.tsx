import { createFileRoute } from '@tanstack/react-router'
import { TicketDetailPage } from '../../features/tickets/detail'

export const Route = createFileRoute('/tickets/$ticketId')({
  component: TicketDetailPage,
})
