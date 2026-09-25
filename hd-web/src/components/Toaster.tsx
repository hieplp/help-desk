import { useToasts } from './toast'

export function Toaster() {
  const toasts = useToasts((s) => s.toasts)
  if (toasts.length === 0) return null

  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((t) => (
        <p key={t.id} className="demo-panel rise-in m-0 px-4 py-2 text-sm">
          {t.message}
        </p>
      ))}
    </div>
  )
}
