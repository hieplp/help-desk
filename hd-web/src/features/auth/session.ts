import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'

export type Session = {
  token: string
  user: { id: number; name: string; email: string; role: 'requester' | 'agent' }
}

type SessionState = {
  session: Session | null
  setSession: (session: Session | null) => void
}

export const useSession = create<SessionState>()(
  persist(
    (set) => ({
      session: null,
      setSession: (session) => set({ session }),
    }),
    {
      name: 'hd.session',
      storage: createJSONStorage(() => localStorage),
    },
  ),
)

export const getSession = () => useSession.getState().session
export const setSession = (session: Session | null) =>
  useSession.getState().setSession(session)
