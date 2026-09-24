import { TicketForm } from './components/TicketForm'

export function NewTicketPage() {
  return (
    <main className="demo-page demo-center">
      <section className="demo-panel rise-in w-full max-w-md">
        <p className="island-kicker mb-2">Help Desk</p>
        <h1 className="demo-title mb-6">New ticket</h1>
        <TicketForm />
      </section>
    </main>
  )
}
