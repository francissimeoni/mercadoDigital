type StorefrontErrorStateProps = {
  title: string
  message: string
  actionLabel?: string
  onAction?: () => void
}

export default function StorefrontErrorState({
  title,
  message,
  actionLabel,
  onAction,
}: StorefrontErrorStateProps) {
  return (
    <section role="alert" className="sf-status sf-status-error">
      <h2 className="text-xl font-semibold text-red-900">{title}</h2>
      <p className="mt-3 text-sm leading-6 text-red-800">{message}</p>
      {actionLabel && onAction ? (
        <button
          type="button"
          className="sf-button"
          onClick={onAction}
        >
          {actionLabel}
        </button>
      ) : null}
    </section>
  )
}
