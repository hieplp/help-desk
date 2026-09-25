import { useState } from 'react'
import { toast } from '#/components/toast'
import { useAddComment } from '../hooks/useAddComment'
import type { Comment } from '../types'

export function CommentForm({
  ticketId,
  onAdded,
}: {
  ticketId: number
  onAdded: (comment: Comment) => void
}) {
  const [body, setBody] = useState('')
  const { add, error, pending } = useAddComment(ticketId)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    const text = body.trim()
    if (!text || !window.confirm('Post this comment?')) return
    const comment = await add(text)
    if (comment) {
      setBody('')
      toast('Comment posted')
      onAdded(comment)
    }
  }

  return (
    <form onSubmit={submit} className="mt-4 flex flex-col gap-3">
      <textarea
        className="demo-input"
        rows={3}
        maxLength={2000}
        placeholder="Add a comment…"
        value={body}
        onChange={(e) => setBody(e.target.value)}
      />
      {error && <p className="demo-alert demo-alert-danger m-0">{error}</p>}
      <div>
        <button
          className="demo-button"
          type="submit"
          disabled={pending || body.trim().length === 0}
        >
          {pending ? 'Posting…' : 'Post comment'}
        </button>
      </div>
    </form>
  )
}
