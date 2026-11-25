import type { ReactNode } from "react"

type FeedbackVariant = "success" | "error" | "warning" | "info"

type FeedbackAlertProps = {
  variant: FeedbackVariant
  title?: string
  children: ReactNode
}

const stylesByVariant: Record<FeedbackVariant, string> = {
  success: "border-emerald-200 bg-emerald-50 text-emerald-900",
  error: "border-red-300 bg-red-50 text-red-800",
  warning: "border-amber-300 bg-amber-50 text-amber-900",
  info: "border-blue-200 bg-blue-50 text-blue-900",
}

export default function FeedbackAlert({ variant, title, children }: FeedbackAlertProps) {
  return (
    <div className={`rounded-lg border p-4 text-sm shadow-sm ${stylesByVariant[variant]}`}>
      {title ? <p className="font-semibold">{title}</p> : null}
      <div className={title ? "mt-1" : ""}>{children}</div>
    </div>
  )
}
