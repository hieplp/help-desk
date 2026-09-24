import { z } from 'zod'

export const createTicketSchema = z.object({
  title: z.string().trim().min(1, 'Title is required').max(120),
  description: z
    .string()
    .trim()
    .min(1, 'Description is required')
    .max(4000),
  category: z.enum(['hardware', 'software', 'access', 'other']),
  priority: z.enum(['low', 'medium', 'high']),
})

export type CreateTicketValues = z.infer<typeof createTicketSchema>
