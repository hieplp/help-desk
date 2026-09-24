import { createFileRoute } from '@tanstack/react-router'
import { NewTicketPage } from '../../features/tickets/new'

export const Route = createFileRoute('/tickets/new')({
  component: NewTicketPage,
})
