import { createFileRoute } from '@tanstack/react-router'
import { z } from 'zod'
import { TicketsPage } from '../../features/tickets'

const searchSchema = z.object({
  status: z.enum(['open', 'in_progress', 'resolved', 'closed']).optional(),
})

export const Route = createFileRoute('/tickets/')({
  validateSearch: searchSchema,
  component: TicketsPage,
})
