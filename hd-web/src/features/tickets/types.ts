export type Status = 'open' | 'in_progress' | 'resolved' | 'closed'
export type Category = 'hardware' | 'software' | 'access' | 'other'
export type Priority = 'low' | 'medium' | 'high'

export const STATUSES: Status[] = ['open', 'in_progress', 'resolved', 'closed']

export type TicketListItem = {
  id: number
  title: string
  category: Category
  priority: Priority
  status: Status
  requesterId: number
  requesterName: string
  assigneeId: number | null
  createdAt: string
  updatedAt: string
}

export type Comment = {
  id: number
  ticketId: number
  authorId: number
  body: string
  createdAt: string
}

export type TicketDetail = TicketListItem & {
  description: string
  comments: Comment[]
}

/** PATCH /tickets/:id response — list fields minus requesterName, plus description. */
export type TicketResponse = Omit<TicketListItem, 'requesterName'> & {
  description: string
}
