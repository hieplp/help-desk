export default function Footer() {
  return (
    <footer className="mt-20 border-t border-[var(--line)] px-4 pb-14 pt-10 text-[var(--sea-ink-soft)]">
      <div className="page-wrap text-center sm:text-left">
        <p className="m-0 text-sm">&copy; {new Date().getFullYear()} Help Desk</p>
      </div>
    </footer>
  )
}
