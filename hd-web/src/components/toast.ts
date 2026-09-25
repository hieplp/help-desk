import { create } from 'zustand'

export type Toast = { id: number; message: string }

type ToastState = {
  toasts: Toast[]
  push: (message: string) => void
  remove: (id: number) => void
}

let nextId = 1

export const useToasts = create<ToastState>()((set) => ({
  toasts: [],
  push: (message) => {
    const id = nextId++
    set((s) => ({ toasts: [...s.toasts, { id, message }] }))
    setTimeout(() => {
      useToasts.getState().remove(id)
    }, 3000)
  },
  remove: (id) => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
}))

export const toast = (message: string) => useToasts.getState().push(message)
