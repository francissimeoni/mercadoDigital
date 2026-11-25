import { StrictMode } from "react"
import { createRoot } from "react-dom/client"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import "./index.css"
import App from "./App.tsx"
import { AuthProvider } from "./auth/AuthContext"
import { TenantProvider } from "./tenant/TenantContext"
import { StorefrontAuthProvider } from "./storefront/auth/StorefrontAuthContext"

const queryClient = new QueryClient()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <TenantProvider>
        <StorefrontAuthProvider>
          <AuthProvider>
            <App />
          </AuthProvider>
        </StorefrontAuthProvider>
      </TenantProvider>
    </QueryClientProvider>
  </StrictMode>,
)
