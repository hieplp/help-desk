import { getSession, setSession } from '#/features/auth/session'

async function request(method: string, path: string, body?: unknown) {
  const session = getSession()
  const res = await fetch(`/api${path}`, {
    method,
    headers: {
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(session ? { Authorization: `Bearer ${session.token}` } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) {
    const data = await res.json().catch(() => null)
    if (res.status === 401 && session) {
      setSession(null)
      window.location.href = '/login'
    }
    throw new Error(data?.message ?? `Request failed (${res.status})`)
  }
  return res.status === 204 ? null : res.json()
}

export const api = {
  get: (path: string) => request('GET', path),
  post: (path: string, body?: unknown) => request('POST', path, body),
  put: (path: string, body?: unknown) => request('PUT', path, body),
  patch: (path: string, body?: unknown) => request('PATCH', path, body),
  delete: (path: string) => request('DELETE', path),
}
